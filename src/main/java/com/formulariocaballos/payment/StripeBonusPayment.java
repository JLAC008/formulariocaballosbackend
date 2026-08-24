package com.formulariocaballos.payment;

import com.formulariocaballos.customer.CustomerUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stripe_bonus_payments")
public class StripeBonusPayment {
    @Id
    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private CustomerUser user;

    @Column(nullable = false)
    private Integer bonuses;

    @Column(name = "bonus_pack_id")
    private Long bonusPackId;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StripeBonusPaymentStatus status = StripeBonusPaymentStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
