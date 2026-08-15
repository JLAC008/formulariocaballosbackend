package com.formulariocaballos.booking.dto;

import com.formulariocaballos.booking.ReservationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateBookingStatusRequest(@NotNull ReservationStatus status) {}
