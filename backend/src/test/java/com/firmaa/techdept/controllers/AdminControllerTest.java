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
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import com.firmaa.techdept.security.JwtAuthenticationFilter;
import com.firmaa.techdept.security.JwtUtils;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtUtils jwtUtils;

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

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

    // --- PUT/DELETE /api/admin/employees/{id} ---

    private User existingUser(long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("old-hash");
        user.setRole(Role.ROLE_USER);
        return user;
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateEmployee_success_keepsPasswordWhenBlank() throws Exception {
        User user = existingUser(2L, "worker");
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        mockMvc.perform(put("/api/admin/employees/2")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "worker",
                                "password", "",
                                "jobTitle", "Lead",
                                "dateOfBirth", "",
                                "salary", ""))))
                .andExpect(status().isOk())
                .andExpect(content().string("Работникът е обновен успешно!"));

        assertEquals("old-hash", user.getPassword());
        assertEquals("Lead", user.getJobTitle());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateEmployee_usernameTaken_returnsBadRequest() throws Exception {
        when(userRepository.findById(2L)).thenReturn(Optional.of(existingUser(2L, "worker")));
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        mockMvc.perform(put("/api/admin/employees/2")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "alice"))))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Грешка: Потребителското име е заето!"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateEmployee_renameSelf_returnsBadRequest() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser(1L, "admin")));

        mockMvc.perform(put("/api/admin/employees/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "boss"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteEmployee_removesCompletionsAndUser() throws Exception {
        User user = existingUser(2L, "worker");
        Project project = new Project();
        project.setCompletedBy(new HashSet<>(Set.of(user)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(projectRepository.findByCompletedBy_Id(2L)).thenReturn(List.of(project));

        mockMvc.perform(delete("/api/admin/employees/2").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Работникът е изтрит успешно!"));

        assertTrue(project.getCompletedBy().isEmpty());
        verify(userRepository).delete(user);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteEmployee_self_returnsBadRequest() throws Exception {
        User admin = existingUser(1L, "admin");
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        mockMvc.perform(delete("/api/admin/employees/1").with(csrf()))
                .andExpect(status().isBadRequest());

        verify(userRepository, never()).delete(admin);
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteEmployee_asUser_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/admin/employees/2").with(csrf()))
                .andExpect(status().isForbidden());
    }

    // --- PUT/DELETE /api/admin/workstations/{id} ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateWorkstation_success() throws Exception {
        Workstation ws = new Workstation();
        ws.setId(1L);
        ws.setTitle("Old");
        when(workstationRepository.findById(1L)).thenReturn(Optional.of(ws));

        mockMvc.perform(put("/api/admin/workstations/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "New", "employeeIds", List.of()))))
                .andExpect(status().isOk())
                .andExpect(content().string("Работното място е обновено успешно!"));

        assertEquals("New", ws.getTitle());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteWorkstation_onlyWorkstationOfProject_returnsBadRequest() throws Exception {
        Workstation ws = new Workstation();
        ws.setId(1L);
        Project project = new Project();
        project.setTitle("Alpha");
        project.setWorkstations(new HashSet<>(Set.of(ws)));
        when(workstationRepository.findById(1L)).thenReturn(Optional.of(ws));
        when(projectRepository.findByWorkstations_Id(1L)).thenReturn(List.of(project));

        mockMvc.perform(delete("/api/admin/workstations/1").with(csrf()))
                .andExpect(status().isBadRequest());

        verify(workstationRepository, never()).delete(ws);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteWorkstation_success_unassignsEmployees() throws Exception {
        Workstation ws = new Workstation();
        ws.setId(1L);
        User emp = existingUser(2L, "worker");
        emp.setWorkstation(ws);
        ws.getEmployees().add(emp);
        when(workstationRepository.findById(1L)).thenReturn(Optional.of(ws));

        mockMvc.perform(delete("/api/admin/workstations/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Работното място е изтрито успешно!"));

        assertNull(emp.getWorkstation());
        verify(workstationRepository).delete(ws);
    }

    // --- PUT/DELETE /api/admin/projects/{id} ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProject_success() throws Exception {
        Workstation ws = new Workstation();
        ws.setId(1L);
        Project project = new Project();
        project.setId(5L);
        project.setTitle("Old");
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(workstationRepository.findById(1L)).thenReturn(Optional.of(ws));

        mockMvc.perform(put("/api/admin/projects/5")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "New",
                                "dueDate", "2026-12-31",
                                "workstationIds", List.of(1)))))
                .andExpect(status().isOk())
                .andExpect(content().string("Проектът е обновен успешно!"));

        assertEquals("New", project.getTitle());
        assertEquals(Set.of(ws), project.getWorkstations());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProject_noWorkstations_returnsBadRequest() throws Exception {
        Project project = new Project();
        project.setId(5L);
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));

        mockMvc.perform(put("/api/admin/projects/5")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "New",
                                "dueDate", "2026-12-31",
                                "workstationIds", List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProject_success() throws Exception {
        Project project = new Project();
        project.setId(5L);
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));

        mockMvc.perform(delete("/api/admin/projects/5").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Проектът е изтрит успешно!"));

        verify(projectRepository).delete(project);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProject_notFound_returns404() throws Exception {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/admin/projects/99").with(csrf()))
                .andExpect(status().isNotFound());
    }
}
