package com.formulariocaballos.payment.dto;

public record BonusCheckoutResponse(
    String sessionId,
    String url
) {}
