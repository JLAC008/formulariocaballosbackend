package com.formulariocaballos.payment.dto;

import com.formulariocaballos.state.dto.CustomerUserDto;

public record BonusPaymentStatusResponse(
    String orderId,
    String status,
    Integer bonuses,
    CustomerUserDto user
) {}
