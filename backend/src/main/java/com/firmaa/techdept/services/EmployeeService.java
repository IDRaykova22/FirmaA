package com.firmaa.techdept.services;

import com.firmaa.techdept.dto.EmployeeRequest;
import com.firmaa.techdept.dto.EmployeeResponse;
import com.firmaa.techdept.dto.MyInfoResponse;
import com.firmaa.techdept.exceptions.BadRequestException;
import com.firmaa.techdept.exceptions.NotFoundException;
import com.firmaa.techdept.exceptions.UnauthorizedException;
import com.firmaa.techdept.models.Project;
import com.firmaa.techdept.models.Role;
import com.firmaa.techdept.models.User;
import com.firmaa.techdept.models.Workstation;
import com.firmaa.techdept.repositories.ProjectRepository;
import com.firmaa.techdept.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

/**
 * Business logic for people: employees (ROLE_USER) and managers (ROLE_ADMIN).
 * All validation rules for a person's data live here.
 */
@Service
public class EmployeeService {

    // Bulgarian national minimum wage in EUR/month (2026); update as it changes
    static final BigDecimal MIN_SALARY = new BigDecimal("620.20");
    static final int MIN_AGE = 18;

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder encoder;

    public EmployeeService(UserRepository userRepository, ProjectRepository projectRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAll() {
        return userRepository.findAll().stream().map(EmployeeResponse::from).toList();
    }

    @Transactional
    public User create(EmployeeRequest request) {
        String username = request.username();
        if (isBlank(username) || username.trim().length() < 3) {
            throw new BadRequestException("Грешка: Потребителското име трябва да е поне 3 символа!");
        }
        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Грешка: Потребителското име е заето!");
        }
        if (!isStrongPassword(request.password())) {
            throw new BadRequestException("Грешка: Паролата трябва да е поне 8 символа и да съдържа цифра и специален знак!");
        }

        User employee = new User();
        employee.setUsername(username);
        employee.setPassword(encoder.encode(request.password()));
        employee.setRole(parseRole(request.role()));
        applyDetails(employee, request);

        return userRepository.save(employee);
    }

    /**
     * @param currentUsername the logged-in admin, who may not rename themselves or change their own role
     */
    @Transactional
    public User update(Long id, EmployeeRequest request, String currentUsername) {
        User employee = findUser(id);
        boolean self = employee.getUsername().equals(currentUsername);

        String username = request.username();
        if (isBlank(username) || username.trim().length() < 3) {
            throw new BadRequestException("Грешка: Потребителското име трябва да е поне 3 символа!");
        }
        boolean renaming = !username.equals(employee.getUsername());
        // The JWT carries the username, so renaming yourself would end your own session
        if (renaming && self) {
            throw new BadRequestException("Грешка: Не можете да смените собственото си потребителско име!");
        }
        if (renaming && userRepository.existsByUsername(username)) {
            throw new BadRequestException("Грешка: Потребителското име е заето!");
        }

        // Password is optional on edit: a blank value keeps the current one
        if (!isBlank(request.password()) && !isStrongPassword(request.password())) {
            throw new BadRequestException("Грешка: Паролата трябва да е поне 8 символа и да съдържа цифра и специален знак!");
        }

        // Role is optional on edit: a missing value keeps the current one
        Role role = isBlank(request.role()) ? employee.getRole() : parseRole(request.role());
        if (self && role != employee.getRole()) {
            throw new BadRequestException("Грешка: Не можете да смените собствената си роля!");
        }

        employee.setUsername(username);
        if (!isBlank(request.password())) {
            employee.setPassword(encoder.encode(request.password()));
        }
        // Managers don't sit at a workstation, so a promotion frees the seat
        if (role == Role.ROLE_ADMIN && employee.getRole() != Role.ROLE_ADMIN) {
            employee.setWorkstation(null);
        }
        employee.setRole(role);
        applyDetails(employee, request);

        return userRepository.save(employee);
    }

