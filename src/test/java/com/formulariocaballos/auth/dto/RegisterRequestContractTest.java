package com.formulariocaballos.auth.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestContractTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsSpanishPhoneAndStrongPassword() {
        var request = new RegisterRequest("Ana", "García López", "+34633443322", "ana@example.com", "Segura123");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsInternationalPhoneAndWeakPassword() {
        var request = new RegisterRequest("Ana", "García", "+351912345678", "ana@example.com", "password");
        assertThat(validator.validate(request)).isNotEmpty();
    }
}
