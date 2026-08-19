package com.formulariocaballos.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formulariocaballos.booking.Booking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class WhatsAppNotificationService implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger(WhatsAppNotificationService.class);
    private static final String GRAPH_VERSION = "v21.0";

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String accessToken;
    private final String phoneNumberId;
    private final String adminTo;
    private final String bookingTemplate;
    private final String cancellationTemplate;
    private final String language;
    private final WhatsAppMessageSender sender;

    public WhatsAppNotificationService(ObjectMapper objectMapper,
                                       @Value("${app.whatsapp.enabled:false}") boolean enabled,
                                       @Value("${app.whatsapp.access-token:}") String accessToken,
                                       @Value("${app.whatsapp.phone-number-id:}") String phoneNumberId,
                                       @Value("${app.whatsapp.admin-to:}") String adminTo,
                                       @Value("${app.whatsapp.template-booking:booking_confirmation}") String bookingTemplate,
                                       @Value("${app.whatsapp.template-cancellation:booking_cancellation}") String cancellationTemplate,
                                       @Value("${app.whatsapp.template-language:es_ES}") String language) {
        this(objectMapper, enabled, accessToken, phoneNumberId, adminTo, bookingTemplate, cancellationTemplate,
            language, new HttpWhatsAppMessageSender());
    }

    WhatsAppNotificationService(ObjectMapper objectMapper, boolean enabled, String accessToken,
                                String phoneNumberId, String adminTo, String bookingTemplate,
                                String cancellationTemplate, String language, WhatsAppMessageSender sender) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.accessToken = accessToken;
        this.phoneNumberId = phoneNumberId;
        this.adminTo = adminTo;
        this.bookingTemplate = bookingTemplate;
        this.cancellationTemplate = cancellationTemplate;
        this.language = language;
        this.sender = sender;
    }

    @Override
    public void bookingCreated(Booking booking) {
        sendTemplate(bookingTemplate, booking);
    }

    @Override
    public void bookingCancelled(Booking booking) {
        sendTemplate(cancellationTemplate, booking);
    }

    private void sendTemplate(String templateName, Booking booking) {
        if (!enabled) {
            return;
        }
        if (!isConfigured(templateName)) {
            log.warn("WhatsApp no esta configurado; se omite la notificacion.");
            return;
        }

        for (String recipient : recipients(booking)) {
            try {
                sender.send(endpoint(), accessToken, buildTemplatePayload(recipient, templateName, booking));
            } catch (Exception ex) {
                log.warn("No se pudo enviar WhatsApp de reserva a {}.", recipient, ex);
            }
        }
    }

    String buildTemplatePayload(String to, String templateName, Booking booking) throws JsonProcessingException {
        List<Map<String, String>> parameters = List.of(
            textParameter(valueOrFallback(booking.getCustomerName(), "Cliente")),
            textParameter(typeLabel(booking)),
            textParameter(valueOrFallback(booking.getTitle(), "Experiencia")),
            textParameter(valueOrFallback(booking.getDate(), booking.getDateKey() == null ? "" : booking.getDateKey().toString())),
            textParameter(valueOrFallback(booking.getHour(), "")),
            textParameter(valueOrFallback(booking.getPhone(), ""))
        );

        Map<String, Object> payload = Map.of(
            "messaging_product", "whatsapp",
            "recipient_type", "individual",
            "to", to,
            "type", "template",
            "template", Map.of(
                "name", templateName,
                "language", Map.of("code", language),
                "components", List.of(Map.of(
                    "type", "body",
                    "parameters", parameters
                ))
            )
        );

        return objectMapper.writeValueAsString(payload);
    }

    List<String> recipients(Booking booking) {
        Set<String> values = new LinkedHashSet<>();
        addRecipient(values, adminTo);
        if (booking != null) {
            addRecipient(values, booking.getPhone());
        }
        return new ArrayList<>(values);
    }

    private void addRecipient(Set<String> values, String phone) {
        String normalized = normalizePhone(phone);
        if (StringUtils.hasText(normalized)) {
            values.add(normalized);
        }
    }

    private boolean isConfigured(String templateName) {
        return StringUtils.hasText(accessToken)
            && StringUtils.hasText(phoneNumberId)
            && StringUtils.hasText(templateName);
    }

    private String endpoint() {
        return "https://graph.facebook.com/" + GRAPH_VERSION + "/" + phoneNumberId + "/messages";
    }

    private Map<String, String> textParameter(String value) {
        return Map.of("type", "text", "text", value);
    }

    private String typeLabel(Booking booking) {
        String type = booking == null ? "" : booking.getType();
        return ("routes".equalsIgnoreCase(type) || "route".equalsIgnoreCase(type)) ? "ruta" : "clase";
    }

    private String valueOrFallback(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return "";
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("00")) {
            digits = digits.substring(2);
        }
        return digits;
    }

    interface WhatsAppMessageSender {
        void send(String url, String accessToken, String payload) throws IOException, InterruptedException;
    }

    private static final class HttpWhatsAppMessageSender implements WhatsAppMessageSender {
        private final HttpClient client = HttpClient.newHttpClient();

        @Override
        public void send(String url, String accessToken, String payload) throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("WhatsApp API returned " + response.statusCode() + ": " + response.body());
            }
        }
    }
}
