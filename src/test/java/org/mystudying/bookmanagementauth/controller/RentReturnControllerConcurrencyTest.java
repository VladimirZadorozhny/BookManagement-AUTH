package org.mystudying.bookmanagementauth.controller;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Sql({"/insertConcurrentUsersTestRecords.sql", "/insertTestRecords.sql"})
public class RentReturnControllerConcurrencyTest {
    private static final String USERS_TABLE = "users";
    private static final String BOOKS_TABLE = "books";
    private static final String BOOKINGS_TABLE = "bookings";
    private static final String AUTHORS_TABLE = "authors"; // Added for final cleanup (because roll back is off in specific test)


    private final MockMvc mockMvc;
    private final JdbcClient jdbcClient;
    private final TransactionTemplate txTemplate;


    public RentReturnControllerConcurrencyTest(MockMvc mockMvc, JdbcClient jdbcClient, TransactionTemplate txTemplate) {
        this.mockMvc = mockMvc;
        this.jdbcClient = jdbcClient;
        this.txTemplate = txTemplate;

    }

    private long idOfTestUser1() {
        return jdbcClient.sql("select id from users where email = 'test1@example.com'")
                .query(Long.class)
                .single();
    }

    private long idOfTestUser2() {
        return jdbcClient.sql("select id from users where email = 'test2@example.com'")
                .query(Long.class)
                .single();
    }


