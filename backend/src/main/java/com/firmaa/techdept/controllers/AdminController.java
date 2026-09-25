package com.firmaa.techdept.controllers;

import com.firmaa.techdept.models.*;
import com.firmaa.techdept.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

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
        var result = users.stream().map(u -> Map.of(
                "id", u.getId(),
                "username", u.getUsername(),
                "role", u.getRole().name(),
                "jobTitle", u.getJobTitle() != null ? u.getJobTitle() : "",
                "phoneNumber", u.getPhoneNumber() != null ? u.getPhoneNumber() : ""
        )).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/employees")
    public ResponseEntity<?> createEmployee(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        String password = (String) body.get("password");

        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest().body("Грешка: Потребителското име е заето!");
        }

        User employee = new User();
        employee.setUsername(username);
        employee.setPassword(encoder.encode(password));
        employee.setRole(Role.ROLE_USER);
        employee.setJobTitle((String) body.get("jobTitle"));
        employee.setAddress((String) body.get("address"));
        employee.setPhoneNumber((String) body.get("phoneNumber"));

        if (body.get("dateOfBirth") != null) {
            employee.setDateOfBirth(java.time.LocalDate.parse((String) body.get("dateOfBirth")));
        }
        if (body.get("salary") != null) {
            employee.setSalary(new java.math.BigDecimal(body.get("salary").toString()));
        }

        userRepository.save(employee);
        return ResponseEntity.ok("Работникът е създаден успешно!");
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
        if (body.get("employeeIds") != null) {
            @SuppressWarnings("unchecked")
            List<Number> ids = (List<Number>) body.get("employeeIds");
            for (Number id : ids) {
                User dbUser = userRepository.findById(id.longValue()).orElse(null);
                if (dbUser != null) {
                    dbUser.setWorkstation(savedWs);
                    userRepository.save(dbUser);
                }
            }
        }
        return ResponseEntity.ok("Работното място е създадено успешно!");
    }

    @PutMapping("/workstations/{id}/employees")
    public ResponseEntity<?> assignEmployeesToWorkstation(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Workstation ws = workstationRepository.findById(id).orElse(null);
        if (ws == null) {
            return ResponseEntity.notFound().build();
        }

        // Remove current employees from this workstation
        for (User emp : ws.getEmployees()) {
            emp.setWorkstation(null);
            userRepository.save(emp);
        }

        // Assign new employees
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) body.get("employeeIds");
        if (ids != null) {
            for (Number empId : ids) {
                User dbUser = userRepository.findById(empId.longValue()).orElse(null);
                if (dbUser != null) {
                    dbUser.setWorkstation(ws);
                    userRepository.save(dbUser);
                }
            }
        }

        return ResponseEntity.ok("Работниците са обновени успешно!");
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
        project.setDueDate(java.time.LocalDate.parse((String) body.get("dueDate")));

        @SuppressWarnings("unchecked")
        List<Number> wsIds = (List<Number>) body.get("workstationIds");
        if (wsIds == null || wsIds.isEmpty()) {
            return ResponseEntity.badRequest().body("Проектът трябва да има поне едно работно място!");
        }

        Set<Workstation> workstations = new HashSet<>();
        for (Number id : wsIds) {
            Workstation ws = workstationRepository.findById(id.longValue()).orElse(null);
            if (ws != null) workstations.add(ws);
        }
        project.setWorkstations(workstations);

        projectRepository.save(project);
        return ResponseEntity.ok("Проектът е създаден успешно!");
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