package com.formulariocaballos.state.dto;

import com.formulariocaballos.booking.ReservationStatus;

import java.math.BigDecimal;

public record BookingDto(
    Long id,
    Long userId,
    Long experienceId,
    String type,
    String title,
    String date,
    String dateKey,
    String hour,
    String payment,
    String customerName,
    String phone,
    BigDecimal amount,
    ReservationStatus status
) {}
