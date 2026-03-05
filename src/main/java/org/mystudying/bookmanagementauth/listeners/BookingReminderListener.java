package org.mystudying.bookmanagementauth.listeners;

import jakarta.mail.MessagingException;
import org.mystudying.bookmanagementauth.domain.BookingReminderLog;
import org.mystudying.bookmanagementauth.domain.User;
import org.mystudying.bookmanagementauth.events.BookingReminderEvent;
import org.mystudying.bookmanagementauth.repositories.BookingReminderLogRepository;
import org.mystudying.bookmanagementauth.repositories.UserRepository;
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
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Component
public class BookingReminderListener {

    private static final Logger log = LoggerFactory.getLogger(BookingReminderListener.class);
    private final MailService mailService;
    private final MailTemplateService mailTemplateService;
    private final BookingReminderLogRepository bookingReminderLogRepository;
    private final UserRepository userRepository;

    public BookingReminderListener(MailService mailService, MailTemplateService mailTemplateService, BookingReminderLogRepository bookingReminderLogRepository, UserRepository userRepository) {
        this.mailService = mailService;
        this.mailTemplateService = mailTemplateService;
        this.bookingReminderLogRepository = bookingReminderLogRepository;
        this.userRepository = userRepository;
    }

    @Async("mailExecutor")
    @EventListener
    @Transactional
    @Retryable(
            retryFor = MessagingException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void handleBookingReminder(BookingReminderEvent event) throws MessagingException {
        // First, attempt to send the email
        log.info("Attempting to send booking reminder email of type {} to: {}", event.reminderType(), event.email());
        String subject = "";

        // Fetch user name for personalized email
        Optional<User> userOptional = userRepository.findByEmail(event.email());
        String userName = userOptional.map(User::getName).orElse("User");


        switch (event.reminderType()) {
            case THREE_DAYS_LEFT:
                subject = "Reminder: Your book '" + event.bookTitle() + "' is due soon!";
                break;
            case DUE_TODAY:
                subject = "Reminder: Your book '" + event.bookTitle() + "' is due today!";
                break;
            case OVERDUE:
                subject = "Action Required: Your book '" + event.bookTitle() + "' is overdue!";
                break;
            default:
                log.warn("Unknown reminder type: {}", event.reminderType());
                return;
        }

        // Use MailTemplateService to build the body
        String body = mailTemplateService.buildReminderMail(userName, event.bookTitle(), event.dueDate(), event.reminderType());

        mailService.send(event.email(), subject, body);
        log.info("Booking reminder email of type {} sent to: {}", event.reminderType(), event.email());

        // Log successful send to prevent duplicates, only AFTER email is successfully sent
        try {
            bookingReminderLogRepository.save(new BookingReminderLog(event.bookingId(), event.reminderType(), OffsetDateTime.now()));
            log.info("Booking reminder log saved for booking {} of type {}", event.bookingId(), event.reminderType());
        } catch (DataIntegrityViolationException e) {
            log.warn("Reminder already logged for booking {} of type {}. This should not happen if previous send was successful.", event.bookingId(), event.reminderType());
        }
    }

    @Recover
    public void recover(MessagingException e, BookingReminderEvent event) {
        log.error("Failed to send booking reminder email of type {} to {} after multiple retries: {}",
                event.reminderType(), event.email(), e.getMessage());
        // TODO: Potentially notify admin or store in a dead-letter queue
    }
}
