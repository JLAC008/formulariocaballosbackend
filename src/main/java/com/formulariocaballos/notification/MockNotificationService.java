package com.formulariocaballos.notification;

import com.formulariocaballos.booking.Booking;
import org.springframework.stereotype.Service;

@Service
public class MockNotificationService implements NotificationService {
    @Override
    public void bookingCreated(Booking booking) {
        // Extension point for WhatsApp notifications.
    }

    @Override
    public void bookingCancelled(Booking booking) {
        // Extension point for WhatsApp notifications.
    }
}
