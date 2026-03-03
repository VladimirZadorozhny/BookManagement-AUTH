-- V6__create_booking_reminder_log_table.sql

CREATE TABLE booking_reminder_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    reminder_type VARCHAR(50) NOT NULL, -- Stores ReminderType enum names (e.g., 'THREE_DAYS_LEFT', 'DUE_TODAY', 'OVERDUE')
    sent_at DATETIME(6) NOT NULL,

    CONSTRAINT uc_booking_reminder UNIQUE (booking_id, reminder_type)
);
