package com.firmaa.techdept.controllers;

import com.firmaa.techdept.dto.LoginRequest;
import com.firmaa.techdept.dto.LoginResponse;
import com.firmaa.techdept.services.AuthService;
import org.springframework.web.bind.annotation.*;

/** Public login endpoint; everything else in the API requires the token it returns. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse authenticateUser(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
