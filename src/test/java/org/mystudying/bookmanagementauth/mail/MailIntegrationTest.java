package org.mystudying.bookmanagementauth.mail;

import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mystudying.bookmanagementauth.dto.RegisterRequestDto;
import org.mystudying.bookmanagementauth.dto.UserDto;
import org.mystudying.bookmanagementauth.services.AdminMailService;
import org.mystudying.bookmanagementauth.support.AbstractSecurityIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for various mail templates and event wiring.
 * Note: These tests use Propagation.NOT_SUPPORTED to allow transactions to commit
 * and trigger @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT).
 */
public class MailIntegrationTest extends AbstractSecurityIntegrationTest {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private AdminMailService adminMailService;

    @Value("${app.baseUrl:http://localhost:8080}")
    private String baseUrl;

    private GreenMail greenMail;

    @BeforeEach
    void setup() throws FolderException {
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();
        greenMail.purgeEmailFromAllMailboxes();
    }

    @AfterEach
    void tearDown() {
        greenMail.stop();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldSendVerificationEmailOnRegistration() throws Exception {
        String email = "mail.test" + UUID.randomUUID() + "@example.com";
        RegisterRequestDto request = new RegisterRequestDto("Mail User", email, "password123");

        // Transaction must commit to trigger listener
        transactionTemplate.execute(status -> {
            authLifecycleService.register(request);
            return null;
        });

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            MimeMessage message = mailTestUtils.verifyEmailReceived(greenMail, email, "Welcome to Book Management - Please Verify Your Account");
            String body = mailTestUtils.getTextFromMessage(message);
            assertTrue(body.contains(baseUrl + "/verify?token="));

        });

        testDataCleanup.deleteUserCascade(email);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldSendPasswordResetEmail() throws Exception {
        // GIVEN: A verified user exists
        String email = "mail.test" + UUID.randomUUID() + "@example.com";
        transactionTemplate.execute(status -> {
            signupAndVerify("Mail User", email, "password123");
            return null;
        });

        // WHEN: Requesting password reset
        transactionTemplate.execute(status -> {
            authLifecycleService.requestPasswordReset(email);
            return null;
        });

        // THEN: Email received
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            MimeMessage message = mailTestUtils.verifyEmailReceived(greenMail, email, "Password Reset Request for Book Management");
            String body = mailTestUtils.getTextFromMessage(message);
            assertTrue(body.contains(baseUrl + "/reset-password?token="));

        });
        testDataCleanup.deleteUserCascade(email);

    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldSendLoginAlertEmail() throws Exception {
        // GIVEN: A verified user
        String email = "mail.test" + UUID.randomUUID() + "@example.com";
        transactionTemplate.execute(status -> {
            signupAndVerify("Mail User", email, "password123");
            return null;
        });

        // WHEN: Logging in (Success handler triggers the event)
        loginAs(email, "password123");

        // THEN: Login alert email received
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            mailTestUtils.verifyEmailReceived(greenMail, email, "Security Alert: New Login to Your Account");

        });
        testDataCleanup.deleteUserCascade(email);

    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldSendAdminCustomEmail() throws Exception {
        // GIVEN: A target user
        String email = "mail.test" + UUID.randomUUID() + "@example.com";
        UserDto user = transactionTemplate.execute(status -> signupAndVerify("Mail User", email, "password123"));

        // WHEN: Admin sends a custom mail
        transactionTemplate.execute(status -> {
            adminMailService.sendMailToUser(user.id(), "Admin Subject", "Admin Message Body");
            return null;
        });

        // THEN: Email received
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            MimeMessage message = mailTestUtils.verifyEmailReceived(greenMail, email, "Admin Subject");
            String body = mailTestUtils.getTextFromMessage(message);
            assertTrue(body.contains("Admin Message Body"));

        });
        testDataCleanup.deleteUserCascade(email);

    }
}
