package com.firmaa.techdept.controllers;

import com.firmaa.techdept.dto.MyInfoResponse;
import com.firmaa.techdept.services.EmployeeService;
import com.firmaa.techdept.services.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/** Endpoints for any logged-in user, acting on their own data. */
@RestController
@RequestMapping("/api/employee")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final ProjectService projectService;

    public EmployeeController(EmployeeService employeeService, ProjectService projectService) {
        this.employeeService = employeeService;
        this.projectService = projectService;
    }

    /** The current user's profile, workstation and that workstation's projects. */
    @GetMapping("/me")
    public MyInfoResponse getMyInfo(Principal principal) {
        return employeeService.getMyInfo(principal.getName());
    }

    @PostMapping("/projects/{projectId}/done")
    public ResponseEntity<String> markProjectAsDone(@PathVariable Long projectId, Principal principal) {
        projectService.markDone(projectId, principal.getName());
        return ResponseEntity.ok("Успешно маркирахте проекта като завършен!");
    }
}
