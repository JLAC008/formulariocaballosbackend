package com.formulariocaballos.payment;

import com.formulariocaballos.booking.PaymentStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class MockPaymentService implements PaymentService {
    @Override
    public PaymentStatus charge(BigDecimal amount, String paymentMethod) {
        return PaymentStatus.APPROVED;
    }
}
