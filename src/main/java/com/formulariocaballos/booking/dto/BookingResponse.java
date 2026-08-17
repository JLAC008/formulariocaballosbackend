package com.formulariocaballos.booking.dto;

import com.formulariocaballos.booking.Booking;
import com.formulariocaballos.booking.PaymentStatus;
import com.formulariocaballos.booking.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingResponse(Long id, Long userId, Long experienceId, String type, String title,
                              LocalDate dateKey, String hour, BigDecimal amount,
                              PaymentStatus paymentStatus, ReservationStatus status) {
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(booking.getId(), booking.getUser() == null ? null : booking.getUser().getId(),
            booking.getExperience().getId(), booking.getType(), booking.getTitle(), booking.getDateKey(),
            booking.getHour(), booking.getAmount(), booking.getPaymentStatus(), booking.getStatus());
    }
}
