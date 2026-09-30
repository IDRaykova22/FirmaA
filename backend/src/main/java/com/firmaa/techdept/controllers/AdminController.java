package com.firmaa.techdept.controllers;

import com.firmaa.techdept.dto.AssignEmployeesRequest;
import com.firmaa.techdept.dto.EmployeeRequest;
import com.firmaa.techdept.dto.EmployeeResponse;
import com.firmaa.techdept.dto.ProjectRequest;
import com.firmaa.techdept.dto.ProjectResponse;
import com.firmaa.techdept.dto.WorkstationRequest;
import com.firmaa.techdept.dto.WorkstationResponse;
import com.firmaa.techdept.models.Role;
import com.firmaa.techdept.models.User;
import com.firmaa.techdept.services.EmployeeService;
import com.firmaa.techdept.services.ProjectService;
import com.firmaa.techdept.services.WorkstationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Admin (manager) endpoints. Only maps HTTP to the service layer: all rules
 * live in the services, and their exceptions become HTTP errors in
 * {@link com.firmaa.techdept.exceptions.GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final EmployeeService employeeService;
    private final WorkstationService workstationService;
    private final ProjectService projectService;

    public AdminController(EmployeeService employeeService,
                           WorkstationService workstationService,
                           ProjectService projectService) {
        this.employeeService = employeeService;
        this.workstationService = workstationService;
        this.projectService = projectService;
    }

    // ── Employees and managers ──────────────────────────────────────────

    @GetMapping("/employees")
    public List<EmployeeResponse> getAllEmployees() {
        return employeeService.getAll();
    }

    @PostMapping("/employees")
    public ResponseEntity<String> createEmployee(@RequestBody EmployeeRequest request) {
        User created = employeeService.create(request);
        return ResponseEntity.ok(created.getRole() == Role.ROLE_ADMIN
                ? "Ръководителят е създаден успешно!"
                : "Работникът е създаден успешно!");
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<String> updateEmployee(@PathVariable Long id, @RequestBody EmployeeRequest request, Principal principal) {
        User updated = employeeService.update(id, request, principal.getName());
        return ResponseEntity.ok(updated.getRole() == Role.ROLE_ADMIN
                ? "Ръководителят е обновен успешно!"
                : "Работникът е обновен успешно!");
    }

    @DeleteMapping("/employees/{id}")
    public ResponseEntity<String> deleteEmployee(@PathVariable Long id, Principal principal) {
        User deleted = employeeService.delete(id, principal.getName());
        return ResponseEntity.ok(deleted.getRole() == Role.ROLE_ADMIN
                ? "Ръководителят е изтрит успешно!"
                : "Работникът е изтрит успешно!");
    }

    // ── Workstations ────────────────────────────────────────────────────

    @GetMapping("/workstations")
    public List<WorkstationResponse> getAllWorkstations() {
        return workstationService.getAll();
    }

    @PostMapping("/workstations")
    public ResponseEntity<String> createWorkstation(@RequestBody WorkstationRequest request) {
        workstationService.create(request);
        return ResponseEntity.ok("Работното място е създадено успешно!");
    }

    @PutMapping("/workstations/{id}")
    public ResponseEntity<String> updateWorkstation(@PathVariable Long id, @RequestBody WorkstationRequest request) {
        workstationService.update(id, request);
        return ResponseEntity.ok("Работното място е обновено успешно!");
    }

    @PutMapping("/workstations/{id}/employees")
    public ResponseEntity<String> assignEmployeesToWorkstation(@PathVariable Long id, @RequestBody AssignEmployeesRequest request) {
        workstationService.setEmployees(id, request.employeeIds());
        return ResponseEntity.ok("Работниците са обновени успешно!");
    }

    @DeleteMapping("/workstations/{id}")
    public ResponseEntity<String> deleteWorkstation(@PathVariable Long id) {
        workstationService.delete(id);
        return ResponseEntity.ok("Работното място е изтрито успешно!");
    }

    // ── Projects ────────────────────────────────────────────────────────

    @GetMapping("/projects")
    public List<ProjectResponse> getAllProjects() {
        return projectService.getAll();
    }

    @GetMapping("/projects/{id}")
    public ProjectResponse getProjectDetails(@PathVariable Long id) {
        return projectService.getById(id);
    }

    @PostMapping("/projects")
    public ResponseEntity<String> createProject(@RequestBody ProjectRequest request) {
        projectService.create(request);
        return ResponseEntity.ok("Проектът е създаден успешно!");
    }

    @PutMapping("/projects/{id}")
    public ResponseEntity<String> updateProject(@PathVariable Long id, @RequestBody ProjectRequest request) {
        projectService.update(id, request);
        return ResponseEntity.ok("Проектът е обновен успешно!");
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<String> deleteProject(@PathVariable Long id) {
        projectService.delete(id);
        return ResponseEntity.ok("Проектът е изтрит успешно!");
    }
}
