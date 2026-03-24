package org.mystudying.bookmanagementauth.scheduling;

import org.mystudying.bookmanagementauth.domain.ReminderType;
import org.mystudying.bookmanagementauth.exceptions.NonRetryableMailException;
import org.mystudying.bookmanagementauth.repositories.BookingRepository;
import org.mystudying.bookmanagementauth.services.BookingReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class BookingReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingReminderScheduler.class);
    private final BookingRepository bookingRepository;
    private final BookingReminderService reminderService;

    public BookingReminderScheduler(BookingRepository bookingRepository, BookingReminderService reminderService) {
        this.bookingRepository = bookingRepository;
        this.reminderService = reminderService;
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "UTC") // Every day at 9 AM
    public void sendReminders() {
        log.info("Running scheduled booking reminder job.");
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        LocalDate threeDaysLater = today.plusDays(3);
        LocalDate oneDayAgo = today.minusDays(1);
        LocalDate twoDaysAgo = today.minusDays(2);


        // Reminders for books due in 3 days
        try {
            List<Long> dueSoonIds = bookingRepository.findBookingIdsDueInDays(today, threeDaysLater);
            for (Long id : dueSoonIds) {
                try {
                    reminderService.processReminder(id, ReminderType.THREE_DAYS_LEFT);
                } catch (NonRetryableMailException e) {
                    log.warn("Skipping missing booking {}", id);
                } catch (Exception e) {
                    log.error("Unexpecting error for booking {}", id, e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process due-soon reminders", e);
        }

        // Reminders for books due today
        try {
            List<Long> dueTodayIds = bookingRepository.findBookingIdsDueToday(today, today);
            for (Long id : dueTodayIds) {
                try {
                    reminderService.processReminder(id, ReminderType.DUE_TODAY);
                } catch (NonRetryableMailException e) {
                    log.warn("Skipping missing booking {}", id);
                } catch (Exception e) {
                    log.error("Unexpecting error for booking {}", id, e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process due-today reminders", e);
        }

        // Reminders for books 1 day overdue

        try {
            List<Long> overdueIds = bookingRepository.findBookingIdsOverdueByDays(oneDayAgo, twoDaysAgo);
            for (Long id : overdueIds) {
                try {
                    reminderService.processReminder(id, ReminderType.OVERDUE);
                } catch (NonRetryableMailException e) {
                    log.warn("Skipping missing booking {}", id);
                } catch (Exception e) {
                    log.error("Unexpecting error for booking {}", id, e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process overdue reminders", e);
        }

        log.info("Finished scheduled booking reminder job.");
    }
}
