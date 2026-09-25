package com.firmaa.techdept.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firmaa.techdept.models.User;
import com.firmaa.techdept.repositories.UserRepository;
import com.firmaa.techdept.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtUtils jwtUtils;

    // --- /api/auth/register ---

    @Test
    void register_success() throws Exception {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("hashed");

        User request = new User();
        request.setUsername("john");
        request.setPassword("pass123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully!"));
    }

    @Test
    void register_usernameTaken_returnsBadRequest() throws Exception {
        when(userRepository.existsByUsername("john")).thenReturn(true);

        User request = new User();
        request.setUsername("john");
        request.setPassword("pass123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Username is already taken!"));
    }

    // --- /api/auth/login ---

    @Test
    void login_success_returnsJwtToken() throws Exception {
        User storedUser = new User();
        storedUser.setUsername("john");
        storedUser.setPassword("hashed");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches("pass123", "hashed")).thenReturn(true);
        when(jwtUtils.generateJwtToken("john")).thenReturn("mock.jwt.token");

        User request = new User();
        request.setUsername("john");
        request.setPassword("pass123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("mock.jwt.token"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        User storedUser = new User();
        storedUser.setUsername("john");
        storedUser.setPassword("hashed");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches("wrongpass", "hashed")).thenReturn(false);

        User request = new User();
        request.setUsername("john");
        request.setPassword("wrongpass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Error: Invalid Credentials"));
    }

    @Test
    void login_userNotFound_returns500() throws Exception {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        User request = new User();
        request.setUsername("ghost");
        request.setPassword("pass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }
}
