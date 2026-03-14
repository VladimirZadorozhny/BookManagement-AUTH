package org.mystudying.bookmanagementauth.services;

import jakarta.mail.MessagingException;
import org.mystudying.bookmanagementauth.domain.BookingReminderLog;
import org.mystudying.bookmanagementauth.events.BookingReminderEvent;
import org.mystudying.bookmanagementauth.repositories.BookingReminderLogRepository;
import org.mystudying.bookmanagementauth.services.mail.MailService;
import org.mystudying.bookmanagementauth.services.mail.MailTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class ReminderProcessingService {
    private static final Logger log = LoggerFactory.getLogger(ReminderProcessingService.class);
    private final MailService mailService;
    private final MailTemplateService mailTemplateService;
    private final BookingReminderLogRepository bookingReminderLogRepository;

    public ReminderProcessingService(MailService mailService, MailTemplateService mailTemplateService, BookingReminderLogRepository bookingReminderLogRepository) {
        this.mailService = mailService;
        this.mailTemplateService = mailTemplateService;
        this.bookingReminderLogRepository = bookingReminderLogRepository;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processReminder(BookingReminderEvent event) throws MessagingException {
        // 1. Check if already sent (Idempotency check)
        Optional<BookingReminderLog> existing = bookingReminderLogRepository.findForUpdate(event.bookingId(), event.reminderType());
        if (existing.isPresent()) {
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
