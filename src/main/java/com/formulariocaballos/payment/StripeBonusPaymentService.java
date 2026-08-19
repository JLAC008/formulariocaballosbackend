package com.formulariocaballos.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formulariocaballos.customer.CustomerUser;
import com.formulariocaballos.customer.CustomerUserRepository;
import com.formulariocaballos.exception.BusinessException;
import com.formulariocaballos.exception.ResourceNotFoundException;
import com.formulariocaballos.payment.dto.BonusCheckoutResponse;
import com.formulariocaballos.payment.dto.BonusPaymentStatusResponse;
import com.formulariocaballos.state.dto.CustomerUserDto;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class StripeBonusPaymentService {
    private final CustomerUserRepository users;
    private final StripeBonusPaymentRepository payments;
    private final ObjectMapper objectMapper;
    private final String stripeSecretKey;
    private final String webhookSecret;
    private final String frontendUrl;
    private final String currency;
    private final int bonusPackAmount;
    private final long bonusPackPriceCents;

    public StripeBonusPaymentService(CustomerUserRepository users,
                                     StripeBonusPaymentRepository payments,
                                     ObjectMapper objectMapper,
                                     @Value("${app.stripe.secret-key}") String stripeSecretKey,
                                     @Value("${app.stripe.webhook-secret}") String webhookSecret,
                                     @Value("${app.mail.frontend-url}") String frontendUrl,
                                     @Value("${app.stripe.currency}") String currency,
                                     @Value("${app.stripe.bonus-pack-amount}") int bonusPackAmount,
                                     @Value("${app.stripe.bonus-pack-price-cents}") long bonusPackPriceCents) {
        this.users = users;
        this.payments = payments;
        this.objectMapper = objectMapper;
        this.stripeSecretKey = stripeSecretKey;
        this.webhookSecret = webhookSecret;
        this.frontendUrl = frontendUrl;
        this.currency = currency;
        this.bonusPackAmount = bonusPackAmount;
        this.bonusPackPriceCents = bonusPackPriceCents;
    }

    @Transactional
    public BonusCheckoutResponse createCheckout(String email, Integer amount) {
        if (amount == null || amount != bonusPackAmount) {
            throw new BusinessException("Pack de bonos no valido.");
        }
        ensureStripeConfigured();

        CustomerUser user = users.findByEmailIgnoreCase(email)
            .filter(existing -> existing.isActive())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        try {
            Stripe.apiKey = stripeSecretKey;
            SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontendUrl + "/?stripe_bonus=success&session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/?stripe_bonus=cancel")
                .setCustomerEmail(user.getEmail())
                .putAllMetadata(Map.of(
                    "userId", String.valueOf(user.getId()),
                    "bonuses", String.valueOf(bonusPackAmount)
                ))
                .addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(currency)
                        .setUnitAmount(bonusPackPriceCents)
                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                            .setName(bonusPackAmount + " bonos Martinez Luna")
                            .build())
                        .build())
                    .build())
                .build();

            Session session = Session.create(params);
            StripeBonusPayment payment = new StripeBonusPayment();
            payment.setSessionId(session.getId());
            payment.setUser(user);
            payment.setBonuses(bonusPackAmount);
            payment.setAmountCents(bonusPackPriceCents);
            payment.setCurrency(currency);
            payment.setStatus(StripeBonusPaymentStatus.PENDING);
            payments.save(payment);

            return new BonusCheckoutResponse(session.getId(), session.getUrl());
        } catch (StripeException ex) {
            throw new BusinessException("No se pudo iniciar el pago con Stripe.");
        }
    }

    @Transactional
    public BonusPaymentStatusResponse refreshStatus(String email, String sessionId) {
        ensureStripeConfigured();
        StripeBonusPayment payment = payments.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (!payment.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new BusinessException("El pago no pertenece a este usuario.");
        }

        try {
            Stripe.apiKey = stripeSecretKey;
            Session session = Session.retrieve(sessionId);
            if ("paid".equalsIgnoreCase(session.getPaymentStatus())) {
                complete(payment);
            }
            return toStatusResponse(payment);
        } catch (StripeException ex) {
            throw new BusinessException("No se pudo comprobar el pago con Stripe.");
        }
    }

    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        if (!StringUtils.hasText(webhookSecret)) {
            throw new BusinessException("Stripe webhook no configurado.");
        }
        try {
            Webhook.constructEvent(payload, signatureHeader, webhookSecret);
            JsonNode root = objectMapper.readTree(payload);
            if (!"checkout.session.completed".equals(root.path("type").asText())) {
                return;
            }
            String sessionId = root.path("data").path("object").path("id").asText();
            payments.findById(sessionId).ifPresent(this::complete);
        } catch (Exception ex) {
            throw new BusinessException("Webhook de Stripe no valido.");
        }
    }

    private void complete(StripeBonusPayment payment) {
        if (payment.getStatus() == StripeBonusPaymentStatus.COMPLETED) {
            return;
        }

        CustomerUser user = payment.getUser();
        int currentBonuses = user.getBonuses() == null ? 0 : Math.max(0, user.getBonuses());
        user.setBonuses(currentBonuses + payment.getBonuses());
        user.setUpdatedAt(LocalDateTime.now());
        users.save(user);

        payment.setStatus(StripeBonusPaymentStatus.COMPLETED);
        payment.setCompletedAt(LocalDateTime.now());
        payments.save(payment);
    }

    private BonusPaymentStatusResponse toStatusResponse(StripeBonusPayment payment) {
        CustomerUser user = payment.getUser();
        return new BonusPaymentStatusResponse(
            payment.getSessionId(),
            payment.getStatus().name(),
            payment.getBonuses(),
            new CustomerUserDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getEmail(),
                user.getRole().name(),
                user.getBonuses(),
                user.isEmailVerified(),
                user.isActive(),
                user.getCreatedAt().toString(),
                user.getUpdatedAt().toString()
            )
        );
    }

    private void ensureStripeConfigured() {
        if (!StringUtils.hasText(stripeSecretKey)) {
            throw new BusinessException("Stripe no esta configurado.");
        }
    }
}
