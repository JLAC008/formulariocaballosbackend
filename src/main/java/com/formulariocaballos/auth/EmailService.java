package com.formulariocaballos.auth;

import com.formulariocaballos.booking.Booking;
import com.formulariocaballos.customer.CustomerUser;

public interface EmailService {
    void sendVerification(String email, String firstName, String token);
    void sendPasswordReset(String email, String token);
    void sendBookingConfirmation(Booking booking);
    void sendBookingCancellation(Booking booking);
    void sendBonusesAdded(CustomerUser user, int addedBonuses);
}
