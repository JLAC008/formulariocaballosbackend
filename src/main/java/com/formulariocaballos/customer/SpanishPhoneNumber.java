package com.formulariocaballos.customer;

import com.formulariocaballos.exception.BusinessException;

public final class SpanishPhoneNumber {
    private SpanishPhoneNumber() {}

    public static String normalize(String value) {
        if (value == null) {
            throw new BusinessException("El teléfono es obligatorio.");
        }
        String normalized = value.replaceAll("[\\s-]", "");
        if (normalized.startsWith("0034")) {
            normalized = "+34" + normalized.substring(4);
        } else if (!normalized.startsWith("+34")) {
            normalized = "+34" + normalized;
        }
        if (!normalized.matches("\\+34[6789]\\d{8}")) {
            throw new BusinessException("El teléfono debe ser un número español válido.");
        }
        return normalized;
    }
}
