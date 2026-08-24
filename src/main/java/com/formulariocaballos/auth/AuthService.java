package com.formulariocaballos.auth;

import com.formulariocaballos.auth.dto.AuthResponse;
import com.formulariocaballos.auth.dto.ChangePasswordRequest;
import com.formulariocaballos.auth.dto.LoginRequest;
import com.formulariocaballos.auth.dto.RegisterRequest;
import com.formulariocaballos.auth.dto.UpdateProfileRequest;
import com.formulariocaballos.customer.CustomerUser;
import com.formulariocaballos.customer.CustomerUserRepository;
import com.formulariocaballos.customer.Role;
import com.formulariocaballos.customer.SpanishPhoneNumber;
import com.formulariocaballos.exception.BusinessException;
import com.formulariocaballos.security.JwtTokenProvider;
import com.formulariocaballos.state.dto.CustomerUserDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final String adminUsername;
    private final String adminPasswordHash;
    private final CustomerUserRepository customerUserRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;

    public AuthService(@Value("${app.admin.username}") String adminUsername,
                       @Value("${app.admin.password}") String adminPassword,
                       CustomerUserRepository customerUserRepository,
                       JwtTokenProvider jwtTokenProvider,
                       PasswordEncoder passwordEncoder,
                       AuthTokenService authTokenService) {
        this.adminUsername = adminUsername.trim().toLowerCase();
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
        this.adminPasswordHash = passwordEncoder.encode(adminPassword);
        this.customerUserRepository = customerUserRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse login(LoginRequest request) {
        String username = request.email().trim().toLowerCase();

        if (adminUsername.equals(username) && passwordEncoder.matches(request.password(), adminPasswordHash)) {
            return new AuthResponse(jwtTokenProvider.generateToken(username, "ADMIN"), username, "ADMIN", null, false);
        }

        CustomerUser user = customerUserRepository.findByEmailIgnoreCase(username)
            .filter(existing -> existing.isActive() && existing.isEmailVerified()
                && passwordEncoder.matches(request.password(), existing.getPasswordHash()))
            .orElseThrow(() -> new BusinessException("Credenciales incorrectas."));

        return new AuthResponse(jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name()), user.getEmail(), user.getRole().name(), toDto(user), false);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        return customerUserRepository.findByEmailIgnoreCase(email)
            .map(existing -> resendVerificationForPendingUser(existing, request, email))
            .orElseGet(() -> createPendingUser(request, email));
    }

    private AuthResponse createPendingUser(RegisterRequest request, String email) {
        CustomerUser user = new CustomerUser();
        user.setId(System.currentTimeMillis());
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(SpanishPhoneNumber.normalize(request.phone()));
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setBonuses(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user = customerUserRepository.save(user);
        authTokenService.sendVerification(user);

        return new AuthResponse(null, user.getEmail(), user.getRole().name(), toDto(user), false);
    }

    private AuthResponse resendVerificationForPendingUser(CustomerUser user, RegisterRequest request, String email) {
        if (user.isEmailVerified()) {
            throw new BusinessException("Ya existe un usuario con ese email.");
        }

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(SpanishPhoneNumber.normalize(request.phone()));
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setActive(true);
        user.setEmailVerified(false);
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }
        user.setUpdatedAt(LocalDateTime.now());
        user = customerUserRepository.save(user);
        authTokenService.sendVerification(user);

        return new AuthResponse(null, user.getEmail(), user.getRole().name(), toDto(user), true);
    }

    public void verifyEmail(String token) { authTokenService.verify(token); }

    public void requestPasswordReset(String email) {
        customerUserRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
            .orElseThrow(() -> new BusinessException("No existe ningun usuario con ese email."));
        authTokenService.requestReset(email);
    }

    public void resetPassword(String token, String password) { authTokenService.reset(token, password); }

    public void resendVerification(String email) {
        customerUserRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
            .filter(user -> !user.isEmailVerified())
            .ifPresent(authTokenService::sendVerification);
    }

    public CustomerUserDto me(String email) {
        CustomerUser user = customerUserRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
            .orElseThrow(() -> new com.formulariocaballos.exception.ResourceNotFoundException("User not found"));
        return toDto(user);
    }

    @Transactional
    public CustomerUserDto updateProfile(String email, UpdateProfileRequest request) {
        CustomerUser user = customerUserRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
            .filter(existing -> existing.isActive() && existing.isEmailVerified())
            .orElseThrow(() -> new com.formulariocaballos.exception.ResourceNotFoundException("User not found"));

        user.setFirstName(request.firstName().trim().replaceAll("\\s+", " "));
        user.setLastName(request.lastName().trim().replaceAll("\\s+", " "));
        user.setPhone(SpanishPhoneNumber.normalize(request.phone()));
        user.setUpdatedAt(LocalDateTime.now());
        return toDto(customerUserRepository.save(user));
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        CustomerUser user = customerUserRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
            .filter(existing -> existing.isActive() && existing.isEmailVerified())
            .orElseThrow(() -> new com.formulariocaballos.exception.ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("La contraseña actual no es correcta.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        customerUserRepository.save(user);
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
