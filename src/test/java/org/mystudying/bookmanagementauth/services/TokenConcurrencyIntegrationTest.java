package org.mystudying.bookmanagementauth.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mystudying.bookmanagementauth.dto.RegisterRequestDto;
import org.mystudying.bookmanagementauth.dto.UserDto;
import org.mystudying.bookmanagementauth.exceptions.TokenAlreadyUsedException;
import org.mystudying.bookmanagementauth.support.AbstractSecurityIntegrationTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenConcurrencyIntegrationTest extends AbstractSecurityIntegrationTest {

    private static final String TEST_EMAIL = "conc.token@example.com";

    @AfterEach
    void cleanup() {
        testDataCleanup.deleteUserCascade(TEST_EMAIL);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void onlyOneRequestShouldSucceedWhenConsumingTokenConcurrently() throws Exception {
        // GIVEN: A registered user with a token
        RegisterRequestDto request = new RegisterRequestDto("Conc User", TEST_EMAIL, "password123");
        UserDto user = authLifecycleService.register(request);

        String token = jdbcClient.sql("SELECT token FROM verification_token WHERE user_id = ?")
                .param(user.id()).query(String.class).single();

        // WHEN: Consuming the same token twice concurrently
        Callable<Boolean> task = () -> {
            try {
                verificationService.verifyToken(token);
                return true;
            } catch (TokenAlreadyUsedException e) {
                return false;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        List<Boolean> results = concurrentTestHelper.runParallel(task, 2);

        // THEN: One should be true (success), one should be false (TokenAlreadyUsedException)
        long successCount = results.stream().filter(r -> r).count();
        long failureCount = results.stream().filter(r -> !r).count();

        assertThat(successCount).isEqualTo(1);
        assertThat(failureCount).isEqualTo(1);
    }
}