    /** @return the deleted user, so the caller can tell whether it was a manager */
    @Transactional
    public User delete(Long id, String currentUsername) {
        User employee = findUser(id);
        if (employee.getUsername().equals(currentUsername)) {
            throw new BadRequestException("Грешка: Не можете да изтриете собствения си акаунт!");
        }

        // Drop the employee's project completions before removing the user
        for (Project project : projectRepository.findByCompletedBy_Id(id)) {
            project.getCompletedBy().removeIf(u -> u.getId().equals(id));
            projectRepository.save(project);
        }

        userRepository.delete(employee);
        return employee;
    }

    /** The logged-in user's own profile, with their workstation's projects. */
    @Transactional(readOnly = true)
    public MyInfoResponse getMyInfo(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        MyInfoResponse.MyWorkstation workstation = null;
        Workstation ws = user.getWorkstation();
        if (ws != null) {
            List<MyInfoResponse.MyProject> projects = projectRepository.findByWorkstations_Id(ws.getId()).stream()
                    .map(p -> new MyInfoResponse.MyProject(
                            p.getId(),
                            p.getTitle(),
                            p.getDescription(),
                            p.getDueDate(),
                            p.getCompletedBy().stream().anyMatch(u -> u.getId().equals(user.getId()))))
                    .toList();
            workstation = new MyInfoResponse.MyWorkstation(
                    ws.getId(), ws.getTitle(), ws.getDescription(),
                    ws.getComputerCount() != null ? ws.getComputerCount() : 0,
                    projects);
        }

        return new MyInfoResponse(
                user.getId(), user.getUsername(), user.getFirstName(), user.getLastName(),
                user.getRole().name(), user.getDepartment(), user.getJobTitle(), user.getAddress(),
                user.getDateOfBirth(), user.getSalary(), user.getPhoneNumber(), user.getJoinDate(),
                workstation);
    }

    // ── Validation ──────────────────────────────────────────────────────

    /** Validates and copies the non-credential fields. Expects the role to be set already. */
    private void applyDetails(User employee, EmployeeRequest request) {
        if (isBlank(request.firstName()) || isBlank(request.lastName())) {
            throw new BadRequestException("Грешка: Името и фамилията са задължителни!");
        }

        boolean manager = employee.getRole() == Role.ROLE_ADMIN;
        if (manager && isBlank(request.department())) {
            throw new BadRequestException("Грешка: Отделът е задължителен за ръководител!");
        }

        String phone = request.phoneNumber();
        if (!isBlank(phone) && !isValidPhoneNumber(phone)) {
            throw new BadRequestException("Грешка: Невалиден телефонен номер!");
        }

        LocalDate dob = request.dateOfBirth();
        if (dob != null && Period.between(dob, LocalDate.now()).getYears() < MIN_AGE) {
            throw new BadRequestException("Грешка: Работникът трябва да е поне на " + MIN_AGE + " години!");
        }

        BigDecimal salary = request.salary();
        if (salary != null && salary.compareTo(MIN_SALARY) < 0) {
            throw new BadRequestException("Грешка: Заплатата не може да е под минималната работна заплата (" + MIN_SALARY + " €)!");
        }

        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        employee.setDepartment(manager ? request.department().trim() : null);
        employee.setJobTitle(request.jobTitle());
        employee.setAddress(request.address());
        employee.setPhoneNumber(isBlank(phone) ? null : phone);
        employee.setDateOfBirth(dob);
        employee.setSalary(salary);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Грешка: Служителят не е намерен!"));
    }

    static Role parseRole(String role) {
        return "ROLE_ADMIN".equals(role) ? Role.ROLE_ADMIN : Role.ROLE_USER;
    }

    static boolean isStrongPassword(String password) {
        return password != null && password.length() >= 8
                && password.matches(".*\\d.*")
                && password.matches(".*[^A-Za-z0-9].*");
    }

    // Bulgarian numbers: 10 digits (0888123456) or with the 359 country code (12 digits)
    static boolean isValidPhoneNumber(String phoneNumber) {
        String digits = phoneNumber.replaceAll("\\D", "");
        return digits.length() == 10 || (digits.length() == 12 && digits.startsWith("359"));
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
