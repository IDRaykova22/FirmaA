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
            dto.put("role", u.getRole().name());
            dto.put("jobTitle", u.getJobTitle() != null ? u.getJobTitle() : "");
            dto.put("phoneNumber", u.getPhoneNumber() != null ? u.getPhoneNumber() : "");
            dto.put("address", u.getAddress() != null ? u.getAddress() : "");
            dto.put("dateOfBirth", u.getDateOfBirth() != null ? u.getDateOfBirth().toString() : "");
            dto.put("salary", u.getSalary());
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
        employee.setRole(Role.ROLE_USER);
        String detailsError = applyEmployeeDetails(employee, body);
        if (detailsError != null) {
            return ResponseEntity.badRequest().body(detailsError);
        }

        userRepository.save(employee);
        return ResponseEntity.ok("Работникът е създаден успешно!");
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

    // National minimum wage (BGN/month); update as it changes
    private static final BigDecimal MIN_SALARY = new BigDecimal("1077");

    /** Applies the non-credential employee fields. Returns an error message, or null if valid. */
    private String applyEmployeeDetails(User employee, Map<String, Object> body) {
        employee.setJobTitle((String) body.get("jobTitle"));
        employee.setAddress((String) body.get("address"));
        employee.setPhoneNumber((String) body.get("phoneNumber"));

        Object dateOfBirth = body.get("dateOfBirth");
        employee.setDateOfBirth(isBlank(dateOfBirth) ? null : LocalDate.parse(dateOfBirth.toString()));

        Object salary = body.get("salary");
        if (!isBlank(salary)) {
            BigDecimal salaryValue = new BigDecimal(salary.toString());
            if (salaryValue.compareTo(MIN_SALARY) < 0) {
                return "Грешка: Заплатата не може да е под минималната работна заплата (" + MIN_SALARY + " лв.)!";
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

    // ── Workstations ────────────────────────────────────────────────────

    @GetMapping("/workstations")
    public ResponseEntity<?> getAllWorkstations() {
        List<Workstation> workstations = workstationRepository.findAll();
        var result = workstations.stream().map(ws -> {
            List<Map<String, Object>> emps = ws.getEmployees().stream().map(u -> Map.<String, Object>of(
                    "id", u.getId(),
                    "username", u.getUsername()
            )).collect(Collectors.toList());
            return Map.of(
                    "id", ws.getId(),
                    "title", ws.getTitle(),
                    "description", ws.getDescription() != null ? ws.getDescription() : "",
                    "employees", emps
            );
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/workstations")
    public ResponseEntity<?> createWorkstation(@RequestBody Map<String, Object> body) {
        Workstation ws = new Workstation();
        ws.setTitle((String) body.get("title"));
        ws.setDescription((String) body.get("description"));
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
        ws.setTitle(title);
        ws.setDescription((String) body.get("description"));
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
            return Map.of(
                    "id", p.getId(),
                    "title", p.getTitle(),
                    "description", p.getDescription() != null ? p.getDescription() : "",
                    "dueDate", p.getDueDate().toString(),
                    "workstations", wsList,
                    "completedBy", completedList
            );
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/projects")
    public ResponseEntity<?> createProject(@RequestBody Map<String, Object> body) {
        Project project = new Project();
        project.setTitle((String) body.get("title"));
        project.setDescription((String) body.get("description"));
        project.setDueDate(LocalDate.parse((String) body.get("dueDate")));

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

        @SuppressWarnings("unchecked")
        List<Number> wsIds = (List<Number>) body.get("workstationIds");
        if (wsIds == null || wsIds.isEmpty()) {
            return ResponseEntity.badRequest().body("Проектът трябва да има поне едно работно място!");
        }

        project.setTitle(title);
        project.setDescription((String) body.get("description"));
        project.setDueDate(LocalDate.parse(dueDate.toString()));
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