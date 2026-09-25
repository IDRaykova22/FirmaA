package com.firmaa.techdept.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firmaa.techdept.models.Project;
import com.firmaa.techdept.models.User;
import com.firmaa.techdept.models.Workstation;
import com.firmaa.techdept.repositories.ProjectRepository;
import com.firmaa.techdept.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ProjectRepository projectRepository;

    // --- GET /api/employee/dashboard ---

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void getDashboard_withWorkstation_returnsDashboardData() throws Exception {
        Workstation ws = new Workstation();
        ws.setId(1L);
        ws.setTitle("Dev Station");

        User alice = new User();
        alice.setUsername("alice");
        alice.setWorkstation(ws);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        mockMvc.perform(get("/api/employee/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myWorkstation.title").value("Dev Station"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void getDashboard_noWorkstation_returnsBadRequest() throws Exception {
        User alice = new User();
        alice.setUsername("alice");
        alice.setWorkstation(null);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        mockMvc.perform(get("/api/employee/dashboard"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Нямате назначено работно място."));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void getDashboard_userNotFound_returnsBadRequest() throws Exception {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/employee/dashboard"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getDashboard_asAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/employee/dashboard"))
                .andExpect(status().isForbidden());
    }

    // --- POST /api/employee/projects/{id}/done ---

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void markProjectAsDone_success() throws Exception {
        User alice = new User();
        alice.setUsername("alice");

        Project project = new Project();
        project.setId(1L);
        project.setTitle("Alpha");
        project.setCompletedBy(new HashSet<>());

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenReturn(project);

        mockMvc.perform(post("/api/employee/projects/1/done").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Успешно маркирахте проекта като завършен!"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void markProjectAsDone_projectNotFound_returns404() throws Exception {
        User alice = new User();
        alice.setUsername("alice");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/employee/projects/99/done").with(csrf()))
                .andExpect(status().isNotFound());
    }
}
