package org.mystudying.bookmanagementauth.listeners;

import jakarta.mail.MessagingException;
import org.mystudying.bookmanagementauth.events.PasswordResetRequestedEvent;
import org.mystudying.bookmanagementauth.services.mail.FailedMailService;
import org.mystudying.bookmanagementauth.services.mail.MailService;
import org.mystudying.bookmanagementauth.services.mail.MailTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PasswordResetMailListener {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailListener.class);
    private final MailService mailService;
    private final MailTemplateService mailTemplateService;
    private final FailedMailService failedMailService;

    @Value("${app.baseUrl:http://localhost:8080}")
    private String baseUrl;

    public PasswordResetMailListener(MailService mailService, MailTemplateService mailTemplateService, FailedMailService failedMailService) {
        this.mailService = mailService;
        this.mailTemplateService = mailTemplateService;
        this.failedMailService = failedMailService;
    }

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
            retryFor = MessagingException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void handlePasswordResetRequest(PasswordResetRequestedEvent event) throws MessagingException {
        log.info("Attempting to send password reset email to: {}", event.email());
        String subject = getSubject();
        String body = buildBody(event);

        mailService.send(event.email(), subject, body);
        log.info("Password reset email sent to: {}", event.email());
    }

    @Recover
    public void recover(MessagingException e, PasswordResetRequestedEvent event) {
        log.error("Failed to send password reset email to {} after multiple retries: {}", event.email(), e.getMessage());
        failedMailService.logFailedMail(
                event.email(),
                getSubject(),
                buildBody(event),
                e.getMessage()
        );
    }

    private String getSubject() {
        return "Password Reset Request for Book Management";
    }

    private String buildBody(PasswordResetRequestedEvent event) {
        String resetLink = String.format("%s/reset-password?token=%s", baseUrl, event.token());
        return mailTemplateService.buildPasswordResetMail(event.email(), resetLink);
    }
}
