package com.formulariocaballos.payment.dto;

import com.formulariocaballos.payment.BonusPack;

public record BonusPackResponse(
    Long id,
    String name,
    Integer bonuses,
    Long priceCents,
    String currency,
    Boolean active
) {
    public static BonusPackResponse from(BonusPack pack) {
        return new BonusPackResponse(
            pack.getId(),
            pack.getName(),
            pack.getBonuses(),
            pack.getPriceCents(),
            pack.getCurrency(),
            pack.getActive()
        );
    }
}
