package com.formulariocaballos.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrevoEmailService implements EmailService {
    private static final String BREVO_ENDPOINT = "https://api.brevo.com/v3/smtp/email";
    private static final String SENDER_NAME = "Martínez Luna";

    private final ObjectMapper objectMapper;
    private final SpringTemplateEngine templateEngine;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.brevo-api-key}")
    private String apiKey;

    @Value("${app.mail.frontend-url}")
    private String frontendUrl;

    @Override
    public void sendVerification(String email, String firstName, String token) {
        String link = frontendUrl + "/verify-email?token=" + token;

        Context context = new Context();
        context.setVariable("name", firstName);
        context.setVariable("link", link);
        String html = templateEngine.process("email/verification", context);

        sendHtml(email, firstName, "Confirma tu cuenta", html);
    }

    @Override
    public void sendPasswordReset(String email, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        sendHtml(email, "", "Restablece tu contrasena", "<p>Restablece tu contrasena: <a href=\"" + link + "\">" + link + "</a></p>");
    }

    private void sendHtml(String recipient, String name, String subject, String html) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sender", Map.of("email", from, "name", SENDER_NAME));
            payload.put("to", List.of(Map.of("email", recipient, "name", name)));
            payload.put("subject", subject);
            payload.put("htmlContent", html);
            payload.put("textContent", stripHtml(html));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BREVO_ENDPOINT))
                .timeout(Duration.ofSeconds(20))
                .header("accept", "application/json")
                .header("api-key", apiKey)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Brevo rechazó el envío a {} ({}): {}", recipient, response.statusCode(), response.body());
            }
        } catch (Exception exception) {
            log.error("No se pudo enviar el correo a {}", recipient, exception);
        }
    }

    private String stripHtml(String html) {
        return html.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }
}