package com.firmaa.techdept.services;

import com.firmaa.techdept.dto.LoginRequest;
import com.firmaa.techdept.dto.LoginResponse;
import com.firmaa.techdept.exceptions.UnauthorizedException;
import com.firmaa.techdept.models.User;
import com.firmaa.techdept.repositories.UserRepository;
import com.firmaa.techdept.security.JwtUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Checks credentials and issues the JWT the frontend sends with every later request. */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    public AuthService(UserRepository userRepository, PasswordEncoder encoder, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = request.username() == null ? null
                : userRepository.findByUsername(request.username()).orElse(null);

        // Same message for an unknown user and a wrong password, so usernames can't be probed
        if (user == null || request.password() == null || !encoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        return new LoginResponse(jwtUtils.generateJwtToken(user.getUsername()), user.getUsername(), user.getRole().name());
    }
}
