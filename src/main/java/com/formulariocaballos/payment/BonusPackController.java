package com.formulariocaballos.payment;

import com.formulariocaballos.payment.dto.BonusPackRequest;
import com.formulariocaballos.payment.dto.BonusPackResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BonusPackController {
    private final BonusPackService service;

    public BonusPackController(BonusPackService service) {
        this.service = service;
    }

    @GetMapping("/api/bonus-packs")
    public List<BonusPackResponse> active() {
        return service.active();
    }

    @GetMapping("/api/admin/bonus-packs")
    public List<BonusPackResponse> all() {
        return service.all();
    }

    @PostMapping("/api/admin/bonus-packs")
    public ResponseEntity<BonusPackResponse> create(@Valid @RequestBody BonusPackRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PutMapping("/api/admin/bonus-packs/{id}")
    public ResponseEntity<BonusPackResponse> update(@PathVariable Long id, @Valid @RequestBody BonusPackRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/api/admin/bonus-packs/{id}/toggle")
    public ResponseEntity<BonusPackResponse> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(service.toggle(id));
    }

    @DeleteMapping("/api/admin/bonus-packs/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
