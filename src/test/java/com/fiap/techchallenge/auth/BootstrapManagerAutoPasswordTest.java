package com.fiap.techchallenge.auth;

import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.auth.entities.UserAuth;
import com.fiap.techchallenge.auth.enums.AuthProvider;
import com.fiap.techchallenge.auth.repositories.UserAuthRepository;
import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.user.api.representation.UserInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link BootstrapManagerRunner} auto-generates a password when
 * {@code app.auth.bootstrap-manager.password} is left unset — {@link BootstrapManagerRunnerTest}
 * always supplies one, so this covers the other branch with its own (differently-configured) context.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "app.auth.bootstrap-manager.email=bootstrap-manager-autopw@example.com",
        "app.auth.bootstrap-manager.first-name=Boot",
        "app.auth.bootstrap-manager.last-name=Strap",
        "app.auth.bootstrap-manager.document-type=CPF",
        "app.auth.bootstrap-manager.document-code=15350946056",
        "app.auth.bootstrap-manager.phone=11999999999"
})
class BootstrapManagerAutoPasswordTest {

    private static final String EMAIL = "bootstrap-manager-autopw@example.com";

    @Autowired
    UserService userService;

    @Autowired
    UserAuthRepository userAuths;

    @Test
    void generatesARandomPasswordWhenNoneIsConfigured() {
        UserInfo user = userService.findByEmail(EMAIL).orElseThrow();

        assertThat(userAuths.findByUserIdAndProvider(user.id(), AuthProvider.LOCAL))
                .get()
                .extracting(UserAuth::getPasswordHash)
                .isNotNull();
    }
}
