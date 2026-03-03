package org.mystudying.bookmanagementauth.scheduling;

import org.mystudying.bookmanagementauth.domain.ReminderType;
import org.mystudying.bookmanagementauth.repositories.BookingRepository;
import org.mystudying.bookmanagementauth.services.BookingReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
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

    @Scheduled(cron = "0 0 9 * * *") // Every day at 9 AM
    public void sendReminders() {
        log.info("Running scheduled booking reminder job.");
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime endOfDay = now.withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);

        // Reminders for books due in 3 days
        OffsetDateTime threeDaysLater = now.plusDays(3).withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);
        List<Long> dueSoonIds = bookingRepository.findBookingIdsDueInDays(startOfDay, threeDaysLater);
        dueSoonIds.forEach(id -> reminderService.processReminder(id, ReminderType.THREE_DAYS_LEFT));

        // Reminders for books due today
        List<Long> dueTodayIds = bookingRepository.findBookingIdsDueToday(startOfDay, endOfDay);
        dueTodayIds.forEach(id -> reminderService.processReminder(id, ReminderType.DUE_TODAY));

        // Reminders for books 1 day overdue
        OffsetDateTime oneDayAgoStart = now.minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime oneDayAgoEnd = now.minusDays(1).withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);
        List<Long> overdueIds = bookingRepository.findBookingIdsOverdueByDays(oneDayAgoStart, oneDayAgoEnd);
        overdueIds.forEach(id -> reminderService.processReminder(id, ReminderType.OVERDUE));

        log.info("Finished scheduled booking reminder job.");
    }
}
