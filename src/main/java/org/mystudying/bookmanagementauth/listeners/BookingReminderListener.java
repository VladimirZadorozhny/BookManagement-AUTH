package org.mystudying.bookmanagementauth.listeners;

import org.mystudying.bookmanagementauth.events.BookingReminderEvent;
import org.mystudying.bookmanagementauth.exceptions.RetryableMailException;
import org.mystudying.bookmanagementauth.services.ReminderProcessingService;
import org.mystudying.bookmanagementauth.services.mail.FailedMailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BookingReminderListener {

    private static final Logger log = LoggerFactory.getLogger(BookingReminderListener.class);

    private final FailedMailService failedMailService;
    private final ReminderProcessingService reminderProcessingService;

    public BookingReminderListener(FailedMailService failedMailService, ReminderProcessingService reminderProcessingService) {
        this.failedMailService = failedMailService;
        this.reminderProcessingService = reminderProcessingService;
    }

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
            retryFor = RetryableMailException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void handleBookingReminder(BookingReminderEvent event) {
        reminderProcessingService.processReminder(event);
    }

    @Recover
    public void recover(Exception e, BookingReminderEvent event) {
        log.error("Failed to send booking reminder email of type {} to {} after multiple retries: {}",
                event.reminderType(), event.email(), e.getMessage());
        failedMailService.logFailedMail(
                event.email(),
                reminderProcessingService.getSubject(event),
                reminderProcessingService.buildBody(event),
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
