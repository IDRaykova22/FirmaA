package com.firmaa.techdept.controllers;

import com.firmaa.techdept.models.*;
import com.firmaa.techdept.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.firmaa.techdept.models.Project;


import java.util.List;

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

    @PostMapping("/employees")
    public ResponseEntity<?> createEmployee(@RequestBody User employeeRequest) {
        if (userRepository.existsByUsername(employeeRequest.getUsername())) {
            return ResponseEntity.badRequest().body("Грешка: Потребителското име е заето!");
        }

        employeeRequest.setPassword(encoder.encode(employeeRequest.getPassword()));
        employeeRequest.setRole(Role.ROLE_USER);

        userRepository.save(employeeRequest);
        return ResponseEntity.ok("Работникът е създаден успешно!");
    }

    @PostMapping("/workstations")
    public ResponseEntity<?> createWorkstation(@RequestBody Workstation request) {
        Workstation savedWorkstation = workstationRepository.save(request);

        if (request.getEmployees() != null && !request.getEmployees().isEmpty()) {
            for (User employee : request.getEmployees()) {
                User dbUser = userRepository.findById(employee.getId()).orElse(null);
                if (dbUser != null) {
                    dbUser.setWorkstation(savedWorkstation);
                    userRepository.save(dbUser);
                }
            }
        }
        return ResponseEntity.ok("Работното място е създадено успешно!");
    }



    @PostMapping("/projects")
    public ResponseEntity<?> createProject(@RequestBody Project request) {
        if (request.getWorkstations() == null || request.getWorkstations().isEmpty()) {
            return ResponseEntity.badRequest().body("Грешка: Проектът трябва да има поне едно работно място!");
        }

        projectRepository.save(request);
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