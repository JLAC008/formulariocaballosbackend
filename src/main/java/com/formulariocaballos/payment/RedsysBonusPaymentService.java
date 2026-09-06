package com.formulariocaballos.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formulariocaballos.customer.CustomerUser;
import com.formulariocaballos.customer.CustomerUserRepository;
import com.formulariocaballos.exception.BusinessException;
import com.formulariocaballos.exception.ResourceNotFoundException;
import com.formulariocaballos.payment.dto.BonusCheckoutResponse;
import com.formulariocaballos.payment.dto.BonusPaymentStatusResponse;
import com.formulariocaballos.state.dto.CustomerUserDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class RedsysBonusPaymentService {
    private static final String SIGNATURE_VERSION = "HMAC_SHA256_V1";
    private static final String MERCHANT_NAME = "Martinez Luna";

    private final CustomerUserRepository users;
    private final RedsysBonusPaymentRepository payments;
    private final BonusPackRepository packs;
    private final ObjectMapper objectMapper;
    private final String merchantCode;
    private final String terminal;
    private final String secretKey;
    private final String currency;
    private final String transactionType;
    private final String paymentUrl;
    private final String notificationUrl;
    private final String frontendUrl;

    public RedsysBonusPaymentService(CustomerUserRepository users,
                                     RedsysBonusPaymentRepository payments,
                                     BonusPackRepository packs,
                                     ObjectMapper objectMapper,
                                     @Value("${app.redsys.merchant-code}") String merchantCode,
                                     @Value("${app.redsys.terminal}") String terminal,
                                     @Value("${app.redsys.secret-key}") String secretKey,
                                     @Value("${app.redsys.currency}") String currency,
                                     @Value("${app.redsys.transaction-type}") String transactionType,
                                     @Value("${app.redsys.payment-url}") String paymentUrl,
                                     @Value("${app.redsys.notification-url}") String notificationUrl,
                                     @Value("${app.mail.frontend-url}") String frontendUrl) {
        this.users = users;
        this.payments = payments;
        this.packs = packs;
        this.objectMapper = objectMapper;
        this.merchantCode = merchantCode;
        this.terminal = terminal;
        this.secretKey = secretKey;
        this.currency = currency;
        this.transactionType = transactionType;
        this.paymentUrl = paymentUrl;
        this.notificationUrl = notificationUrl;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public BonusCheckoutResponse createCheckout(String email, Long packId, Integer amount) {
        ensureRedsysConfigured();
        BonusPack pack = selectedPack(packId, amount);

        CustomerUser user = users.findByEmailIgnoreCase(email)
            .filter(existing -> existing.isActive())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        try {
            String orderId = createUniqueOrderId();
            Map<String, String> parameters = new LinkedHashMap<>();
            parameters.put("DS_MERCHANT_AMOUNT", String.valueOf(pack.getPriceCents()));
            parameters.put("DS_MERCHANT_ORDER", orderId);
            parameters.put("DS_MERCHANT_MERCHANTCODE", merchantCode);
            parameters.put("DS_MERCHANT_CURRENCY", currency);
            parameters.put("DS_MERCHANT_TRANSACTIONTYPE", transactionType);
            parameters.put("DS_MERCHANT_TERMINAL", terminal);
            parameters.put("DS_MERCHANT_MERCHANTURL", merchantNotificationUrl());
            parameters.put("DS_MERCHANT_URLOK", frontendUrl + "/?redsys_bonus=success&order_id=" + orderId);
            parameters.put("DS_MERCHANT_URLKO", frontendUrl + "/?redsys_bonus=cancel&order_id=" + orderId);
            parameters.put("DS_MERCHANT_MERCHANTNAME", MERCHANT_NAME);
            parameters.put("DS_MERCHANT_PRODUCTDESCRIPTION", "Compra de bono - " + pack.getName());
            parameters.put("DS_MERCHANT_TITULAR", user.getEmail());

            String merchantParameters = encodeMerchantParameters(parameters);
            String signature = createSignature(orderId, merchantParameters);

            RedsysBonusPayment payment = new RedsysBonusPayment();
            payment.setOrderId(orderId);
            payment.setUser(user);
            payment.setBonusPackId(pack.getId());
            payment.setBonuses(pack.getBonuses());
            payment.setAmountCents(pack.getPriceCents());
            payment.setCurrency(pack.getCurrency());
            payment.setStatus(RedsysBonusPaymentStatus.PENDING);
            payments.save(payment);

            return new BonusCheckoutResponse(orderId, paymentUrl, SIGNATURE_VERSION, merchantParameters, signature);
        } catch (JsonProcessingException | GeneralSecurityException ex) {
            throw new BusinessException("No se pudo iniciar el pago con Redsys.");
        }
    }

    @Transactional(readOnly = true)
    public BonusPaymentStatusResponse refreshStatus(String email, String orderId) {
        RedsysBonusPayment payment = payments.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (!payment.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new BusinessException("El pago no pertenece a este usuario.");
        }
        return toStatusResponse(payment);
    }

    @Transactional
    public void handleWebhook(Map<String, String> params) {
        String merchantParameters = params.get("Ds_MerchantParameters");
        String signature = params.get("Ds_Signature");
        if (!StringUtils.hasText(merchantParameters) || !StringUtils.hasText(signature)) {
            throw new BusinessException("Notificación Redsys no válida.");
        }

        try {
            JsonNode root = objectMapper.readTree(decodeBase64(merchantParameters));
            String orderId = root.path("Ds_Order").asText(root.path("DS_MERCHANT_ORDER").asText());
            String expectedSignature = normalizeSignature(createSignature(orderId, merchantParameters));
            if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), normalizeSignature(signature).getBytes(StandardCharsets.UTF_8))) {
                throw new BusinessException("Firma Redsys no válida.");
            }

            int responseCode = Integer.parseInt(root.path("Ds_Response").asText("9999"));
            if (responseCode > 99) {
                return;
            }
            payments.findById(orderId).ifPresent(this::complete);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("Notificación Redsys no válida.");
        }
    }

    private void complete(RedsysBonusPayment payment) {
        if (payment.getStatus() == RedsysBonusPaymentStatus.COMPLETED) {
            return;
        }

        CustomerUser user = payment.getUser();
        int currentBonuses = user.getBonuses() == null ? 0 : Math.max(0, user.getBonuses());
        user.setBonuses(currentBonuses + payment.getBonuses());
        user.setUpdatedAt(LocalDateTime.now());
        users.save(user);

        payment.setStatus(RedsysBonusPaymentStatus.COMPLETED);
        payment.setCompletedAt(LocalDateTime.now());
        payments.save(payment);
    }

    private BonusPaymentStatusResponse toStatusResponse(RedsysBonusPayment payment) {
        CustomerUser user = payment.getUser();
        return new BonusPaymentStatusResponse(
            payment.getOrderId(),
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

    private void ensureRedsysConfigured() {
        if (!StringUtils.hasText(merchantCode) || !StringUtils.hasText(secretKey)) {
            throw new BusinessException("Redsys no está configurado.");
        }
    }

    private BonusPack selectedPack(Long packId, Integer amount) {
        if (packId != null) {
            return packs.findById(packId)
                .filter(pack -> Boolean.TRUE.equals(pack.getActive()))
                .filter(pack -> !Boolean.TRUE.equals(pack.getDeleted()))
                .orElseThrow(() -> new BusinessException("Pack de sesiones no válido."));
        }

        if (amount != null) {
            return packs.findFirstByBonusesAndActiveTrueAndDeletedFalseOrderByPriceCentsAsc(amount)
                .orElseThrow(() -> new BusinessException("Pack de sesiones no válido."));
        }

        throw new BusinessException("Selecciona un pack de sesiones.");
    }

    private String createUniqueOrderId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        String prefix = timestamp.substring(0, 10);
        for (int attempts = 0; attempts < 100; attempts++) {
            String orderId = prefix + String.format("%02d", Math.floorMod(System.nanoTime() + attempts, 100));
            if (!payments.existsById(orderId)) {
                return orderId;
            }
        }
        throw new BusinessException("No se pudo generar el pedido de pago.");
    }

    private String merchantNotificationUrl() {
        if (StringUtils.hasText(notificationUrl)) {
            return notificationUrl;
        }
        return frontendUrl + "/api/payments/bonuses/webhook";
    }

    private String encodeMerchantParameters(Map<String, String> parameters) throws JsonProcessingException {
        byte[] json = objectMapper.writeValueAsBytes(parameters);
        return Base64.getEncoder().encodeToString(json);
    }

    private String createSignature(String orderId, String merchantParameters) throws GeneralSecurityException {
        SecretKeySpec key = new SecretKeySpec(encryptOrderKey(orderId), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        return Base64.getEncoder().encodeToString(mac.doFinal(merchantParameters.getBytes(StandardCharsets.UTF_8)));
    }

    private byte[] encryptOrderKey(String orderId) throws GeneralSecurityException {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        SecretKeySpec key = new SecretKeySpec(keyBytes, "DESede");
        Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(new byte[8]));
        return cipher.doFinal(padOrder(orderId));
    }

    private byte[] padOrder(String orderId) {
        byte[] orderBytes = orderId.getBytes(StandardCharsets.UTF_8);
        int paddedLength = ((orderBytes.length + 7) / 8) * 8;
        byte[] padded = new byte[paddedLength];
        System.arraycopy(orderBytes, 0, padded, 0, orderBytes.length);
        return padded;
    }

    private String normalizeSignature(String signature) {
        return signature.replace(' ', '+').replace('-', '+').replace('_', '/');
    }

    private byte[] decodeBase64(String value) {
        String normalized = normalizeSignature(value);
        int remainder = normalized.length() % 4;
        if (remainder > 0) {
            normalized += "=".repeat(4 - remainder);
        }
        return Base64.getDecoder().decode(normalized);
    }
}
