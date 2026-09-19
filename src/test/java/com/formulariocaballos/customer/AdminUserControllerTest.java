package com.formulariocaballos.customer;

import com.formulariocaballos.customer.dto.AdminUpdateUserRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserControllerTest {

    @Test
    void adminCanVerifyPendingUser() {
        CustomerUserRepository users = mock(CustomerUserRepository.class);
        CustomerUser user = pendingUser();
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(users.findByEmailIgnoreCase("pending@example.com")).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);

        AdminUserController controller = new AdminUserController(users, mock(PasswordEncoder.class));
        AdminUpdateUserRequest request = new AdminUpdateUserRequest(
            "Ana", "López", "+34600000000", "pending@example.com", "", "USER", 3, true, true
        );

        var response = controller.update(7L, request);

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(response.emailVerified()).isTrue();
        verify(users).save(user);
    }

    @Test
    void omittedVerificationStateKeepsCurrentValue() {
        CustomerUserRepository users = mock(CustomerUserRepository.class);
        CustomerUser user = pendingUser();
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(users.findByEmailIgnoreCase("pending@example.com")).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);

        AdminUserController controller = new AdminUserController(users, mock(PasswordEncoder.class));
        AdminUpdateUserRequest request = new AdminUpdateUserRequest(
            "Ana", "López", "+34600000000", "pending@example.com", "", "USER", 3, true, null
        );

        controller.update(7L, request);

        assertThat(user.isEmailVerified()).isFalse();
    }

    private CustomerUser pendingUser() {
        CustomerUser user = new CustomerUser();
        user.setId(7L);
        user.setFirstName("Ana");
        user.setLastName("López");
        user.setPhone("+34600000000");
        user.setEmail("pending@example.com");
        user.setPasswordHash("hash");
        user.setRole(Role.USER);
        user.setBonuses(3);
        user.setEmailVerified(false);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}
