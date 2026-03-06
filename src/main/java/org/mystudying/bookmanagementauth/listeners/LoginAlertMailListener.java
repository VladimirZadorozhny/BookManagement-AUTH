package org.mystudying.bookmanagementauth.listeners;

import jakarta.mail.MessagingException;
import org.mystudying.bookmanagementauth.events.LoginSuccessEvent;
import org.mystudying.bookmanagementauth.services.mail.FailedMailService;
import org.mystudying.bookmanagementauth.services.mail.MailService;
import org.mystudying.bookmanagementauth.services.mail.MailTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;

@Component
public class LoginAlertMailListener {

    private static final Logger log = LoggerFactory.getLogger(LoginAlertMailListener.class);
    private final MailService mailService;
    private final MailTemplateService mailTemplateService;
    private final FailedMailService failedMailService;

    public LoginAlertMailListener(MailService mailService, MailTemplateService mailTemplateService, FailedMailService failedMailService) {
        this.mailService = mailService;
        this.mailTemplateService = mailTemplateService;
        this.failedMailService = failedMailService;
    }

    @Async("mailExecutor")
    @EventListener
    @Retryable(
            retryFor = MessagingException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void handleLoginAlert(LoginSuccessEvent event) throws MessagingException {
        log.info("Sending login alert email to: {}", event.email());
        String subject = getSubject();
        String body = buildBody(event);
        
        mailService.send(event.email(), subject, body);
        log.info("Login alert email sent to: {}", event.email());
    }

    @Recover
    public void recover(MessagingException e, LoginSuccessEvent event) {
        log.error("Failed to send login alert email to {} after multiple retries: {}", event.email(), e.getMessage());
        failedMailService.logFailedMail(
                event.email(),
                getSubject(),
                buildBody(event),
                e.getMessage()
        );
    }

    private String getSubject() {
        return "Security Alert: New Login to Your Account";
    }

    private String buildBody(LoginSuccessEvent event) {
        return mailTemplateService.buildLoginAlertMail(event.name(), event.loginTime());
    }
}
