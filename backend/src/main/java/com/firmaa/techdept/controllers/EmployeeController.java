package com.firmaa.techdept.controllers;

import com.firmaa.techdept.models.Project;
import com.firmaa.techdept.models.User;
import com.firmaa.techdept.models.Workstation;
import com.firmaa.techdept.repositories.ProjectRepository;
import com.firmaa.techdept.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/employee")
public class EmployeeController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    /**
     * Returns the current user's profile info and their workstation/projects.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body("User not found");
        }

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("role", user.getRole().name());
        info.put("jobTitle", user.getJobTitle());
        info.put("address", user.getAddress());
        info.put("dateOfBirth", user.getDateOfBirth());
        info.put("salary", user.getSalary());
        info.put("phoneNumber", user.getPhoneNumber());
        info.put("joinDate", user.getJoinDate());

        // Include workstation info
        Workstation ws = user.getWorkstation();
        if (ws != null) {
            Map<String, Object> wsInfo = new LinkedHashMap<>();
            wsInfo.put("id", ws.getId());
            wsInfo.put("title", ws.getTitle());
            wsInfo.put("description", ws.getDescription());

            // Find projects assigned to this workstation
            List<Project> allProjects = projectRepository.findAll();
            List<Map<String, Object>> projects = allProjects.stream()
                    .filter(p -> p.getWorkstations().contains(ws))
                    .map(p -> {
                        Map<String, Object> pInfo = new LinkedHashMap<>();
                        pInfo.put("id", p.getId());
                        pInfo.put("title", p.getTitle());
                        pInfo.put("description", p.getDescription());
                        pInfo.put("dueDate", p.getDueDate());
                        pInfo.put("completedByMe", p.getCompletedBy().stream()
                                .anyMatch(u -> u.getId().equals(user.getId())));
                        return pInfo;
                    })
                    .collect(Collectors.toList());

            wsInfo.put("projects", projects);
            info.put("workstation", wsInfo);
        }

        return ResponseEntity.ok(info);
    }

    @PostMapping("/projects/{projectId}/done")
    public ResponseEntity<?> markProjectAsDone(@PathVariable Long projectId, Principal principal) {
        User currentUser = userRepository.findByUsername(principal.getName()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(401).body("User not found");
        }

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }

        project.getCompletedBy().add(currentUser);
        projectRepository.save(project);

        return ResponseEntity.ok("Успешно маркирахте проекта като завършен!");
    }
}