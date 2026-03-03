package org.mystudying.bookmanagementauth.listeners;

import jakarta.mail.MessagingException;
import org.mystudying.bookmanagementauth.events.UserRegisteredEvent;
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
public class UserMailListener {

    private static final Logger log = LoggerFactory.getLogger(UserMailListener.class);
    private final MailService mailService;
    private final MailTemplateService mailTemplateService;

    @Value("${app.baseUrl:http://localhost:8080}")
    private String baseUrl;

    public UserMailListener(MailService mailService, MailTemplateService mailTemplateService) {
        this.mailService = mailService;
        this.mailTemplateService = mailTemplateService;
    }

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
            retryFor = MessagingException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void handleUserRegistered(UserRegisteredEvent event) throws MessagingException {
        log.info("Attempting to send registration email to: {}", event.email());
        String subject = "Welcome to Book Management - Please Verify Your Account";

        String verificationLink = String.format("%s/verify?token=%s", baseUrl, event.token());

        // Use MailTemplateService to build the body
        String body = mailTemplateService.buildRegistrationMail(event.name(), verificationLink);

        mailService.send(event.email(), subject, body);
        log.info("Registration email sent to: {}", event.email());
    }

    @Recover
    public void recover(MessagingException e, UserRegisteredEvent event) {
        log.error("Failed to send registration email to {} after multiple retries: {}", event.email(), e.getMessage());
        // TODO: Potentially notify admin or store in a dead-letter queue
    }
}
