package org.mystudying.bookmanagementauth.mail;

import org.junit.jupiter.api.Test;
import org.mystudying.bookmanagementauth.domain.FailedMailLog;
import org.mystudying.bookmanagementauth.dto.auth.RegisterRequestDto;
import org.mystudying.bookmanagementauth.exceptions.RetryableMailException;
import org.mystudying.bookmanagementauth.repositories.FailedMailLogRepository;
import org.mystudying.bookmanagementauth.services.mail.MailService;
import org.mystudying.bookmanagementauth.support.AbstractSecurityIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class MailRetryIntegrationTest extends AbstractSecurityIntegrationTest {

    @MockBean
    private MailService mailService;

    @Autowired
    private FailedMailLogRepository failedMailLogRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldLogToFailedMailLogAfterExhaustingRetries() {
        // GIVEN: mailService always throws exception
        String email = "retry.test" + UUID.randomUUID() + "@example.com";
        doThrow(new RetryableMailException("SMTP Connection Refused", new Exception()))
                .when(mailService).send(anyString(), anyString(), anyString());

        RegisterRequestDto request = new RegisterRequestDto("Retry User", email, "password123");

        // WHEN: Registering a user (Transaction must commit to trigger listener)
        transactionTemplate.execute(status -> {
            authLifecycleService.register(request);
            return null;
        });

        // THEN: Wait for retries and recovery logic to finish (async)
        await().atMost(15, TimeUnit.SECONDS).until(() -> {
            List<FailedMailLog> failures = failedMailLogRepository.findAll();
            return failures.stream().anyMatch(f -> f.getToEmail().equals(email));
        });

        // Verify retry happened (at least 3 attempts as per config)
        verify(mailService, atLeast(3)).send(eq(email), anyString(), anyString());

        // AND: Verify entry in Dead-Letter Queue (FailedMailLog)
        FailedMailLog failure = failedMailLogRepository.findAll().stream()
                .filter(f -> f.getToEmail().equals(email))
                .findFirst()
                .orElseThrow();

        assertTrue(failure.getErrorMessage().contains("SMTP Connection Refused"),
                "Expected error message to contain 'SMTP Connection Refused'");


        // Cleanup
        testDataCleanup.deleteFailedMailsByRecipient(email);
        testDataCleanup.deleteUserCascade(email);
    }
}
