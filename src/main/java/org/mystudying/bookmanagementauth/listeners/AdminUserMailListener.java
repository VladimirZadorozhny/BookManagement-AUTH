package org.mystudying.bookmanagementauth.listeners;

import org.mystudying.bookmanagementauth.events.AdminUserMailRequestedEvent;
import org.mystudying.bookmanagementauth.exceptions.RetryableMailException;
import org.mystudying.bookmanagementauth.services.mail.FailedMailService;
import org.mystudying.bookmanagementauth.services.mail.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AdminUserMailListener extends AbstractMailListener<AdminUserMailRequestedEvent> {

    private static final Logger log = LoggerFactory.getLogger(AdminUserMailListener.class);

    public AdminUserMailListener(MailService mailService, FailedMailService failedMailService) {
        super(mailService, failedMailService);
    }

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
            retryFor = RetryableMailException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void handle(AdminUserMailRequestedEvent event) {
        log.info("Sending admin mail to user {} with subject: {}", event.email(), event.subject());
        sendMail(event.email(), event.subject(), event.body());
        log.info("Admin mail sent to user {}", event.email());
    }

    @Recover
    public void recover(Exception e, AdminUserMailRequestedEvent event) {
        log.error("Failed to send admin mail to {} after multiple retries: {}", event.email(), e.getMessage());
        logFailure(
                event.email(),
                event.subject(),
                event.body(),
                e
        );
    }
}
