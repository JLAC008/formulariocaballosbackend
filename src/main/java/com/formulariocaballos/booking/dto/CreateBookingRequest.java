package com.formulariocaballos.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateBookingRequest(
    @NotNull Long experienceId,
    @NotNull LocalDate dateKey,
    @NotBlank @jakarta.validation.constraints.Pattern(regexp = "^(?:[01]\\d|2[0-3]):[0-5]\\d$", message = "La hora no tiene un formato válido") String hour,
    String date,
    String payment,
    String customerName,
    @jakarta.validation.constraints.Pattern(regexp = "^[+0-9\\s-]{9,20}$", message = "El teléfono no tiene un formato válido") String phone
) {}
