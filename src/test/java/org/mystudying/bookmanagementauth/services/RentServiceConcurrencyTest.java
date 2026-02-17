package org.mystudying.bookmanagementauth.services;

import org.junit.jupiter.api.Test;
import org.mystudying.bookmanagementauth.domain.Book;
import org.mystudying.bookmanagementauth.exceptions.BookNotAvailableException;
import org.mystudying.bookmanagementauth.exceptions.BookNotFoundException;
import org.mystudying.bookmanagementauth.repositories.BookRepository;
import org.mystudying.bookmanagementauth.repositories.BookingRepository;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(UserService.class)
@Sql({"/insertConcurrentUsersTestRecords.sql", "/insertTestRecords.sql"})
public class RentServiceConcurrencyTest {

    private static final String USERS_TABLE = "users";
    private static final String BOOKS_TABLE = "books";
    private static final String BOOKINGS_TABLE = "bookings";
    private static final String AUTHORS_TABLE = "authors"; // Added for final cleanup (because roll back is off in specific test)

    private final JdbcClient jdbcClient;

    private final BookingRepository bookingRepository;

    private final UserService userService;
    private final TransactionTemplate txTemplate;
    private final BookRepository bookRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;   // just for simulate dependency in UserService


    public RentServiceConcurrencyTest(JdbcClient jdbcClient, BookingRepository bookingRepository, UserService userService, TransactionTemplate txTemplate, BookRepository bookRepository) {
        this.jdbcClient = jdbcClient;
        this.bookingRepository = bookingRepository;
        this.userService = userService;
        this.txTemplate = txTemplate;
        this.bookRepository = bookRepository;
    }


    private long idOfRentableBook() {
        return jdbcClient.sql("SELECT id FROM books WHERE title = 'Rentable Book'")
                .query(Long.class)
                .single();
    }

    private List<Long> getConcurrentTestUserIds() {
        return jdbcClient.sql("SELECT id FROM users WHERE email LIKE 'conc.test%@example.com' ORDER BY id")
                .query(Long.class)
                .list();
    }


    // Helper method for manual cleanup in concurrency tests
    // This cleanup is targeted to undo the specific changes made by the concurrency test,
    // and also to clean up any test records inserted by @Sql.
    private void cleanupAllTestData() {
        // Order: bookings -> book_genres -> users_roles -> books -> authors -> users -> genres

        // Delete all records from the test SQL scripts
        // Clear bookings first
        jdbcClient.sql("DELETE FROM " + BOOKINGS_TABLE + " WHERE user_id IN (SELECT id FROM " + USERS_TABLE + " WHERE email IN ('delete@example.com', 'rent@example.com', 'test1@example.com', 'test2@example.com') OR email LIKE 'conc.test%@example.com')").update();

        // Clear book_genres
        jdbcClient.sql("DELETE FROM book_genres WHERE book_id IN (SELECT id FROM " + BOOKS_TABLE + " WHERE title IN ('Book For Deletion', 'Rentable Book', 'Test Book 1', 'Test Book 2'))").update();

        // Clear users_roles
        jdbcClient.sql("DELETE FROM users_roles WHERE user_id IN (SELECT id FROM " + USERS_TABLE + " WHERE email IN ('delete@example.com', 'rent@example.com', 'test1@example.com', 'test2@example.com') OR email LIKE 'conc.test%@example.com')").update();

        // Clear books
        jdbcClient.sql("DELETE FROM " + BOOKS_TABLE + " WHERE title IN ('Book For Deletion', 'Rentable Book', 'Test Book 1', 'Test Book 2')").update();

        // Clear authors, users, genres
        jdbcClient.sql("DELETE FROM " + AUTHORS_TABLE + " WHERE name IN ('Author For Deletion', 'Test Author 1', 'Test Author 2')").update();
        jdbcClient.sql("DELETE FROM " + USERS_TABLE + " WHERE email IN ('delete@example.com', 'rent@example.com', 'test1@example.com', 'test2@example.com') OR email LIKE 'conc.test%@example.com'").update();
        jdbcClient.sql("DELETE FROM genres WHERE name IN ('Test Genre 1', 'Test Genre 2', 'Test Genre 3')").update();
    }

    //    10 Users competing for 5 Copies of Book.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentRentingShouldNotExceedAvailableStock() throws Exception {
        long bookId = idOfRentableBook();
        try {
            txTemplate.execute(status -> {
                jdbcClient.sql("DELETE FROM " + BOOKINGS_TABLE + " WHERE book_id = ?").param(bookId).update();
                jdbcClient.sql("UPDATE " + BOOKS_TABLE + " SET available = 5 WHERE id = ?").param(bookId).update();
                return null;
            });
            List<Long> userIds = getConcurrentTestUserIds();

            ExecutorService executor = Executors.newFixedThreadPool(10);
            CountDownLatch ready = new CountDownLatch(10);
            CountDownLatch start = new CountDownLatch(1);

            List<Future<HttpStatus>> futures = new ArrayList<>();

            for (Long userId : userIds) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();

                    try {
                        userService.rentBook(userId, bookId);
                        return HttpStatus.NO_CONTENT;
                    } catch (BookNotAvailableException e) {
                        return HttpStatus.CONFLICT;
                    }
                }));
            }

            ready.await();   // all threads ready
            start.countDown(); // fire simultaneously

            List<HttpStatus> results = new ArrayList<>();
            for (Future<HttpStatus> f : futures) {
                results.add(f.get());
            }

            executor.shutdown();

            long successCount = results.stream()
                    .filter(s -> s == HttpStatus.NO_CONTENT)
                    .count();

            long conflictCount = results.stream()
                    .filter(s -> s == HttpStatus.CONFLICT)
                    .count();

            assertThat(successCount).isEqualTo(5);
            assertThat(conflictCount).isEqualTo(5);

            // Final DB verification
            Book book = bookRepository.findById(bookId).orElseThrow(() -> new BookNotFoundException(bookId));
            assertThat(book.getAvailable()).isEqualTo(0);

            assertThat(bookingRepository.countByBookId(bookId))
                    .isEqualTo(5);

        } finally {
            cleanupAllTestData();
        }
    }
}
