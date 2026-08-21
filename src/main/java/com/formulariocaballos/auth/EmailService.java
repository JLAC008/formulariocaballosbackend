package com.formulariocaballos.auth;

import com.formulariocaballos.booking.Booking;

public interface EmailService {
    void sendVerification(String email, String firstName, String token);
    void sendPasswordReset(String email, String token);
    void sendBookingCancellation(Booking booking);
}
