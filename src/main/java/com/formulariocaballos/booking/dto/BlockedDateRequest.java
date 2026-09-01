package com.formulariocaballos.booking.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record BlockedDateRequest(@NotNull LocalDate dateKey) {}
