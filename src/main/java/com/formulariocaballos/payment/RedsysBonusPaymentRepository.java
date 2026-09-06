package com.formulariocaballos.payment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RedsysBonusPaymentRepository extends JpaRepository<RedsysBonusPayment, String> {
    boolean existsByBonusPackId(Long bonusPackId);
}
