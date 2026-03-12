package org.mystudying.bookmanagementauth.listeners;

import jakarta.mail.MessagingException;
import org.mystudying.bookmanagementauth.domain.BookingReminderLog;
import org.mystudying.bookmanagementauth.events.BookingReminderEvent;
import org.mystudying.bookmanagementauth.repositories.BookingReminderLogRepository;
import org.mystudying.bookmanagementauth.repositories.UserRepository;
import org.mystudying.bookmanagementauth.services.mail.FailedMailService;
import org.mystudying.bookmanagementauth.services.mail.MailService;
import org.mystudying.bookmanagementauth.services.mail.MailTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.OffsetDateTime;

@Component
public class BookingReminderListener {

    private static final Logger log = LoggerFactory.getLogger(BookingReminderListener.class);
    private final MailService mailService;
    private final MailTemplateService mailTemplateService;
    private final BookingReminderLogRepository bookingReminderLogRepository;
    private final UserRepository userRepository;
    private final FailedMailService failedMailService;

    public BookingReminderListener(MailService mailService, MailTemplateService mailTemplateService, BookingReminderLogRepository bookingReminderLogRepository, UserRepository userRepository, FailedMailService failedMailService) {
        this.mailService = mailService;
        this.mailTemplateService = mailTemplateService;
        this.bookingReminderLogRepository = bookingReminderLogRepository;
        this.userRepository = userRepository;
        this.failedMailService = failedMailService;
    }

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
            retryFor = MessagingException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void handleBookingReminder(BookingReminderEvent event) throws MessagingException {
        // 1. Check if already sent (Idempotency check)
        boolean alreadySent = bookingReminderLogRepository.existsByBookingIdAndReminderType(event.bookingId(), event.reminderType());
        if (alreadySent) {
            log.info("Reminder already logged for booking {} of type {}. Skipping.", event.bookingId(), event.reminderType());
            return;
        }

        // 2. Attempt to send the email
        log.info("Attempting to send booking reminder email of type {} to: {}", event.reminderType(), event.email());
        String subject = getSubject(event);
        String body = buildBody(event);

        mailService.send(event.email(), subject, body);
        log.info("Booking reminder email of type {} sent to: {}", event.reminderType(), event.email());

        // 3. Log successful send to prevent future duplicates
        try {
            bookingReminderLogRepository.save(new BookingReminderLog(event.bookingId(), event.reminderType(), OffsetDateTime.now()));
            log.info("Booking reminder log saved for booking {} of type {}", event.bookingId(), event.reminderType());
        } catch (DataIntegrityViolationException e) {
            log.warn("Reminder was concurrently logged for booking {} of type {}. Skipping duplicate log.", event.bookingId(), event.reminderType());
        }
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
