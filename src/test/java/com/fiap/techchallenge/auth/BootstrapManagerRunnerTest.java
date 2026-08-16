package com.fiap.techchallenge.auth;

import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.auth.services.BootstrapManagerRunner;
import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.user.api.representation.UserInfo;
import com.fiap.techchallenge.user.enums.WorkerRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code /auth/register/worker} requires a MANAGER token, so on a fresh database none can ever be
 * minted through the API. {@link BootstrapManagerRunner} is the escape hatch: it runs on startup
 * and creates exactly one MANAGER when {@code app.auth.bootstrap-manager.email} is configured.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "app.auth.bootstrap-manager.email=bootstrap-manager@example.com",
        "app.auth.bootstrap-manager.first-name=Boot",
        "app.auth.bootstrap-manager.last-name=Strap",
        "app.auth.bootstrap-manager.document-type=CPF",
        "app.auth.bootstrap-manager.document-code=00000000000",
        "app.auth.bootstrap-manager.phone=11999999999"
})
class BootstrapManagerRunnerTest {

    private static final String EMAIL = "bootstrap-manager@example.com";

    @Autowired
    UserService userService;

    @Autowired
    BootstrapManagerRunner runner;

    @Test
    void createsExactlyOneManagerAndReRunningIsANoOp() {
        UserInfo user = userService.findByEmail(EMAIL).orElseThrow();

        assertThat(user.worker()).isTrue();
        assertThat(user.workerRole()).isEqualTo(WorkerRole.MANAGER);

        // The runner already ran once during context startup; running it again must not attempt to
        // recreate the user (which would blow up on the unique email/document indexes).
        runner.run(null);

        UserInfo stillTheSameUser = userService.findByEmail(EMAIL).orElseThrow();
        assertThat(stillTheSameUser.id()).isEqualTo(user.id());
    }
}
