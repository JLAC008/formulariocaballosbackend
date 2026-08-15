package com.formulariocaballos.customer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpanishPhoneNumberTest {
    @Test
    void normalizesSpanishMobile() {
        assertEquals("+34633443322", SpanishPhoneNumber.normalize("633 443 322"));
        assertEquals("+34633443322", SpanishPhoneNumber.normalize("0034 633-443-322"));
    }

    @Test
    void rejectsInternationalAndInvalidNumbers() {
        assertThrows(RuntimeException.class, () -> SpanishPhoneNumber.normalize("+351 912345678"));
        assertThrows(RuntimeException.class, () -> SpanishPhoneNumber.normalize("123456"));
    }
}
