package org.mystudying.bookmanagementauth.listeners;

import jakarta.mail.MessagingException;
import org.mystudying.bookmanagementauth.events.BookingReminderEvent;
import org.mystudying.bookmanagementauth.services.ReminderProcessingService;
import org.mystudying.bookmanagementauth.services.mail.FailedMailService;
import org.mystudying.bookmanagementauth.services.mail.MailTemplateService;
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
    private final MailTemplateService mailTemplateService;
    private final FailedMailService failedMailService;
    private final ReminderProcessingService reminderProcessingService;

    public BookingReminderListener(MailTemplateService mailTemplateService, FailedMailService failedMailService, ReminderProcessingService reminderProcessingService) {
        this.mailTemplateService = mailTemplateService;
        this.failedMailService = failedMailService;
        this.reminderProcessingService = reminderProcessingService;
    }

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
            retryFor = MessagingException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void handleBookingReminder(BookingReminderEvent event) throws MessagingException {
        reminderProcessingService.processReminder(event);
    }

    @Recover
    public void recover(MessagingException e, BookingReminderEvent event) {
        log.error("Failed to send booking reminder email of type {} to {} after multiple retries: {}",
                event.reminderType(), event.email(), e.getMessage());
        failedMailService.logFailedMail(
                event.email(),
                getSubject(event),
                buildBody(event),
                e.getMessage()
        );
    }

    private String getSubject(BookingReminderEvent event) {
        switch (event.reminderType()) {
            case THREE_DAYS_LEFT:
                return "Reminder: Your book '" + event.bookTitle() + "' is due soon!";
            case DUE_TODAY:
                return "Reminder: Your book '" + event.bookTitle() + "' is due today!";
            case OVERDUE:
                return "Action Required: Your book '" + event.bookTitle() + "' is overdue!";
            default:
                return "Library Notification";
        }
    }

    private String buildBody(BookingReminderEvent event) {
        return mailTemplateService.buildReminderMail(event.userName(), event.bookTitle(), event.dueDate(), event.reminderType());
    }
}
