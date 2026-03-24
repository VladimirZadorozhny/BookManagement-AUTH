package org.mystudying.bookmanagementauth.events;

import org.mystudying.bookmanagementauth.domain.ReminderType;

import java.time.LocalDate;

public record BookingReminderEvent(
        Long bookingId,
        String userName,
        String email,
        String bookTitle,
        LocalDate dueDate,
        ReminderType reminderType
) {
}
