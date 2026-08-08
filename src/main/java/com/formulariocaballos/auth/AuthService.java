package com.formulariocaballos.auth;

import com.formulariocaballos.auth.dto.AuthResponse;
import com.formulariocaballos.auth.dto.LoginRequest;
import com.formulariocaballos.auth.dto.RegisterRequest;
import com.formulariocaballos.customer.CustomerUser;
import com.formulariocaballos.customer.CustomerUserRepository;
import com.formulariocaballos.exception.BusinessException;
import com.formulariocaballos.security.JwtTokenProvider;
import com.formulariocaballos.state.dto.CustomerUserDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final String adminUsername;
    private final String adminPassword;
    private final CustomerUserRepository customerUserRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(@Value("${app.admin.username}") String adminUsername,
                       @Value("${app.admin.password}") String adminPassword,
                       CustomerUserRepository customerUserRepository,
                       JwtTokenProvider jwtTokenProvider) {
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.customerUserRepository = customerUserRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse login(LoginRequest request) {
        String username = request.username().trim().toLowerCase();

        if (adminUsername.equals(username) && adminPassword.equals(request.password())) {
            return new AuthResponse(jwtTokenProvider.generateToken(username, "ADMIN"), username, "ADMIN", null);
        }

        CustomerUser user = customerUserRepository.findByEmailIgnoreCase(username)
            .filter(existing -> existing.getPassword().equals(request.password()))
            .orElseThrow(() -> new BusinessException("Credenciales incorrectas."));

        return new AuthResponse(jwtTokenProvider.generateToken(user.getEmail(), user.getRole()), user.getEmail(), user.getRole(), toDto(user));
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        customerUserRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            throw new BusinessException("Ya existe un usuario con ese email.");
        });

        CustomerUser user = new CustomerUser();
        user.setId(System.currentTimeMillis());
        user.setName(request.name().trim());
        user.setPhone(request.phone().trim());
        user.setEmail(email);
        user.setPassword(request.password().trim());
        user.setRole("CUSTOMER");
        user.setBonuses(0);
        user.setCreatedAt(LocalDateTime.now());
        user = customerUserRepository.save(user);

        return new AuthResponse(jwtTokenProvider.generateToken(user.getEmail(), user.getRole()), user.getEmail(), user.getRole(), toDto(user));
    }

    private CustomerUserDto toDto(CustomerUser user) {
        return new CustomerUserDto(
            user.getId(),
            user.getName(),
            user.getPhone(),
            user.getEmail(),
            user.getPassword(),
            user.getRole(),
            user.getBonuses(),
            user.getCreatedAt().toString()
        );
    }
}
