package com.formulariocaballos.notification;

import com.formulariocaballos.booking.Booking;
import com.formulariocaballos.customer.CustomerUser;

public interface NotificationService {
    void bookingCreated(Booking booking);
    void bookingCancelled(Booking booking);
    void bonusesAdded(CustomerUser user, int addedBonuses);
}
