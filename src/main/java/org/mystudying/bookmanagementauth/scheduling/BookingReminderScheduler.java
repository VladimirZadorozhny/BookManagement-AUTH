package org.mystudying.bookmanagementauth.scheduling;

import org.mystudying.bookmanagementauth.domain.ReminderType;
import org.mystudying.bookmanagementauth.repositories.BookingRepository;
import org.mystudying.bookmanagementauth.services.BookingReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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

        OffsetDateTime startOfDay = today.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = today.atTime(23, 59, 59).atOffset(ZoneOffset.UTC);

        // Reminders for books due in 3 days
        OffsetDateTime threeDaysLater = today.plusDays(3).atTime(23, 59, 59).atOffset(ZoneOffset.UTC);
        try {
            List<Long> dueSoonIds = bookingRepository.findBookingIdsDueInDays(startOfDay, threeDaysLater);
            dueSoonIds.forEach(id -> reminderService.processReminder(id, ReminderType.THREE_DAYS_LEFT));
        } catch (Exception e) {
            log.error("Failed to process due-soon reminders", e);
        }

        // Reminders for books due today
        try {
            List<Long> dueTodayIds = bookingRepository.findBookingIdsDueToday(startOfDay, endOfDay);
            dueTodayIds.forEach(id -> reminderService.processReminder(id, ReminderType.DUE_TODAY));
        } catch (Exception e) {
            log.error("Failed to process due-today reminders", e);
        }

        // Reminders for books 1 day overdue
        OffsetDateTime oneDayAgoStart = today.minusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime oneDayAgoEnd = today.minusDays(1).atTime(23, 59, 59).atOffset(ZoneOffset.UTC);
        try {
            List<Long> overdueIds = bookingRepository.findBookingIdsOverdueByDays(oneDayAgoStart, oneDayAgoEnd);
            overdueIds.forEach(id -> reminderService.processReminder(id, ReminderType.OVERDUE));
        } catch (Exception e) {
            log.error("Failed to process overdue reminders", e);
        }

        log.info("Finished scheduled booking reminder job.");
    }
}
