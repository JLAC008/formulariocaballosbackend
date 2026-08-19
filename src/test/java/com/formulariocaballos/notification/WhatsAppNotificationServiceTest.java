package com.formulariocaballos.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formulariocaballos.booking.Booking;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class WhatsAppNotificationServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void doesNotSendWhenDisabled() {
        CapturingSender sender = new CapturingSender();
        WhatsAppNotificationService service = service(false, sender);

        service.bookingCreated(booking());

        assertThat(sender.payloads).isEmpty();
    }

    @Test
    void buildsTemplatePayloadWithBookingData() throws Exception {
        CapturingSender sender = new CapturingSender();
        WhatsAppNotificationService service = service(true, sender);

        service.bookingCreated(booking());

        assertThat(sender.urls).containsExactly("https://graph.facebook.com/v21.0/12345/messages",
            "https://graph.facebook.com/v21.0/12345/messages");
        JsonNode payload = objectMapper.readTree(sender.payloads.get(0));
        assertThat(payload.path("messaging_product").asText()).isEqualTo("whatsapp");
        assertThat(payload.path("to").asText()).isEqualTo("34611111111");
        assertThat(payload.path("template").path("name").asText()).isEqualTo("booking_confirmation");
        JsonNode parameters = payload.path("template").path("components").get(0).path("parameters");
        assertThat(parameters.get(0).path("text").asText()).isEqualTo("Usuario Demo");
        assertThat(parameters.get(1).path("text").asText()).isEqualTo("ruta");
        assertThat(parameters.get(2).path("text").asText()).isEqualTo("Ruta Sendero");
        assertThat(parameters.get(3).path("text").asText()).isEqualTo("Miercoles, 19 de Agosto 2026");
        assertThat(parameters.get(4).path("text").asText()).isEqualTo("18:00");
        assertThat(parameters.get(5).path("text").asText()).isEqualTo("+34600000000");
    }

    @Test
    void doesNotPropagateSenderErrors() {
        WhatsAppNotificationService service = service(true, (url, token, payload) -> {
            throw new IOException("Meta rejected message");
        });

        assertThatCode(() -> service.bookingCreated(booking())).doesNotThrowAnyException();
    }

    private WhatsAppNotificationService service(boolean enabled, WhatsAppNotificationService.WhatsAppMessageSender sender) {
        return new WhatsAppNotificationService(objectMapper, enabled, "token", "12345", "34611111111",
            "booking_confirmation", "booking_cancellation", "es_ES", sender);
    }

    private Booking booking() {
        Booking booking = new Booking();
        booking.setType("routes");
        booking.setTitle("Ruta Sendero");
        booking.setDate("Miercoles, 19 de Agosto 2026");
        booking.setDateKey(LocalDate.of(2026, 8, 19));
        booking.setHour("18:00");
        booking.setCustomerName("Usuario Demo");
        booking.setPhone("+34600000000");
        return booking;
    }

    private static final class CapturingSender implements WhatsAppNotificationService.WhatsAppMessageSender {
        private final List<String> urls = new ArrayList<>();
        private final List<String> payloads = new ArrayList<>();

        @Override
        public void send(String url, String accessToken, String payload) {
            urls.add(url);
            payloads.add(payload);
        }
    }
}
