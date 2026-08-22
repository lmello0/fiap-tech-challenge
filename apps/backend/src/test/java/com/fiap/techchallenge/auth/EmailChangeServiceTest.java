package com.fiap.techchallenge.auth;

import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.auth.exceptions.InvalidTokenException;
import com.fiap.techchallenge.auth.services.EmailChangeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EmailChangeServiceTest {

    @Autowired
    EmailChangeService service;

    @Test
    void confirmingWithAnUnknownTokenIsRejected() {
        assertThatThrownBy(() -> service.confirmEmailChange("not-a-real-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid or expired email change token");
    }
}
