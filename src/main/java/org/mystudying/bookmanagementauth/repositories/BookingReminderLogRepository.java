package org.mystudying.bookmanagementauth.repositories;

import org.mystudying.bookmanagementauth.domain.BookingReminderLog;
import org.mystudying.bookmanagementauth.domain.ReminderType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingReminderLogRepository extends JpaRepository<BookingReminderLog, Long> {
    boolean existsByBookingIdAndReminderType(Long bookingId, ReminderType reminderType);
}
