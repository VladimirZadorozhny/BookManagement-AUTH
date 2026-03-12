package org.mystudying.bookmanagementauth.support.db;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Helper component for surgical cleanup of test data.
 * Designed to clean up ONLY specific test records without affecting seed data.
 */
@Component
public class TestDataCleanup {

    private final JdbcClient jdbcClient;

    public TestDataCleanup(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Deletes a specific user and all its dependent records (tokens, bookings, roles).
     */
    @Transactional
    public void deleteUserCascade(String email) {
        jdbcClient.sql("SELECT id FROM users WHERE email = ?").param(email).query(Long.class).optional().ifPresent(userId -> {
            jdbcClient.sql("DELETE FROM verification_token WHERE user_id = ?").param(userId).update();
            jdbcClient.sql("DELETE FROM password_reset_token WHERE user_id = ?").param(userId).update();
            jdbcClient.sql("DELETE FROM bookings WHERE user_id = ?").param(userId).update();
            jdbcClient.sql("DELETE FROM users_roles WHERE user_id = ?").param(userId).update();
            jdbcClient.sql("DELETE FROM users WHERE id = ?").param(userId).update();
        });
    }

    /**
     * Deletes failed mail logs for a specific recipient.
     */
    @Transactional
    public void deleteFailedMailsByRecipient(String email) {
        jdbcClient.sql("DELETE FROM failed_mail_log WHERE to_email = ?").param(email).update();
    }

    /**
     * Deletes booking reminders for a specific booking.
     */
    @Transactional
    public void deleteRemindersByBookingId(long bookingId) {
        jdbcClient.sql("DELETE FROM booking_reminder_log WHERE booking_id = ?").param(bookingId).update();
    }

    /**
     * Specifically cleans up all records inserted by:
     * - insertTestRecords.sql
     * - insertConcurrentUsersTestRecords.sql
     * - insertUserLogicTestRecords.sql
     */
    @Transactional
    public void cleanupAllTestSqlData() {
        // Emails from all test SQL files
        List<String> testEmails = List.of(
                "test1@example.com", "test2@example.com", "delete@example.com", "rent@example.com",
                "clean@logic.test", "overdue@logic.test", "fine@logic.test"
        );

        // 1. Delete by email-based lookups
        testEmails.forEach(this::deleteUserCascade);

        // 2. Delete concurrent test users by pattern
        jdbcClient.sql("SELECT id FROM users WHERE email LIKE 'conc.test%@example.com'").query(Long.class).list()
                .forEach(id -> {
                    jdbcClient.sql("DELETE FROM bookings WHERE user_id = ?").param(id).update();
                    jdbcClient.sql("DELETE FROM users_roles WHERE user_id = ?").param(id).update();
                    jdbcClient.sql("DELETE FROM users WHERE id = ?").param(id).update();
                });

        // 3. Delete Books created in tests
        List<String> testBookTitles = List.of(
                "Test Book 1", "Test Book 2", "Book For Deletion", "Rentable Book",
                "Logic Book A", "Logic Book B", "Overdue Book", "Fined Book"
        );

        if (!testBookTitles.isEmpty()) {
            jdbcClient.sql("DELETE FROM book_genres WHERE book_id IN (SELECT id FROM books WHERE title IN (:titles))")
                    .param("titles", testBookTitles).update();
            jdbcClient.sql("DELETE FROM bookings WHERE book_id IN (SELECT id FROM books WHERE title IN (:titles))")
                    .param("titles", testBookTitles).update();
            jdbcClient.sql("DELETE FROM books WHERE title IN (:titles)")
                    .param("titles", testBookTitles).update();
        }

        // 4. Delete Authors created in tests
        List<String> testAuthorNames = List.of(
                "Test Author 1", "Test Author 2", "Author For Deletion", "Logic Author"
        );
        if (!testAuthorNames.isEmpty()) {
            jdbcClient.sql("DELETE FROM authors WHERE name IN (:names)")
                    .param("names", testAuthorNames).update();
        }

        // 5. Delete Genres created in tests
        List<String> testGenreNames = List.of(
                "Test Genre 1", "Test Genre 2", "Test Genre 3", "Logic Genre"
        );
        if (!testGenreNames.isEmpty()) {
            jdbcClient.sql("DELETE FROM genres WHERE name IN (:names)")
                    .param("names", testGenreNames).update();
        }
    }
}
