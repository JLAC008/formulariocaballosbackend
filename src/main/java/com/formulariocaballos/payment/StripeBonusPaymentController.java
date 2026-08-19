package com.formulariocaballos.payment;

import com.formulariocaballos.payment.dto.BonusCheckoutResponse;
import com.formulariocaballos.payment.dto.BonusPaymentStatusResponse;
import com.formulariocaballos.payment.dto.CreateBonusCheckoutRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/bonuses")
public class StripeBonusPaymentController {
    private final StripeBonusPaymentService service;

    public StripeBonusPaymentController(StripeBonusPaymentService service) {
        this.service = service;
    }

    @PostMapping("/checkout")
    public ResponseEntity<BonusCheckoutResponse> checkout(Authentication authentication,
                                                          @Valid @RequestBody CreateBonusCheckoutRequest request) {
        return ResponseEntity.ok(service.createCheckout(authentication.getName(), request.amount()));
    }

    @GetMapping("/status")
    public ResponseEntity<BonusPaymentStatusResponse> status(Authentication authentication,
                                                            @RequestParam String sessionId) {
        return ResponseEntity.ok(service.refreshStatus(authentication.getName(), sessionId));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
                                        @RequestHeader("Stripe-Signature") String signatureHeader) {
        service.handleWebhook(payload, signatureHeader);
        return ResponseEntity.noContent().build();
    }
}
