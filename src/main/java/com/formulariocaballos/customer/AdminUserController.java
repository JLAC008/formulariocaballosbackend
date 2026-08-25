package com.formulariocaballos.customer;

import com.formulariocaballos.customer.dto.AdminCreateUserRequest;
import com.formulariocaballos.exception.BusinessException;
import com.formulariocaballos.state.dto.CustomerUserDto;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final CustomerUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AdminUserController(CustomerUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping
    public CustomerUserDto create(@Valid @RequestBody AdminCreateUserRequest request) {
        String email = request.email().trim().toLowerCase();
        users.findByEmailIgnoreCase(email).ifPresent(user -> {
            throw new BusinessException("Ya existe un usuario con ese email.");
        });

        CustomerUser user = new CustomerUser();
        user.setId(System.currentTimeMillis());
        user.setFirstName(cleanName(request.firstName()));
        user.setLastName(cleanName(request.lastName()));
        user.setPhone(SpanishPhoneNumber.normalize(request.phone()));
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(parseRole(request.role()));
        user.setBonuses(Math.max(0, request.sessions() == null ? 0 : request.sessions()));
        user.setActive(true);
        user.setEmailVerified(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return toDto(users.save(user));
    }

    private Role parseRole(String role) {
        try {
            return Role.valueOf(role.trim().toUpperCase());
        } catch (Exception exception) {
            throw new BusinessException("El rol debe ser USER o ADMIN.");
        }
    }

    private String cleanName(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private CustomerUserDto toDto(CustomerUser user) {
        return new CustomerUserDto(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getPhone(),
            user.getEmail(),
            user.getRole().name(),
            user.getBonuses(),
            user.isEmailVerified(),
            user.isActive(),
            user.getCreatedAt().toString(),
            user.getUpdatedAt().toString()
        );
    }
}
