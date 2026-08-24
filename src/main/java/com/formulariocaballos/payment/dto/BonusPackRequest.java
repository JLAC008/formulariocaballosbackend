package com.formulariocaballos.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record BonusPackRequest(
    @NotBlank String name,
    @Min(1) Integer bonuses,
    @Min(100) Long priceCents,
    String currency,
    Boolean active
) {}
