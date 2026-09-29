package com.firmaa.techdept.controllers;

import com.firmaa.techdept.models.*;
import com.firmaa.techdept.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkstationRepository workstationRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PasswordEncoder encoder;

    // ── Employees ───────────────────────────────────────────────────────

    @GetMapping("/employees")
    public ResponseEntity<?> getAllEmployees() {
        List<User> users = userRepository.findAll();
        // Return a safe projection (no passwords)
        var result = users.stream().map(u -> {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", u.getId());
            dto.put("username", u.getUsername());
            dto.put("firstName", u.getFirstName() != null ? u.getFirstName() : "");
            dto.put("lastName", u.getLastName() != null ? u.getLastName() : "");
            dto.put("role", u.getRole().name());
            dto.put("department", u.getDepartment() != null ? u.getDepartment() : "");
            dto.put("jobTitle", u.getJobTitle() != null ? u.getJobTitle() : "");
            dto.put("phoneNumber", u.getPhoneNumber() != null ? u.getPhoneNumber() : "");
            dto.put("address", u.getAddress() != null ? u.getAddress() : "");
            dto.put("dateOfBirth", u.getDateOfBirth() != null ? u.getDateOfBirth().toString() : "");
            dto.put("salary", u.getSalary());
            dto.put("joinDate", u.getJoinDate() != null ? u.getJoinDate().toString() : "");
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/employees")
    public ResponseEntity<?> createEmployee(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        String password = (String) body.get("password");

        if (isBlank(username) || username.trim().length() < 3) {
            return ResponseEntity.badRequest().body("Грешка: Потребителското име трябва да е поне 3 символа!");
        }
        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest().body("Грешка: Потребителското име е заето!");
        }
        if (!isStrongPassword(password)) {
            return ResponseEntity.badRequest().body("Грешка: Паролата трябва да е поне 8 символа и да съдържа цифра и специален знак!");
        }

        User employee = new User();
        employee.setUsername(username);
        employee.setPassword(encoder.encode(password));
        employee.setRole(parseRole(body.get("role")));
        String detailsError = applyEmployeeDetails(employee, body);
        if (detailsError != null) {
            return ResponseEntity.badRequest().body(detailsError);
        }

        userRepository.save(employee);
        return ResponseEntity.ok(employee.getRole() == Role.ROLE_ADMIN
                ? "Ръководителят е създаден успешно!"
                : "Работникът е създаден успешно!");
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<?> updateEmployee(@PathVariable Long id, @RequestBody Map<String, Object> body, Principal principal) {
        User employee = userRepository.findById(id).orElse(null);
        if (employee == null) {
            return ResponseEntity.notFound().build();
        }

        String username = (String) body.get("username");
        if (isBlank(username) || username.trim().length() < 3) {
            return ResponseEntity.badRequest().body("Грешка: Потребителското име трябва да е поне 3 символа!");
        }
        boolean renaming = !username.equals(employee.getUsername());
        // The JWT carries the username, so renaming yourself would end your own session
        if (renaming && principal != null && employee.getUsername().equals(principal.getName())) {
            return ResponseEntity.badRequest().body("Грешка: Не можете да смените собственото си потребителско име!");
        }
        if (renaming && userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest().body("Грешка: Потребителското име е заето!");
        }
        employee.setUsername(username);

        // Password is optional on edit: a blank value keeps the current one
        String password = (String) body.get("password");
        if (!isBlank(password)) {
            if (!isStrongPassword(password)) {
                return ResponseEntity.badRequest().body("Грешка: Паролата трябва да е поне 8 символа и да съдържа цифра и специален знак!");
            }
            employee.setPassword(encoder.encode(password));
        }

        // Role is optional on edit: a missing value keeps the current one
        if (!isBlank(body.get("role"))) {
            Role role = parseRole(body.get("role"));
            boolean self = principal != null && employee.getUsername().equals(principal.getName());
            if (self && role != employee.getRole()) {
                return ResponseEntity.badRequest().body("Грешка: Не можете да смените собствената си роля!");
            }
            // Managers don't sit at a workstation, so a promotion frees the seat
            if (role == Role.ROLE_ADMIN && employee.getRole() != Role.ROLE_ADMIN) {
                employee.setWorkstation(null);
            }
            employee.setRole(role);
        }

        String detailsError = applyEmployeeDetails(employee, body);
        if (detailsError != null) {
            return ResponseEntity.badRequest().body(detailsError);
        }

        userRepository.save(employee);
        return ResponseEntity.ok("Работникът е обновен успешно!");
    }

    @DeleteMapping("/employees/{id}")
    @Transactional
    public ResponseEntity<?> deleteEmployee(@PathVariable Long id, Principal principal) {
        User employee = userRepository.findById(id).orElse(null);
        if (employee == null) {
            return ResponseEntity.notFound().build();
        }
        if (principal != null && employee.getUsername().equals(principal.getName())) {
            return ResponseEntity.badRequest().body("Грешка: Не можете да изтриете собствения си акаунт!");
        }

        // Drop the employee's project completions before removing the user
        for (Project project : projectRepository.findByCompletedBy_Id(id)) {
            project.getCompletedBy().removeIf(u -> u.getId().equals(id));
            projectRepository.save(project);
        }

        userRepository.delete(employee);
        return ResponseEntity.ok("Работникът е изтрит успешно!");
    }

    // Bulgarian national minimum wage in EUR/month (2026); update as it changes
    private static final BigDecimal MIN_SALARY = new BigDecimal("620.20");

    private static Role parseRole(Object role) {
        return "ROLE_ADMIN".equals(role) ? Role.ROLE_ADMIN : Role.ROLE_USER;
    }

    /** Applies the non-credential employee fields. Returns an error message, or null if valid. Expects the role to be set. */
    private String applyEmployeeDetails(User employee, Map<String, Object> body) {
        Object firstName = body.get("firstName");
        Object lastName = body.get("lastName");
        if (isBlank(firstName) || isBlank(lastName)) {
            return "Грешка: Името и фамилията са задължителни!";
        }
        employee.setFirstName(firstName.toString().trim());
        employee.setLastName(lastName.toString().trim());

        Object department = body.get("department");
        if (employee.getRole() == Role.ROLE_ADMIN) {
            if (isBlank(department)) {
                return "Грешка: Отделът е задължителен за ръководител!";
            }
            employee.setDepartment(department.toString().trim());
        } else {
            employee.setDepartment(null);
        }

        employee.setJobTitle((String) body.get("jobTitle"));
        employee.setAddress((String) body.get("address"));

        Object phoneNumber = body.get("phoneNumber");
        if (!isBlank(phoneNumber) && !isValidPhoneNumber(phoneNumber.toString())) {
            return "Грешка: Невалиден телефонен номер!";
        }
        employee.setPhoneNumber(isBlank(phoneNumber) ? null : phoneNumber.toString());

        Object dateOfBirth = body.get("dateOfBirth");
        if (!isBlank(dateOfBirth)) {
            LocalDate dob = LocalDate.parse(dateOfBirth.toString());
            if (Period.between(dob, LocalDate.now()).getYears() < 18) {
                return "Грешка: Работникът трябва да е поне на 18 години!";
            }
            employee.setDateOfBirth(dob);
        } else {
            employee.setDateOfBirth(null);
        }

        Object salary = body.get("salary");
        if (!isBlank(salary)) {
            BigDecimal salaryValue = new BigDecimal(salary.toString());
            if (salaryValue.compareTo(MIN_SALARY) < 0) {
                return "Грешка: Заплатата не може да е под минималната работна заплата (" + MIN_SALARY + " €)!";
            }
            employee.setSalary(salaryValue);
        } else {
            employee.setSalary(null);
        }

        return null;
    }

    private static boolean isBlank(Object value) {
        return value == null || value.toString().isBlank();
    }

    private static boolean isStrongPassword(String password) {
        return password != null && password.length() >= 8
                && password.matches(".*\\d.*")
                && password.matches(".*[^A-Za-z0-9].*");
    }

    // Bulgarian mobile numbers: 10 digits (0888123456) or with the 359 country code (12 digits)
    private static boolean isValidPhoneNumber(String phoneNumber) {
        String digits = phoneNumber.replaceAll("\\D", "");
        return digits.length() == 10 || (digits.length() == 12 && digits.startsWith("359"));
    }

    // ── Workstations ────────────────────────────────────────────────────

    @GetMapping("/workstations")
    public ResponseEntity<?> getAllWorkstations() {
        List<Workstation> workstations = workstationRepository.findAll();
        var result = workstations.stream().map(ws -> {
            List<Map<String, Object>> emps = ws.getEmployees().stream().map(u -> Map.<String, Object>of(
                    "id", u.getId(),
                    "username", u.getUsername(),
                    "firstName", u.getFirstName() != null ? u.getFirstName() : "",
                    "lastName", u.getLastName() != null ? u.getLastName() : ""
            )).collect(Collectors.toList());
            return Map.of(
                    "id", ws.getId(),
                    "title", ws.getTitle(),
                    "description", ws.getDescription() != null ? ws.getDescription() : "",
                    "computerCount", ws.getComputerCount() != null ? ws.getComputerCount() : 0,
                    "employees", emps
            );
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/workstations")
    public ResponseEntity<?> createWorkstation(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        if (isBlank(title)) {
            return ResponseEntity.badRequest().body("Грешка: Заглавието е задължително!");
        }
        Integer computerCount = parseComputerCount(body.get("computerCount"));
        if (computerCount == null) {
            return ResponseEntity.badRequest().body("Грешка: Броят компютри трябва да е цяло число, 0 или повече!");
        }

        Workstation ws = new Workstation();
        ws.setTitle(title);
        ws.setDescription((String) body.get("description"));
        ws.setComputerCount(computerCount);
        Workstation savedWs = workstationRepository.save(ws);

        // Assign employees to this workstation
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) body.get("employeeIds");
        assignEmployees(savedWs, ids);
        return ResponseEntity.ok("Работното място е създадено успешно!");
    }

    @PutMapping("/workstations/{id}/employees")
    public ResponseEntity<?> assignEmployeesToWorkstation(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Workstation ws = workstationRepository.findById(id).orElse(null);
        if (ws == null) {
            return ResponseEntity.notFound().build();
        }

        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) body.get("employeeIds");
        unassignEmployees(ws);
        assignEmployees(ws, ids);

        return ResponseEntity.ok("Работниците са обновени успешно!");
    }

    @PutMapping("/workstations/{id}")
    @Transactional
    public ResponseEntity<?> updateWorkstation(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Workstation ws = workstationRepository.findById(id).orElse(null);
        if (ws == null) {
            return ResponseEntity.notFound().build();
        }

        String title = (String) body.get("title");
        if (isBlank(title)) {
            return ResponseEntity.badRequest().body("Грешка: Заглавието е задължително!");
        }
        Integer computerCount = parseComputerCount(body.get("computerCount"));
        if (computerCount == null) {
            return ResponseEntity.badRequest().body("Грешка: Броят компютри трябва да е цяло число, 0 или повече!");
        }
        ws.setTitle(title);
        ws.setDescription((String) body.get("description"));
        ws.setComputerCount(computerCount);
        workstationRepository.save(ws);

        // Only touch the employee list when the client sends one
        if (body.containsKey("employeeIds")) {
            @SuppressWarnings("unchecked")
            List<Number> ids = (List<Number>) body.get("employeeIds");
            unassignEmployees(ws);
            assignEmployees(ws, ids);
        }

        return ResponseEntity.ok("Работното място е обновено успешно!");
    }

    @DeleteMapping("/workstations/{id}")
    @Transactional
    public ResponseEntity<?> deleteWorkstation(@PathVariable Long id) {
        Workstation ws = workstationRepository.findById(id).orElse(null);
        if (ws == null) {
            return ResponseEntity.notFound().build();
        }

        List<Project> projects = projectRepository.findByWorkstations_Id(id);

        // A project must keep at least one workstation, so refuse if this is the last one
        List<String> blocking = projects.stream()
                .filter(p -> p.getWorkstations().size() <= 1)
                .map(Project::getTitle)
                .collect(Collectors.toList());
        if (!blocking.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    "Грешка: Работното място е единственото за проект(и): " + String.join(", ", blocking)
                            + ". Първо редактирайте или изтрийте тези проекти.");
        }

        for (Project project : projects) {
            project.getWorkstations().removeIf(w -> w.getId().equals(id));
            projectRepository.save(project);
        }
        unassignEmployees(ws);

        workstationRepository.delete(ws);
        return ResponseEntity.ok("Работното място е изтрито успешно!");
    }

    /** Blank means 0 computers. Returns null when the value isn't a non-negative whole number. */
    private static Integer parseComputerCount(Object value) {
        if (isBlank(value)) {
            return 0;
        }
        try {
            BigDecimal count = new BigDecimal(value.toString());
            if (count.signum() < 0 || count.stripTrailingZeros().scale() > 0) {
                return null;
            }
            return count.intValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }

    private void unassignEmployees(Workstation ws) {
        for (User emp : ws.getEmployees()) {
            emp.setWorkstation(null);
            userRepository.save(emp);
        }
        ws.getEmployees().clear();
    }

    private void assignEmployees(Workstation ws, List<Number> ids) {
        if (ids == null) {
            return;
        }
        for (Number empId : ids) {
            User dbUser = userRepository.findById(empId.longValue()).orElse(null);
            if (dbUser != null) {
                dbUser.setWorkstation(ws);
                userRepository.save(dbUser);
            }
        }
    }

    // ── Projects ────────────────────────────────────────────────────────

    @GetMapping("/projects")
    public ResponseEntity<?> getAllProjects() {
        List<Project> projects = projectRepository.findAll();
        var result = projects.stream().map(p -> {
            var wsList = p.getWorkstations().stream().map(ws -> Map.<String, Object>of(
                    "id", ws.getId(),
                    "title", ws.getTitle()
            )).collect(Collectors.toList());
            var completedList = p.getCompletedBy().stream().map(u -> Map.<String, Object>of(
                    "id", u.getId(),
                    "username", u.getUsername()
            )).collect(Collectors.toList());
            // Workers seated at the project's workstations (managers don't sit at one)
            long employeeCount = p.getWorkstations().stream()
                    .flatMap(ws -> ws.getEmployees().stream())
                    .filter(u -> u.getRole() == Role.ROLE_USER)
                    .count();
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", p.getId());
            dto.put("title", p.getTitle());
            dto.put("description", p.getDescription() != null ? p.getDescription() : "");
            dto.put("dueDate", p.getDueDate().toString());
            dto.put("projectValue", p.getProjectValue());
            dto.put("employeeCount", employeeCount);
            dto.put("workstations", wsList);
            dto.put("completedBy", completedList);
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/projects")
    public ResponseEntity<?> createProject(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        Object dueDate = body.get("dueDate");
        if (isBlank(title) || isBlank(dueDate)) {
            return ResponseEntity.badRequest().body("Грешка: Заглавието и крайният срок са задължителни!");
        }
        BigDecimal projectValue = parseProjectValue(body.get("projectValue"));
        if (projectValue == null) {
            return ResponseEntity.badRequest().body("Грешка: Стойността на проекта е задължителна и не може да е отрицателна!");
        }

        Project project = new Project();
        project.setTitle(title);
        project.setDescription((String) body.get("description"));
        project.setDueDate(LocalDate.parse(dueDate.toString()));
        project.setProjectValue(projectValue);

        @SuppressWarnings("unchecked")
        List<Number> wsIds = (List<Number>) body.get("workstationIds");
        if (wsIds == null || wsIds.isEmpty()) {
            return ResponseEntity.badRequest().body("Проектът трябва да има поне едно работно място!");
        }

        project.setWorkstations(findWorkstations(wsIds));

        projectRepository.save(project);
        return ResponseEntity.ok("Проектът е създаден успешно!");
    }

    @PutMapping("/projects/{id}")
    @Transactional
    public ResponseEntity<?> updateProject(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Project project = projectRepository.findById(id).orElse(null);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }

        String title = (String) body.get("title");
        Object dueDate = body.get("dueDate");
        if (isBlank(title) || isBlank(dueDate)) {
            return ResponseEntity.badRequest().body("Грешка: Заглавието и крайният срок са задължителни!");
        }
        BigDecimal projectValue = parseProjectValue(body.get("projectValue"));
        if (projectValue == null) {
            return ResponseEntity.badRequest().body("Грешка: Стойността на проекта е задължителна и не може да е отрицателна!");
        }

        @SuppressWarnings("unchecked")
        List<Number> wsIds = (List<Number>) body.get("workstationIds");
        if (wsIds == null || wsIds.isEmpty()) {
            return ResponseEntity.badRequest().body("Проектът трябва да има поне едно работно място!");
        }

        project.setTitle(title);
        project.setDescription((String) body.get("description"));
        project.setDueDate(LocalDate.parse(dueDate.toString()));
        project.setProjectValue(projectValue);
        project.getWorkstations().clear();
        project.getWorkstations().addAll(findWorkstations(wsIds));

        projectRepository.save(project);
        return ResponseEntity.ok("Проектът е обновен успешно!");
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Long id) {
        Project project = projectRepository.findById(id).orElse(null);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }

        // Project owns both join tables, so its workstation/completion rows go with it
        projectRepository.delete(project);
        return ResponseEntity.ok("Проектът е изтрит успешно!");
    }

    /** Returns the value in EUR, or null when it's missing, not a number, or negative. */
    private static BigDecimal parseProjectValue(Object value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            BigDecimal amount = new BigDecimal(value.toString());
            return amount.signum() < 0 ? null : amount;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Set<Workstation> findWorkstations(List<Number> wsIds) {
        Set<Workstation> workstations = new HashSet<>();
        for (Number id : wsIds) {
            Workstation ws = workstationRepository.findById(id.longValue()).orElse(null);
            if (ws != null) workstations.add(ws);
        }
        return workstations;
    }

    @GetMapping("/projects/{projectId}")
    public ResponseEntity<?> getProjectDetails(@PathVariable Long projectId) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(project);
    }
}