    private long idOfRentUser() {
        return jdbcClient.sql("select id from users where email = 'rent@example.com'")
                .query(Long.class)
                .single();
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


    // Helper method to read JSON from file
    private String readJsonFile(String filename) throws IOException {
        return new ClassPathResource(filename).getContentAsString(StandardCharsets.UTF_8);
    }

    // Helper method for manual cleanup in concurrency tests
    // This cleanup is targeted to undo the specific changes made by the concurrency test,
    // and also to clean up any test records inserted by @Sql.
    private void dbCleanup(long user1Id, long user2Id, long bookId) {
        // Order: bookings -> book_genres -> users_roles -> books -> authors -> users -> genres

        // 1. Delete all bookings involving these users or this book
        jdbcClient.sql("DELETE FROM " + BOOKINGS_TABLE + " WHERE user_id = ? OR user_id = ? OR book_id = ?")
                .param(user1Id).param(user2Id).param(bookId).update();


        // 2. Delete all records from insertTestRecords.sql
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

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    // Important for concurrency tests due @Transactional also in Service class that is used here in test
    @WithMockUser(roles = "ADMIN")
    void rentBookConcurrentAccessTwoUsersOneBookAvailableShouldOneSucceedOneFail() throws Exception {
        long user1Id = idOfRentUser();
        long user2Id = idOfTestUser2();
        long bookId = idOfRentableBook(); // This book has 1 available initially

        // Setup: ensure the book is available and not booked by anyone.

        txTemplate.execute(status -> {
            jdbcClient.sql("DELETE FROM " + BOOKINGS_TABLE + " WHERE book_id = ?").param(bookId).update();
            jdbcClient.sql("UPDATE " + BOOKS_TABLE + " SET available = 1 WHERE id = ?").param(bookId).update();
            return null;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Define two tasks, one for each user that are trying to rent the same book.
        Callable<Integer> task1 = new DelegatingSecurityContextCallable<>(() -> {
            String rentRequestJson = readJsonFile("rentOrReturnBookRequest.json").replace("1", String.valueOf(bookId));
            return mockMvc.perform(post("/api/users/{userId}/rent", user1Id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(rentRequestJson))
                    .andReturn().getResponse().getStatus();
        });

        Callable<Integer> task2 = new DelegatingSecurityContextCallable<>(() -> {
            String rentRequestJson = readJsonFile("rentOrReturnBookRequest.json").replace("1", String.valueOf(bookId));
            return mockMvc.perform(post("/api/users/{userId}/rent", user2Id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(rentRequestJson))
                    .andReturn().getResponse().getStatus();
        });

        List<Callable<Integer>> tasks = new ArrayList<>(List.of(task1, task2));

        try {
            // Invoke both tasks concurrently and collect their status codes
            List<Integer> statusCodes = executor.invokeAll(tasks)
                    .stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());


            // Assert that one request succeeded (204) and the other - failed (409), but without guaranty which task did the job first; we check the set content but not the codes' order
            assertThat(statusCodes).containsExactlyInAnyOrder(204, 409);

            // Verify final state in the database
            assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcClient, BOOKINGS_TABLE, "book_id = " + bookId)).isEqualTo(1);
            assertThat(jdbcClient.sql("SELECT available FROM " + BOOKS_TABLE + " WHERE id = ?").param(bookId).query(Integer.class).single()).isEqualTo(0);

        } finally {
            executor.shutdown();
            txTemplate.execute(status -> {
                dbCleanup(user1Id, user2Id, bookId);
                return null;
            });
        }
    }


    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    // Important for concurrency tests due @Transactional also in Service class that is used here in test
    @WithMockUser(roles = "ADMIN")
    void rentBookConcurrentAccessTwoUsersTwoBooksAvailableShouldBothSucceed() throws Exception {
        long user1Id = idOfRentUser();
        long user2Id = idOfTestUser2();
        long bookId = idOfRentableBook();

        // Setup: ensure the book is available (2 available) and not booked by anyone.

        txTemplate.execute(status -> {
            jdbcClient.sql("DELETE FROM " + BOOKINGS_TABLE + " WHERE book_id = ?").param(bookId).update();
            jdbcClient.sql("UPDATE " + BOOKS_TABLE + " SET available = 2 WHERE id = ?").param(bookId).update();
            return null;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Define two tasks, one for each user that are trying to rent the same book.
        Callable<Integer> task1 = new DelegatingSecurityContextCallable<>(() -> {
            String rentRequestJson = readJsonFile("rentOrReturnBookRequest.json").replace("1", String.valueOf(bookId));
            return mockMvc.perform(post("/api/users/{userId}/rent", user1Id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(rentRequestJson))
                    .andReturn().getResponse().getStatus();
        });

        Callable<Integer> task2 = new DelegatingSecurityContextCallable<>(() -> {
            String rentRequestJson = readJsonFile("rentOrReturnBookRequest.json").replace("1", String.valueOf(bookId));
            return mockMvc.perform(post("/api/users/{userId}/rent", user2Id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(rentRequestJson))
                    .andReturn().getResponse().getStatus();
        });

        List<Callable<Integer>> tasks = new ArrayList<>(List.of(task1, task2));

        try {
            // Invoke both tasks concurrently and collect their status codes
            List<Integer> statusCodes = executor.invokeAll(tasks)
                    .stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());

            // Assert that both requests succeeded (204)
            assertThat(statusCodes).containsOnly(204);

            // Verify final state in the database
            assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcClient, BOOKINGS_TABLE, "book_id = " + bookId)).isEqualTo(2);
            assertThat(jdbcClient.sql("SELECT available FROM " + BOOKS_TABLE + " WHERE id = ?").param(bookId).query(Integer.class).single()).isEqualTo(0);

        } finally {
            executor.shutdown();
            txTemplate.execute(status -> {
                dbCleanup(user1Id, user2Id, bookId);
                return null;
            });
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    // Important for concurrency tests due @Transactional also in Service class that is used here in test
    @WithMockUser(roles = "ADMIN")
    void rentBookConcurrentAccessTenUsersFiveBooksAvailableShouldFiveSucceedAnd5Fail() throws Exception {

        long bookId = idOfRentableBook();

        // Setup: ensure the book is available (available = 5) and not booked by anyone (for clear comparing).

        txTemplate.execute(status -> {
            jdbcClient.sql("DELETE FROM " + BOOKINGS_TABLE + " WHERE book_id = ?").param(bookId).update();
            jdbcClient.sql("UPDATE " + BOOKS_TABLE + " SET available = 5 WHERE id = ?").param(bookId).update();
            return null;
        });

        int booksAvailable = jdbcClient.sql("SELECT available FROM " + BOOKS_TABLE + " WHERE id = ?").param(bookId).query(Integer.class).single();

//       IDs of 10 users that are going to rent the same book simultaneously.
        List<Long> userIds = getConcurrentTestUserIds();

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Callable<Integer>> futures = new ArrayList<>();
        String rentRequestJson = readJsonFile("rentOrReturnBookRequest.json").replace("1", String.valueOf(bookId));

        // Define 10 tasks, one for each user that are trying to rent the same book.
        for (Long userId : userIds) {
            futures.add(new DelegatingSecurityContextCallable<>(() ->
                    mockMvc.perform(post("/api/users/{userId}/rent", userId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(rentRequestJson))
                            .andReturn().getResponse().getStatus()));
        }

        try {
            // Invoke ten tasks concurrently and collect their status codes
            List<Integer> statusCodes = executor.invokeAll(futures)
                    .stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());

            long successCount = statusCodes.stream()
                    .filter(code -> code == 204)
                    .count();

            long conflictCount = statusCodes.stream()
                    .filter(code -> code == 409)
                    .count();

//            due to domain business rules (book's available can't be less 0), the users can succeed with renting only while the book is available
            assertThat(successCount).isEqualTo(booksAvailable);
//            the rest users must fail with renting due to conflict (book is not available anymore)
            assertThat(conflictCount).isEqualTo(statusCodes.size() - successCount);

            // Assert that code status of the requests is only (204 No Content = Book is rented) or (409 Conflict)
            assertThat(statusCodes).containsOnly(204, 409);

            // Verify final state in the database (can be checked without flush(), because
            // creating booking - is automatically flushed and visible in DB
            // and decrement book's available is atomic DB update)
            assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcClient, BOOKINGS_TABLE, "book_id = " + bookId)).isEqualTo(5);
            assertThat(jdbcClient.sql("SELECT available FROM " + BOOKS_TABLE + " WHERE id = ?").param(bookId).query(Integer.class).single()).isEqualTo(0);

        } finally {
            executor.shutdown();
            txTemplate.execute(status -> {
                dbCleanup(idOfTestUser1(), idOfTestUser2(), bookId);
                return null;
            });
        }
    }


}
