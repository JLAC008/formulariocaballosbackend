package com.formulariocaballos.payment.dto;

import jakarta.validation.constraints.Min;

public record CreateBonusCheckoutRequest(
    @Min(1)
    Integer amount
) {}
