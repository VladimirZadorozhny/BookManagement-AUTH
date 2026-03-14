package org.mystudying.bookmanagementauth.controller;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mystudying.bookmanagementauth.support.AbstractSecurityIntegrationTest;
import org.mystudying.bookmanagementauth.support.TestJsonUtils;
import org.mystudying.bookmanagementauth.support.db.TestFixtures;
import org.springframework.http.MediaType;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Sql({"/insertConcurrentUsersTestRecords.sql", "/insertTestRecords.sql"})
public class RentReturnControllerConcurrencyTest extends AbstractSecurityIntegrationTest {
    private static final String BOOKS_TABLE = "books";
    private static final String BOOKINGS_TABLE = "bookings";

    private List<Long> getConcurrentTestUserIds() {
        return testDataHelper.userIdsByEmailLike("conc.test%@example.com");
    }


    @AfterEach
    void cleanup() {
        testDataCleanup.cleanupAllTestSqlData();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    // Important for concurrency tests due @Transactional also in Service class that is used here in test
    @WithMockUser(roles = "ADMIN")
    void rentBookConcurrentAccessTwoUsersOneBookAvailableShouldOneSucceedOneFail() throws Exception {
        long user1Id = testDataHelper.idOfUser(TestFixtures.USER_RENT_EMAIL);
        long user2Id = testDataHelper.idOfUser(TestFixtures.USER_2_EMAIL);
        long bookId = testDataHelper.idOfBook(TestFixtures.BOOK_RENTABLE_TITLE); // This book has 1 available initially

        // Setup: ensure the book is available and not booked by anyone.

        jdbcClient.sql("DELETE FROM " + BOOKINGS_TABLE + " WHERE book_id = ?").param(bookId).update();
        jdbcClient.sql("UPDATE " + BOOKS_TABLE + " SET available = 1 WHERE id = ?").param(bookId).update();

        // Define two tasks, one for each user that are trying to rent the same book.
        Callable<Integer> task1 = new DelegatingSecurityContextCallable<>(() -> {
            String rentRequestJson = TestJsonUtils.readJsonFile("rentOrReturnBookRequest.json").replace("1", String.valueOf(bookId));
            return mockMvc.perform(post("/api/users/{userId}/rent", user1Id)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(rentRequestJson))
                    .andReturn().getResponse().getStatus();
        });

        // Define two tasks, one for each user that are trying to rent the same book.
        Callable<Integer> task2 = new DelegatingSecurityContextCallable<>(() -> {
            String rentRequestJson = TestJsonUtils.readJsonFile("rentOrReturnBookRequest.json").replace("1", String.valueOf(bookId));
            return mockMvc.perform(post("/api/users/{userId}/rent", user2Id)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(rentRequestJson))
                    .andReturn().getResponse().getStatus();
        });

        // Invoke both tasks concurrently using the helper
//        List<Integer> statusCodes = concurrentTestHelperLatch.runParallel(List.of(task1, task2), 2);
        List<Integer> statusCodes = concurrentTestHelperBarrier.runParallel(List.of(task1, task2), 2);


        // Assert that one request succeeded (204) and the other - failed (409), but without guaranty which task did the job first; we check the set content but not the codes' order
        assertThat(statusCodes).containsExactlyInAnyOrder(204, 409);

        // Verify final state in the database
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcClient, BOOKINGS_TABLE, "book_id = " + bookId)).isEqualTo(1);
        assertThat(jdbcClient.sql("SELECT available FROM " + BOOKS_TABLE + " WHERE id = ?").param(bookId).query(Integer.class).single()).isEqualTo(0);
    }


    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    // Important for concurrency tests due @Transactional also in Service class that is used here in test
    @WithMockUser(roles = "ADMIN")
    void rentBookConcurrentAccessTwoUsersTwoBooksAvailableShouldBothSucceed() throws Exception {
        long user1Id = testDataHelper.idOfUser(TestFixtures.USER_RENT_EMAIL);
        long user2Id = testDataHelper.idOfUser(TestFixtures.USER_2_EMAIL);
        long bookId = testDataHelper.idOfBook(TestFixtures.BOOK_RENTABLE_TITLE);

        // Setup: ensure the book is available (2 available) and not booked by anyone.

        jdbcClient.sql("DELETE FROM " + BOOKINGS_TABLE + " WHERE book_id = ?").param(bookId).update();
        jdbcClient.sql("UPDATE " + BOOKS_TABLE + " SET available = 2 WHERE id = ?").param(bookId).update();

        // Define two tasks, one for each user that are trying to rent the same book.
        Callable<Integer> task1 = new DelegatingSecurityContextCallable<>(() -> {
            String rentRequestJson = TestJsonUtils.readJsonFile("rentOrReturnBookRequest.json").replace("1", String.valueOf(bookId));
            return mockMvc.perform(post("/api/users/{userId}/rent", user1Id)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(rentRequestJson))
                    .andReturn().getResponse().getStatus();
        });

        // Define two tasks, one for each user that are trying to rent the same book.
        Callable<Integer> task2 = new DelegatingSecurityContextCallable<>(() -> {
            String rentRequestJson = TestJsonUtils.readJsonFile("rentOrReturnBookRequest.json").replace("1", String.valueOf(bookId));
            return mockMvc.perform(post("/api/users/{userId}/rent", user2Id)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(rentRequestJson))
                    .andReturn().getResponse().getStatus();
        });

        // Invoke both tasks concurrently using the helper
//        List<Integer> statusCodes = concurrentTestHelperLatch.runParallel(List.of(task1, task2), 2);
        List<Integer> statusCodes = concurrentTestHelperBarrier.runParallel(List.of(task1, task2), 2);


        // Assert that both requests succeeded (204)
        assertThat(statusCodes).containsOnly(204);

        // Verify final state in the database
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcClient, BOOKINGS_TABLE, "book_id = " + bookId)).isEqualTo(2);
        assertThat(jdbcClient.sql("SELECT available FROM " + BOOKS_TABLE + " WHERE id = ?").param(bookId).query(Integer.class).single()).isEqualTo(0);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    // Important for concurrency tests due @Transactional also in Service class that is used here in test
    @WithMockUser(roles = "ADMIN")
    void rentBookConcurrentAccessTenUsersFiveBooksAvailableShouldFiveSucceedAndFiveFail() throws Exception {

        long bookId = testDataHelper.idOfBook(TestFixtures.BOOK_RENTABLE_TITLE);

        // Setup: ensure the book is available (available = 5) and not booked by anyone (for clear comparing).

        jdbcClient.sql("DELETE FROM " + BOOKINGS_TABLE + " WHERE book_id = ?").param(bookId).update();
        jdbcClient.sql("UPDATE " + BOOKS_TABLE + " SET available = 5 WHERE id = ?").param(bookId).update();

        int booksAvailable = jdbcClient.sql("SELECT available FROM " + BOOKS_TABLE + " WHERE id = ?").param(bookId).query(Integer.class).single();

//       IDs of 10 users that are going to rent the same book simultaneously.
        List<Long> userIds = getConcurrentTestUserIds();

        List<Callable<Integer>> tasks = new ArrayList<>();
        String rentRequestJson = TestJsonUtils.readJsonFile("rentOrReturnBookRequest.json").replace("1", String.valueOf(bookId));

        // Define 10 tasks, one for each user that are trying to rent the same book.
        for (Long userId : userIds) {
            tasks.add(new DelegatingSecurityContextCallable<>(() ->
                    mockMvc.perform(post("/api/users/{userId}/rent", userId)
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(rentRequestJson))
                            .andReturn().getResponse().getStatus()));
        }

        // Invoke ten tasks concurrently using the helper
//        List<Integer> statusCodes = concurrentTestHelperLatch.runParallel(tasks, 10);
        List<Integer> statusCodes = concurrentTestHelperBarrier.runParallel(tasks, 10);


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
    }
}
