package com.firmaa.techdept.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firmaa.techdept.models.Project;
import com.firmaa.techdept.models.Role;
import com.firmaa.techdept.models.User;
import com.firmaa.techdept.models.Workstation;
import com.firmaa.techdept.repositories.ProjectRepository;
import com.firmaa.techdept.repositories.UserRepository;
import com.firmaa.techdept.repositories.WorkstationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private WorkstationRepository workstationRepository;

    @MockitoBean
    private ProjectRepository projectRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    // --- POST /api/admin/employees ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void createEmployee_success() throws Exception {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("hashed");

        User request = new User();
        request.setUsername("alice");
        request.setPassword("pass");

        mockMvc.perform(post("/api/admin/employees")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Работникът е създаден успешно!"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createEmployee_usernameTaken_returnsBadRequest() throws Exception {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        User request = new User();
        request.setUsername("alice");
        request.setPassword("pass");

        mockMvc.perform(post("/api/admin/employees")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Грешка: Потребителското име е заето!"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createEmployee_asUser_returnsForbidden() throws Exception {
        User request = new User();
        request.setUsername("alice");
        request.setPassword("pass");

        mockMvc.perform(post("/api/admin/employees")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // --- POST /api/admin/workstations ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void createWorkstation_success() throws Exception {
        Workstation saved = new Workstation();
        saved.setId(1L);
        saved.setTitle("Dev Station");

        when(workstationRepository.save(any())).thenReturn(saved);

        Workstation request = new Workstation();
        request.setTitle("Dev Station");
        request.setDescription("Main dev area");

        mockMvc.perform(post("/api/admin/workstations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Работното място е създадено успешно!"));
    }

    // --- POST /api/admin/projects ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProject_success() throws Exception {
        Workstation ws = new Workstation();
        ws.setId(1L);
        ws.setTitle("Dev Station");

        Project request = new Project();
        request.setTitle("New Project");
        request.setDueDate(LocalDate.now().plusDays(30));
        request.setWorkstations(Set.of(ws));

        mockMvc.perform(post("/api/admin/projects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Проектът е създаден успешно!"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProject_noWorkstations_returnsBadRequest() throws Exception {
        Project request = new Project();
        request.setTitle("Empty Project");
        request.setDueDate(LocalDate.now().plusDays(10));

        mockMvc.perform(post("/api/admin/projects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Грешка: Проектът трябва да има поне едно работно място!"));
    }

    // --- GET /api/admin/projects/{id} ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void getProjectDetails_found_returnsProject() throws Exception {
        Project project = new Project();
        project.setId(1L);
        project.setTitle("Alpha");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        mockMvc.perform(get("/api/admin/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Alpha"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getProjectDetails_notFound_returns404() throws Exception {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/projects/99"))
                .andExpect(status().isNotFound());
    }
}
