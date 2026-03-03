package org.mystudying.bookmanagementauth.events;

import org.mystudying.bookmanagementauth.domain.ReminderType;

import java.time.OffsetDateTime;

public record BookingReminderEvent(
        Long bookingId,
        String email,
        String bookTitle,
        OffsetDateTime dueDate,
        ReminderType reminderType
) {
}
