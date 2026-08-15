package com.formulariocaballos.notification;

import com.formulariocaballos.booking.Booking;

public interface NotificationService {
    void bookingCreated(Booking booking);
    void bookingCancelled(Booking booking);
}
