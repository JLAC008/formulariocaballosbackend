package com.formulariocaballos.auth;

import com.formulariocaballos.auth.dto.LoginRequest;
import com.formulariocaballos.customer.CustomerUser;
import com.formulariocaballos.customer.CustomerUserRepository;
import com.formulariocaballos.customer.Role;
import com.formulariocaballos.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    @Test
    void rejectsUnverifiedUsers() {
        CustomerUserRepository users = mock(CustomerUserRepository.class);
        CustomerUser user = new CustomerUser();
        user.setEmail("user@example.com");
        user.setPasswordHash(new BCryptPasswordEncoder().encode("ValidPass1"));
        user.setRole(Role.USER);
        user.setActive(true);
        user.setEmailVerified(false);
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        AuthService service = new AuthService("admin@example.com", "AdminPass1", users,
            mock(JwtTokenProvider.class), new BCryptPasswordEncoder(), mock(AuthTokenService.class));

        assertThatThrownBy(() -> service.login(new LoginRequest("user@example.com", "ValidPass1")))
            .hasMessageContaining("Credenciales incorrectas");
    }

    @Test
    void passwordResetReportsUnknownEmail() {
        CustomerUserRepository users = mock(CustomerUserRepository.class);
        when(users.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        AuthService service = new AuthService("admin@example.com", "AdminPass1", users,
            mock(JwtTokenProvider.class), new BCryptPasswordEncoder(), mock(AuthTokenService.class));

        assertThatThrownBy(() -> service.requestPasswordReset("missing@example.com"))
            .hasMessageContaining("No existe ningun usuario con ese email");
    }
}
