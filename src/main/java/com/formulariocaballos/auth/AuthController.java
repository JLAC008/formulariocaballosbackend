package com.formulariocaballos.auth;

import com.formulariocaballos.auth.dto.AuthResponse;
import com.formulariocaballos.auth.dto.LoginRequest;
import com.formulariocaballos.auth.dto.RegisterRequest;
import com.formulariocaballos.auth.dto.ForgotPasswordRequest;
import com.formulariocaballos.auth.dto.ResetPasswordRequest;
import com.formulariocaballos.state.dto.CustomerUserDto;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<CustomerUserDto> me(Authentication authentication) {
        return ResponseEntity.ok(authService.me(authentication.getName()));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.resendVerification(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.noContent().build();
    }
}
