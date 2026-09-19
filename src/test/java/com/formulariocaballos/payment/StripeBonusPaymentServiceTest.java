package com.formulariocaballos.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formulariocaballos.customer.CustomerUserRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class StripeBonusPaymentServiceTest {

    @Test
    void disabledGatewayRejectsNewCheckoutsBeforeCallingStripe() {
        StripeBonusPaymentService service = new StripeBonusPaymentService(
            mock(CustomerUserRepository.class),
            mock(StripeBonusPaymentRepository.class),
            mock(BonusPackRepository.class),
            new ObjectMapper(),
            "",
            "",
            "http://localhost:4200",
            false
        );

        assertThatThrownBy(() -> service.createCheckout("customer@example.com", 1L, null))
            .hasMessage("La pasarela de pago está desactivada.");
    }
}
