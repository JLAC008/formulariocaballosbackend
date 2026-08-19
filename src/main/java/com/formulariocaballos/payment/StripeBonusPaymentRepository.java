package com.formulariocaballos.payment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeBonusPaymentRepository extends JpaRepository<StripeBonusPayment, String> {
}
