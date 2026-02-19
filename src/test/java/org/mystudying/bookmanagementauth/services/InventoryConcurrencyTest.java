package org.mystudying.bookmanagementauth.services;

import org.junit.jupiter.api.Test;
import org.mystudying.bookmanagementauth.domain.Book;
import org.mystudying.bookmanagementauth.exceptions.BookNotFoundException;
import org.mystudying.bookmanagementauth.exceptions.InsufficientAvailableStockException;
import org.mystudying.bookmanagementauth.repositories.BookRepository;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import(InventoryService.class)
@Sql({"/insertTestRecords.sql"})
public class InventoryConcurrencyTest {

    private static final String USERS_TABLE = "users";
    private static final String BOOKS_TABLE = "books";
    private static final String BOOKINGS_TABLE = "bookings";
    private static final String AUTHORS_TABLE = "authors";

    private final JdbcClient jdbcClient;
    private final InventoryService inventoryService;
    private final TransactionTemplate txTemplate;
    private final BookRepository bookRepository;

    public InventoryConcurrencyTest(JdbcClient jdbcClient, InventoryService inventoryService, TransactionTemplate txTemplate, BookRepository bookRepository) {
        this.jdbcClient = jdbcClient;
        this.inventoryService = inventoryService;
        this.txTemplate = txTemplate;
        this.bookRepository = bookRepository;
    }

    private long idOfRentableBook() {
        return jdbcClient.sql("SELECT id FROM books WHERE title = 'Rentable Book'")
                .query(Long.class)
                .single();
    }

    private void cleanupAllTestData() {
        // Order: bookings -> book_genres -> users_roles -> books -> authors -> users -> genres

        // Delete all records from the test SQL scripts
        // Clear bookings first
        jdbcClient.sql("DELETE FROM " + BOOKINGS_TABLE + " WHERE user_id IN (SELECT id FROM " + USERS_TABLE + " WHERE email IN ('delete@example.com', 'rent@example.com', 'test1@example.com', 'test2@example.com'))").update();

        // Clear book_genres
        jdbcClient.sql("DELETE FROM book_genres WHERE book_id IN (SELECT id FROM " + BOOKS_TABLE + " WHERE title IN ('Book For Deletion', 'Rentable Book', 'Test Book 1', 'Test Book 2'))").update();

        // Clear users_roles
        jdbcClient.sql("DELETE FROM users_roles WHERE user_id IN (SELECT id FROM " + USERS_TABLE + " WHERE email IN ('delete@example.com', 'rent@example.com', 'test1@example.com', 'test2@example.com'))").update();

        // Clear books
        jdbcClient.sql("DELETE FROM " + BOOKS_TABLE + " WHERE title IN ('Book For Deletion', 'Rentable Book', 'Test Book 1', 'Test Book 2')").update();

        // Clear authors, users, genres
        jdbcClient.sql("DELETE FROM " + AUTHORS_TABLE + " WHERE name IN ('Author For Deletion', 'Test Author 1', 'Test Author 2')").update();
        jdbcClient.sql("DELETE FROM " + USERS_TABLE + " WHERE email IN ('delete@example.com', 'rent@example.com', 'test1@example.com', 'test2@example.com')").update();
        jdbcClient.sql("DELETE FROM genres WHERE name IN ('Test Genre 1', 'Test Genre 2', 'Test Genre 3')").update();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentReturnsShouldIncreaseStockCorrectly() throws Exception {
        long bookId = idOfRentableBook();
        try {
            txTemplate.execute(status -> {
                jdbcClient.sql("UPDATE books SET available = 0 WHERE id = ?").param(bookId).update();
                return null;
            });

            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        inventoryService.incrementStock(bookId);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            Book book = bookRepository.findById(bookId).orElseThrow(() -> new BookNotFoundException(bookId));
            assertThat(book.getAvailable()).isEqualTo(threadCount);
        } finally {
            cleanupAllTestData();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void writeOffFailsIfNotEnoughStock() {
        long bookId = idOfRentableBook();
        try {
            txTemplate.execute(status -> {
                jdbcClient.sql("UPDATE books SET available = 2 WHERE id = ?").param(bookId).update();
                return null;
            });

            assertThrows(InsufficientAvailableStockException.class, () -> {
                inventoryService.writeOff(bookId, 5);
            });

            Book book = bookRepository.findById(bookId).orElseThrow(() -> new BookNotFoundException(bookId));
            assertThat(book.getAvailable()).isEqualTo(2);
        } finally {
            cleanupAllTestData();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentWriteOffsShouldBeSafe() throws Exception {
        long bookId = idOfRentableBook();
        try {
            txTemplate.execute(status -> {
                jdbcClient.sql("UPDATE books SET available = 10 WHERE id = ?").param(bookId).update();
                return null;
            });

            int attempts = 5;
            int writeOffAmount = 3; // Total 15 requested, only 10 available
            ExecutorService executor = Executors.newFixedThreadPool(attempts);
            List<Future<Boolean>> results = new ArrayList<>();

            for (int i = 0; i < attempts; i++) {
                results.add(executor.submit(() -> {
                    try {
                        inventoryService.writeOff(bookId, writeOffAmount);
                        return true;
                    } catch (InsufficientAvailableStockException e) {
                        return false;
                    }
                }));
            }

            long successCount = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) successCount++;
            }
            executor.shutdown();

            assertThat(successCount).isEqualTo(3); // 3 * 3 = 9. 4th would be 12 > 10.
            Book book = bookRepository.findById(bookId).orElseThrow(() -> new BookNotFoundException(bookId));
            assertThat(book.getAvailable()).isEqualTo(1); // 10 - 9 = 1
        } finally {
            cleanupAllTestData();
        }
    }


}
