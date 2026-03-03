package org.mystudying.bookmanagementauth.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "booking_reminder_log")
@Getter
@Setter
@NoArgsConstructor
public class BookingReminderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bookingId;

    @Enumerated(EnumType.STRING) // Map Enum to String in DB
    @Column(nullable = false, length = 50)
    private ReminderType reminderType;

    @Column(nullable = false)
    private OffsetDateTime sentAt;

    public BookingReminderLog(Long bookingId, ReminderType reminderType, OffsetDateTime sentAt) {
        this.bookingId = bookingId;
        this.reminderType = reminderType;
        this.sentAt = sentAt;
    }
}
