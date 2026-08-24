package com.formulariocaballos.auth;

import com.formulariocaballos.auth.dto.LoginRequest;
import com.formulariocaballos.auth.dto.RegisterRequest;
import com.formulariocaballos.customer.CustomerUser;
import com.formulariocaballos.customer.CustomerUserRepository;
import com.formulariocaballos.customer.Role;
import com.formulariocaballos.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void registerCreatesPendingUserAndSendsVerification() {
        CustomerUserRepository users = mock(CustomerUserRepository.class);
        AuthTokenService tokens = mock(AuthTokenService.class);
        when(users.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(users.save(any(CustomerUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthService service = new AuthService("admin@example.com", "AdminPass1", users,
            mock(JwtTokenProvider.class), new BCryptPasswordEncoder(), tokens);

        var response = service.register(new RegisterRequest("Ana", "Lopez", "+34600000000", "new@example.com", "ValidPass1"));

        assertThat(response.token()).isNull();
        assertThat(response.verificationResent()).isFalse();
        verify(tokens).sendVerification(argThat(user ->
            user.getEmail().equals("new@example.com")
                && user.getRole() == Role.USER
                && !user.isEmailVerified()
        ));
    }

    @Test
    void registerUpdatesPendingUserAndResendsVerification() {
        CustomerUserRepository users = mock(CustomerUserRepository.class);
        AuthTokenService tokens = mock(AuthTokenService.class);
        CustomerUser existing = new CustomerUser();
        existing.setId(12L);
        existing.setEmail("pending@example.com");
        existing.setEmailVerified(false);
        existing.setBonuses(4);
        existing.setRole(Role.USER);
        existing.setCreatedAt(LocalDateTime.now().minusDays(1));
        when(users.findByEmailIgnoreCase("pending@example.com")).thenReturn(Optional.of(existing));
        when(users.save(any(CustomerUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthService service = new AuthService("admin@example.com", "AdminPass1", users,
            mock(JwtTokenProvider.class), new BCryptPasswordEncoder(), tokens);

        var response = service.register(new RegisterRequest("Nuevo", "Nombre", "+34611111111", "pending@example.com", "ValidPass1"));

        assertThat(response.token()).isNull();
        assertThat(response.verificationResent()).isTrue();
        assertThat(existing.getFirstName()).isEqualTo("Nuevo");
        assertThat(existing.getLastName()).isEqualTo("Nombre");
        assertThat(existing.getBonuses()).isEqualTo(4);
        assertThat(existing.isActive()).isTrue();
        assertThat(existing.isEmailVerified()).isFalse();
        verify(tokens).sendVerification(existing);
    }

    @Test
    void registerRejectsVerifiedUser() {
        CustomerUserRepository users = mock(CustomerUserRepository.class);
        AuthTokenService tokens = mock(AuthTokenService.class);
        CustomerUser existing = new CustomerUser();
        existing.setEmail("verified@example.com");
        existing.setEmailVerified(true);
        when(users.findByEmailIgnoreCase("verified@example.com")).thenReturn(Optional.of(existing));

        AuthService service = new AuthService("admin@example.com", "AdminPass1", users,
            mock(JwtTokenProvider.class), new BCryptPasswordEncoder(), tokens);

        assertThatThrownBy(() -> service.register(new RegisterRequest("Ana", "Lopez", "+34600000000", "verified@example.com", "ValidPass1")))
            .hasMessageContaining("Ya existe un usuario con ese email");
        verify(tokens, never()).sendVerification(any());
    }
}
