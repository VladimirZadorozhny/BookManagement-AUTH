package org.mystudying.bookmanagementauth.listeners;

import jakarta.mail.MessagingException;
import org.mystudying.bookmanagementauth.events.AdminUserMailRequestedEvent;
import org.mystudying.bookmanagementauth.services.mail.FailedMailService;
import org.mystudying.bookmanagementauth.services.mail.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;

@Component
public class AdminUserMailListener {

    private static final Logger log = LoggerFactory.getLogger(AdminUserMailListener.class);
    private final MailService mailService;
    private final FailedMailService failedMailService;

    public AdminUserMailListener(MailService mailService, FailedMailService failedMailService) {
        this.mailService = mailService;
        this.failedMailService = failedMailService;
    }

    @Async("mailExecutor")
    @EventListener
    @Retryable(
            retryFor = MessagingException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void handleAdminUserMail(AdminUserMailRequestedEvent event) throws MessagingException {
        log.info("Sending admin mail to user {} with subject: {}", event.email(), event.subject());
        mailService.send(event.email(), event.subject(), event.body());
        log.info("Admin mail sent to user {}", event.email());
    }

    @Recover
    public void recover(MessagingException e, AdminUserMailRequestedEvent event) {
        log.error("Failed to send admin mail to {} after multiple retries: {}", event.email(), e.getMessage());
        failedMailService.logFailedMail(
                event.email(),
                event.subject(),
                event.body(),
                e.getMessage()
        );
    }
}
