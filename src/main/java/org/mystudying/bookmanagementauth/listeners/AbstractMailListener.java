package org.mystudying.bookmanagementauth.listeners;

import org.mystudying.bookmanagementauth.services.mail.FailedMailService;
import org.mystudying.bookmanagementauth.services.mail.MailService;

public abstract class AbstractMailListener<T> {
    protected final MailService mailService;
    protected final FailedMailService failedMailService;

    protected AbstractMailListener(MailService mailService,
                                   FailedMailService failedMailService) {
        this.mailService = mailService;
        this.failedMailService = failedMailService;
    }

    protected void sendMail(String to, String subject, String body) {
        mailService.send(to, subject, body);
    }

    protected void logFailure(String to, String subject, String body, Exception e) {
        failedMailService.logFailedMail(
                to,
                subject,
                body,
                extractErrorMessage(e)
        );
    }

    private String extractErrorMessage(Exception e) {
        StringBuilder message = new StringBuilder();

        message.append(e.getClass().getSimpleName())
                .append(": ")
                .append(e.getMessage());

        Throwable cause = e.getCause();
        while (cause != null) {
            message.append(" | Caused by: ")
                    .append(cause.getClass().getSimpleName())
                    .append(": ")
                    .append(cause.getMessage());
            cause = cause.getCause();
        }

        return message.toString();

    }
}
