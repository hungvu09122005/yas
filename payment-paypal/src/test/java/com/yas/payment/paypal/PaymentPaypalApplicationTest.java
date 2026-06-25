package com.yas.payment.paypal;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
class PaymentPaypalApplicationTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void contextLoads() {
        // It is supposed to be empty
    }

}
