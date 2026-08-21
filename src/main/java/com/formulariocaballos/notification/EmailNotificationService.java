package com.formulariocaballos.notification;

import com.formulariocaballos.auth.EmailService;
import com.formulariocaballos.booking.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService implements NotificationService {
    private final EmailService emailService;

    @Override
    public void bookingCreated(Booking booking) {
        // Reserved for future booking confirmation emails.
    }

    @Override
    public void bookingCancelled(Booking booking) {
        try {
            emailService.sendBookingCancellation(booking);
        } catch (Exception exception) {
            log.error("No se pudo enviar el correo de cancelación de la reserva {}", booking.getId(), exception);
        }
    }
}
