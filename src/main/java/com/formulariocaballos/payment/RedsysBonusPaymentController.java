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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/bonuses")
public class RedsysBonusPaymentController {
    private final RedsysBonusPaymentService service;

    public RedsysBonusPaymentController(RedsysBonusPaymentService service) {
        this.service = service;
    }

    @PostMapping("/checkout")
    public ResponseEntity<BonusCheckoutResponse> checkout(Authentication authentication,
                                                          @Valid @RequestBody CreateBonusCheckoutRequest request) {
        return ResponseEntity.ok(service.createCheckout(authentication.getName(), request.packId(), request.amount()));
    }

    @GetMapping("/status")
    public ResponseEntity<BonusPaymentStatusResponse> status(Authentication authentication,
                                                            @RequestParam String orderId) {
        return ResponseEntity.ok(service.refreshStatus(authentication.getName(), orderId));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestParam Map<String, String> params) {
        service.handleWebhook(params);
        return ResponseEntity.noContent().build();
    }
}
