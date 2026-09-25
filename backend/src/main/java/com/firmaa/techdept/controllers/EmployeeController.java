package com.firmaa.techdept.controllers;

import com.firmaa.techdept.models.Project;
import com.firmaa.techdept.models.User;
import com.firmaa.techdept.repositories.ProjectRepository;
import com.firmaa.techdept.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/employee")
@PreAuthorize("hasRole('USER')")
public class EmployeeController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<?> getMyDashboard(Principal principal) {
        User currentUser = userRepository.findByUsername(principal.getName()).orElse(null);

        if (currentUser == null || currentUser.getWorkstation() == null) {
            return ResponseEntity.badRequest().body("Нямате назначено работно място.");
        }

        Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put("myWorkstation", currentUser.getWorkstation());


        return ResponseEntity.ok(dashboardData);
    }

    @PostMapping("/projects/{projectId}/done")
    public ResponseEntity<?> markProjectAsDone(@PathVariable Long projectId, Principal principal) {
        User currentUser = userRepository.findByUsername(principal.getName()).get();
        Project project = projectRepository.findById(projectId).orElse(null);

        if (project == null) {
            return ResponseEntity.notFound().build();
        }

        project.getCompletedBy().add(currentUser);
        projectRepository.save(project);

        return ResponseEntity.ok("Успешно маркирахте проекта като завършен!");
    }
}