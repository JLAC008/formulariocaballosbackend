package com.formulariocaballos.auth;

import com.formulariocaballos.customer.CustomerUser;
import com.formulariocaballos.customer.CustomerUserRepository;
import com.formulariocaballos.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthTokenService {
    private final AuthTokenRepository tokens;
    private final CustomerUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public void sendVerification(CustomerUser user) {
        String token = create(user, TokenType.EMAIL_VERIFICATION);
        emailService.sendVerification(user.getEmail(), user.getFirstName(), token);
    }

    @Transactional
    public void verify(String value) {
        AuthToken token = usable(value, TokenType.EMAIL_VERIFICATION);
        CustomerUser user = token.getUser();
        user.setEmailVerified(true);
        token.setUsedAt(LocalDateTime.now());
        users.save(user);
        tokens.save(token);
    }

    @Transactional
    public void requestReset(String email) {
        users.findByEmailIgnoreCase(email.trim().toLowerCase()).ifPresent(user -> {
            String token = create(user, TokenType.PASSWORD_RESET);
            emailService.sendPasswordReset(user.getEmail(), token);
        });
    }

    @Transactional
    public void reset(String value, String password) {
        AuthToken token = usable(value, TokenType.PASSWORD_RESET);
        CustomerUser user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(password));
        token.setUsedAt(LocalDateTime.now());
        users.save(user);
        tokens.save(token);
    }

    private String create(CustomerUser user, TokenType type) {
        AuthToken token = new AuthToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString().replace("-", ""));
        token.setType(type);
        token.setExpiresAt(LocalDateTime.now().plusHours(type == TokenType.EMAIL_VERIFICATION ? 24 : 1));
        return tokens.save(token).getToken();
    }

    private AuthToken usable(String value, TokenType type) {
        AuthToken token = tokens.findByTokenAndType(value, type)
            .filter(AuthToken::isUsable)
            .orElseThrow(() -> new BusinessException("El token no es válido o ha caducado."));
        return token;
    }
}
