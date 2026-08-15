package com.formulariocaballos.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.mail.MailException;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailService implements EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.frontend-url}")
    private String frontendUrl;

    @Override
    public void sendVerification(String email, String token) {
        send(email, "Confirma tu cuenta", "Confirma tu cuenta: " + frontendUrl + "/verify-email?token=" + token);
    }

    @Override
    public void sendPasswordReset(String email, String token) {
        send(email, "Restablece tu contrasena", "Restablece tu contrasena: " + frontendUrl + "/reset-password?token=" + token);
    }

    private void send(String recipient, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(text);
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.error("No se pudo enviar el correo a {}", recipient, exception);
        }
    }
}
