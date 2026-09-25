package com.firmaa.techdept.controllers;

import com.firmaa.techdept.security.JwtUtils;
import com.firmaa.techdept.models.User;
import com.firmaa.techdept.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody User loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);

        if (user == null || !encoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid username or password");
        }

        String jwt = jwtUtils.generateJwtToken(user.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("token", jwt);
        response.put("username", user.getUsername());
        response.put("role", user.getRole().name());

        return ResponseEntity.ok(response);
    }
}