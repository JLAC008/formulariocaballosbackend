package com.formulariocaballos.payment.dto;

import jakarta.validation.constraints.Min;

public record CreateBonusCheckoutRequest(
    Long packId,
    @Min(1)
    Integer amount
) {}
