package org.mystudying.bookmanagementauth.repositories;

import jakarta.persistence.LockModeType;
import org.mystudying.bookmanagementauth.domain.BookingReminderLog;
import org.mystudying.bookmanagementauth.domain.ReminderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BookingReminderLogRepository extends JpaRepository<BookingReminderLog, Long> {
    boolean existsByBookingIdAndReminderType(Long bookingId, ReminderType reminderType);

    int countByBookingIdAndReminderType(long bookingId, ReminderType reminderType);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from BookingReminderLog r
            where r.bookingId = :bookingId
            and r.reminderType = :reminderType
            """)
    Optional<BookingReminderLog> findForUpdate(Long bookingId, ReminderType reminderType);
}
