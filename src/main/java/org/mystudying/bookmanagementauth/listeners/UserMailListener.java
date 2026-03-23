package org.mystudying.bookmanagementauth.listeners;

import org.mystudying.bookmanagementauth.events.UserRegisteredEvent;
import org.mystudying.bookmanagementauth.exceptions.RetryableMailException;
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
public class UserMailListener extends AbstractMailListener<UserRegisteredEvent> {

    private static final Logger log = LoggerFactory.getLogger(UserMailListener.class);

    private final MailTemplateService mailTemplateService;


    @Value("${app.baseUrl:http://localhost:8080}")
    private String baseUrl;

    public UserMailListener(MailService mailService, MailTemplateService mailTemplateService, FailedMailService failedMailService) {
        super(mailService, failedMailService);
        this.mailTemplateService = mailTemplateService;

    }

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
            retryFor = RetryableMailException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void handle(UserRegisteredEvent event) {
        log.info("Attempting to send registration email to: {}", event.email());
        String subject = getSubject();
        String body = buildBody(event);

        sendMail(event.email(), subject, body);
        log.info("Registration email sent to: {}", event.email());
    }

    @Recover
    public void recover(Exception e, UserRegisteredEvent event) {
        log.error("Failed to send registration email to {} after multiple retries: {}", event.email(), e.getMessage());
        logFailure(
                event.email(),
                getSubject(),
                buildBody(event),
                e
        );
    }

    private String getSubject() {
        return "Welcome to Book Management - Please Verify Your Account";
    }

    private String buildBody(UserRegisteredEvent event) {
        String verificationLink = String.format("%s/verify?token=%s", baseUrl, event.token());
        return mailTemplateService.buildRegistrationMail(event.name(), verificationLink);
    }
}
