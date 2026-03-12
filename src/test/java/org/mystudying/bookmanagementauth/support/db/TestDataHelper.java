package org.mystudying.bookmanagementauth.support.db;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Helper component for common test data lookup operations.
 */
@Component
public class TestDataHelper {

    private final JdbcClient jdbcClient;

    public TestDataHelper(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public long idOfUser(String email) {
        return jdbcClient.sql("SELECT id FROM users WHERE email = ?")
                .param(email)
                .query(Long.class)
                .single();
    }

    public long idOfBook(String title) {
        return jdbcClient.sql("SELECT id FROM books WHERE title = ?")
                .param(title)
                .query(Long.class)
                .single();
    }

    public long idOfAuthor(String name) {
        return jdbcClient.sql("SELECT id FROM authors WHERE name = ?")
                .param(name)
                .query(Long.class)
                .single();
    }

    public long idOfGenre(String name) {
        return jdbcClient.sql("SELECT id FROM genres WHERE name = ?")
                .param(name)
                .query(Long.class)
                .single();
    }

    public long idOfBooking(long userId, long bookId) {
        return jdbcClient.sql("SELECT id FROM bookings WHERE user_id = ? AND book_id = ? AND returned_at IS NULL")
                .param(userId)
                .param(bookId)
                .query(Long.class)
                .single();
    }

    public List<Long> userIdsByEmailLike(String pattern) {
        return jdbcClient.sql("SELECT id FROM users WHERE email LIKE ? ORDER BY id")
                .param(pattern)
                .query(Long.class)
                .list();
    }
}
