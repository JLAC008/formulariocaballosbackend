package com.formulariocaballos.auth.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsNonSpanishPhoneNumber() {
        RegisterRequest request = new RegisterRequest("Ana", "Luna", "555123456", "ana@example.com", "Password1");

        assertThat(validator.validate(request)).anyMatch(error -> error.getPropertyPath().toString().equals("phone"));
    }

    @Test
    void acceptsSpanishMobilePhoneNumber() {
        RegisterRequest request = new RegisterRequest("Ana", "Luna", "+34612345678", "ana@example.com", "Password1");

        assertThat(validator.validate(request)).isEmpty();
    }
}
