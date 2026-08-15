package com.formulariocaballos.payment;

import com.formulariocaballos.booking.PaymentStatus;

import java.math.BigDecimal;

public interface PaymentService {
    PaymentStatus charge(BigDecimal amount, String paymentMethod);
}
