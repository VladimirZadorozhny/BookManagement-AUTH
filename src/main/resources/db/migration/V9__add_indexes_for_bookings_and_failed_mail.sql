-- V9__add_indexes_for_bookings_and_failed_mail.sql

CREATE INDEX idx_bookings_due_at ON bookings (due_at);

CREATE INDEX idx_failed_mail_to_email ON failed_mail_log (to_email);
CREATE INDEX idx_failed_mail_created_at ON failed_mail_log (created_at);
