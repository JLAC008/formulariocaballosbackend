package com.formulariocaballos.payment.dto;

import com.formulariocaballos.state.dto.CustomerUserDto;

public record BonusPaymentStatusResponse(
    String sessionId,
    String status,
    Integer bonuses,
    CustomerUserDto user
) {}
