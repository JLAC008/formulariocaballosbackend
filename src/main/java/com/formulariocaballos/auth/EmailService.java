package com.formulariocaballos.auth;

public interface EmailService {
    void sendVerification(String email, String firstName, String token);
    void sendPasswordReset(String email, String token);
}
