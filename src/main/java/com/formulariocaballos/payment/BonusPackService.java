package com.formulariocaballos.payment;

import com.formulariocaballos.exception.BusinessException;
import com.formulariocaballos.exception.ResourceNotFoundException;
import com.formulariocaballos.payment.dto.BonusPackRequest;
import com.formulariocaballos.payment.dto.BonusPackResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BonusPackService {
    private final BonusPackRepository packs;
    private final StripeBonusPaymentRepository payments;

    public BonusPackService(BonusPackRepository packs, StripeBonusPaymentRepository payments) {
        this.packs = packs;
        this.payments = payments;
    }

    @Transactional(readOnly = true)
    public List<BonusPackResponse> active() {
        return packs.findByActiveTrueOrderByBonusesAscPriceCentsAsc().stream()
            .map(BonusPackResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<BonusPackResponse> all() {
        return packs.findAllByOrderByActiveDescBonusesAscPriceCentsAsc().stream()
            .map(BonusPackResponse::from)
            .toList();
    }

    @Transactional
    public BonusPackResponse create(BonusPackRequest request) {
        BonusPack pack = new BonusPack();
        pack.setId(System.currentTimeMillis());
        apply(pack, request, null);
        pack.setCreatedAt(LocalDateTime.now());
        return BonusPackResponse.from(packs.save(pack));
    }

    @Transactional
    public BonusPackResponse update(Long id, BonusPackRequest request) {
        BonusPack pack = pack(id);
        apply(pack, request, id);
        return BonusPackResponse.from(packs.save(pack));
    }

    @Transactional
    public BonusPackResponse toggle(Long id) {
        BonusPack pack = pack(id);
        pack.setActive(!Boolean.TRUE.equals(pack.getActive()));
        pack.setUpdatedAt(LocalDateTime.now());
        return BonusPackResponse.from(packs.save(pack));
    }

    @Transactional
    public void delete(Long id) {
        BonusPack pack = pack(id);
        if (payments.existsByBonusPackId(id)) {
            pack.setActive(false);
            pack.setUpdatedAt(LocalDateTime.now());
            packs.save(pack);
            return;
        }

        packs.delete(pack);
    }

    private BonusPack pack(Long id) {
        return packs.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pack de sesiones no encontrado."));
    }

    private void apply(BonusPack pack, BonusPackRequest request, Long currentId) {
        if (request.bonuses() == null || request.bonuses() < 1) {
            throw new BusinessException("El pack debe tener al menos una sesión.");
        }
        if (request.priceCents() == null || request.priceCents() < 100) {
            throw new BusinessException("El precio debe ser al menos 1 EUR.");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new BusinessException("Introduce un nombre para el pack.");
        }

        String name = request.name().trim();
        boolean duplicateName = currentId == null
            ? packs.existsByNameIgnoreCase(name)
            : packs.existsByNameIgnoreCaseAndIdNot(name, currentId);
        if (duplicateName) {
            throw new BusinessException("Ya existe un pack con ese nombre.");
        }

        String currency = request.currency() == null || request.currency().isBlank()
            ? "eur"
            : request.currency().trim().toLowerCase();
        pack.setName(name);
        pack.setBonuses(request.bonuses());
        pack.setPriceCents(request.priceCents());
        pack.setCurrency(currency);
        pack.setActive(request.active() == null || request.active());
        pack.setUpdatedAt(LocalDateTime.now());
    }
}
