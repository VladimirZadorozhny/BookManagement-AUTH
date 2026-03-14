package org.mystudying.bookmanagementauth.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "booking_reminder_log")

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

    protected BookingReminderLog() {
    }

    public Long getId() {
        return id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public ReminderType getReminderType() {
        return reminderType;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }
}
