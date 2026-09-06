package com.formulariocaballos.payment.dto;

public record BonusCheckoutResponse(
    String orderId,
    String url,
    String signatureVersion,
    String merchantParameters,
    String signature
) {}
