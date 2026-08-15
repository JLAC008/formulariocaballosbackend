package com.formulariocaballos.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHashingTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void bcryptHashMatchesPasswordWithoutStoringPlainText() {
        String password = "Password1";
        String hash = encoder.encode(password);

        assertThat(hash).startsWith("$2");
        assertThat(hash).isNotEqualTo(password);
        assertThat(encoder.matches(password, hash)).isTrue();
        assertThat(encoder.matches("wrong-password", hash)).isFalse();
    }
}
