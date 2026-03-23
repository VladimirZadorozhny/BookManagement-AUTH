package org.mystudying.bookmanagementauth.listeners;

import org.mystudying.bookmanagementauth.events.LoginSuccessEvent;
import org.mystudying.bookmanagementauth.exceptions.RetryableMailException;
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
public class LoginAlertMailListener extends AbstractMailListener<LoginSuccessEvent> {

    private static final Logger log = LoggerFactory.getLogger(LoginAlertMailListener.class);

    private final MailTemplateService mailTemplateService;


    public LoginAlertMailListener(MailService mailService, MailTemplateService mailTemplateService, FailedMailService failedMailService) {
        super(mailService, failedMailService);
        this.mailTemplateService = mailTemplateService;

    }

    @Async("mailExecutor")
    @EventListener
    @Retryable(
            retryFor = RetryableMailException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void handle(LoginSuccessEvent event) {
        log.info("Sending login alert email to: {}", event.email());
        String subject = getSubject();
        String body = buildBody(event);

        sendMail(event.email(), subject, body);
        log.info("Login alert email sent to: {}", event.email());
    }

    @Recover
    public void recover(Exception e, LoginSuccessEvent event) {
        log.error("Failed to send login alert email to {} after multiple retries: {}", event.email(), e.getMessage());
        logFailure(
                event.email(),
                getSubject(),
                buildBody(event),
                e
        );
    }

    private String getSubject() {
        return "Security Alert: New Login to Your Account";
    }

    private String buildBody(LoginSuccessEvent event) {
        return mailTemplateService.buildLoginAlertMail(event.name(), event.loginTime());
    }
}
