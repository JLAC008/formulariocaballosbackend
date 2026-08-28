package com.formulariocaballos.customer;

import com.formulariocaballos.auth.AuthTokenRepository;
import com.formulariocaballos.booking.BookingRepository;
import com.formulariocaballos.customer.dto.AdminCreateUserRequest;
import com.formulariocaballos.customer.dto.AdminUpdateUserRequest;
import com.formulariocaballos.exception.BusinessException;
import com.formulariocaballos.payment.StripeBonusPaymentRepository;
import com.formulariocaballos.state.dto.CustomerUserDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final CustomerUserRepository users;
    private final AuthTokenRepository authTokens;
    private final BookingRepository bookings;
    private final StripeBonusPaymentRepository payments;
    private final PasswordEncoder passwordEncoder;

    public AdminUserController(CustomerUserRepository users,
                               AuthTokenRepository authTokens,
                               BookingRepository bookings,
                               StripeBonusPaymentRepository payments,
                               PasswordEncoder passwordEncoder) {
        this.users = users;
        this.authTokens = authTokens;
        this.bookings = bookings;
        this.payments = payments;
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

    @PutMapping("/{id}")
    public CustomerUserDto update(@PathVariable Long id, @Valid @RequestBody AdminUpdateUserRequest request) {
        CustomerUser user = users.findById(id)
            .orElseThrow(() -> new BusinessException("No se ha encontrado el usuario."));
        String email = request.email().trim().toLowerCase();

        users.findByEmailIgnoreCase(email)
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new BusinessException("Ya existe un usuario con ese email.");
            });

        user.setFirstName(cleanName(request.firstName()));
        user.setLastName(cleanName(request.lastName()));
        user.setPhone(SpanishPhoneNumber.normalize(request.phone()));
        user.setEmail(email);
        user.setRole(parseRole(request.role()));
        user.setBonuses(Math.max(0, request.sessions() == null ? 0 : request.sessions()));
        user.setActive(request.active() == null || request.active());

        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        return toDto(users.save(user));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        CustomerUser user = users.findById(id)
            .orElseThrow(() -> new BusinessException("No se ha encontrado el usuario."));

        if (authentication != null && user.getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new BusinessException("No puedes eliminar tu propio usuario administrador desde esta sesión.");
        }

        authTokens.deleteByUserId(id);
        bookings.deleteByUserId(id);
        payments.deleteByUserId(id);
        users.delete(user);

        return ResponseEntity.noContent().build();
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
