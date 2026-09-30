package com.firmaa.techdept.services;

import com.firmaa.techdept.dto.LoginRequest;
import com.firmaa.techdept.dto.LoginResponse;
import com.firmaa.techdept.exceptions.UnauthorizedException;
import com.firmaa.techdept.models.Role;
import com.firmaa.techdept.models.User;
import com.firmaa.techdept.repositories.UserRepository;
import com.firmaa.techdept.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    private static User admin() {
        User user = new User();
        user.setUsername("admin");
        user.setPassword("hash");
        user.setRole(Role.ROLE_ADMIN);
        return user;
    }

    @Test
    void login_correctPassword_returnsTokenAndRole() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin()));
        when(encoder.matches("secret", "hash")).thenReturn(true);
        when(jwtUtils.generateJwtToken("admin")).thenReturn("jwt-token");

        LoginResponse response = authService.login(new LoginRequest("admin", "secret"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.role()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void login_wrongPassword_isUnauthorized() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin()));
        when(encoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_unknownUser_isUnauthorized() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "x")))
                .isInstanceOf(UnauthorizedException.class);
    }
}
