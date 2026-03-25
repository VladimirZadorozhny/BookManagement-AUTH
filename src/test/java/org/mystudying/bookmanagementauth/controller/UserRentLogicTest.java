package org.mystudying.bookmanagementauth.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mystudying.bookmanagementauth.dto.booking.BookingResponseDto;
import org.mystudying.bookmanagementauth.support.AbstractSecurityIntegrationTest;
import org.mystudying.bookmanagementauth.support.db.TestFixtures;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql({"/insertTestRecords.sql", "/insertUserLogicTestRecords.sql"})
public class UserRentLogicTest extends AbstractSecurityIntegrationTest {

    private final EntityManager entityManager;

    public UserRentLogicTest(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Test
    @WithUserDetails(value = TestFixtures.LOGIC_USER_CLEAN, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void rentBookSuccessForCleanUser() throws Exception {
        long userId = testDataHelper.idOfUser(TestFixtures.LOGIC_USER_CLEAN);
        long bookId = testDataHelper.idOfBook(TestFixtures.LOGIC_BOOK_A);

        long activeBookings = JdbcTestUtils.countRowsInTableWhere(jdbcClient, "bookings",
                "returned_at IS NULL AND user_id = " + userId + " AND book_id = " + bookId);

        MvcResult resultBook = mockMvc.perform(get("/api/books/{id}", bookId))
                .andExpect(status().isOk())
                .andReturn();
        String jsonResponseBook = resultBook.getResponse().getContentAsString();
        int initialAvailable = JsonPath.read(jsonResponseBook, "$.available");

        MvcResult resultUserBookings = mockMvc.perform(get("/api/users/{id}/bookings", userId))
                .andExpect(status().isOk())
                .andReturn();
        String jsonResponseBooking = resultUserBookings.getResponse().getContentAsString();
        int initialBookings = JsonPath.read(jsonResponseBooking, "$.length()");
        List<Integer> booksByUser = JsonPath.parse(jsonResponseBooking).read("$[*].bookId");
        assertThat(booksByUser).doesNotContain((int) bookId);

        String requestJson = String.format("{\"bookId\": %d}", bookId);

        mockMvc.perform(post("/api/users/{userId}/rent", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNoContent());

//      Clear persistence context to force entity reload from DB, because we got stale entity from persistence context after atomic update
        entityManager.clear();
        mockMvc.perform(get("/api/books/{id}", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(initialAvailable - 1));

        mockMvc.perform(get("/api/users/{id}/bookings", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(initialBookings + 1))
                .andExpect(jsonPath("$[?(@.bookId == " + bookId + ")]").isNotEmpty())
                .andExpect(jsonPath("$[*].bookId").value(hasItem((int) bookId)));

        // extra test to see the changes in DB
        entityManager.flush();
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcClient, "bookings",
                "returned_at IS NULL AND user_id = " + userId + " AND book_id = " + bookId))
                .isEqualTo(activeBookings + 1);
    }

    @Test
    @WithUserDetails(value = TestFixtures.LOGIC_USER_OVERDUE, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void rentBookFailsWhenUserHasOverdueBooks() throws Exception {
        long userId = testDataHelper.idOfUser(TestFixtures.LOGIC_USER_OVERDUE);
        long bookId = testDataHelper.idOfBook(TestFixtures.LOGIC_BOOK_A);

        String requestJson = String.format("{\"bookId\": %d}", bookId);

        mockMvc.perform(post("/api/users/{userId}/rent", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("User with id " + userId + " has overdue books!")))
                .andExpect(jsonPath("$.code").value("USER_HAS_OVERDUE_BOOKS"));
    }

    @Test
    @WithUserDetails(value = TestFixtures.LOGIC_USER_FINE, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void rentBookFailsWhenUserHasUnpaidFines() throws Exception {
        long userId = testDataHelper.idOfUser(TestFixtures.LOGIC_USER_FINE);
        long bookId = testDataHelper.idOfBook(TestFixtures.LOGIC_BOOK_A);

        String requestJson = String.format("{\"bookId\": %d}", bookId);

        mockMvc.perform(post("/api/users/{userId}/rent", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("User with id " + userId + " has unpaid fines!")))
                .andExpect(jsonPath("$.code").value("USER_HAS_UNPAID_FINES"));
    }

    @Test
    @WithUserDetails(value = TestFixtures.LOGIC_USER_OVERDUE, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void returnBookCalculatesFineWhenOverdue() throws Exception {
        long userId = testDataHelper.idOfUser(TestFixtures.LOGIC_USER_OVERDUE);
        long bookId = testDataHelper.idOfBook(TestFixtures.LOGIC_BOOK_OVERDUE);
        long bookingId = testDataHelper.idOfBooking(userId, bookId);

        String requestJson = String.format("{\"bookId\": %d}", bookId);

        mockMvc.perform(post("/api/users/{userId}/return", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNoContent());

        entityManager.flush();

        // Verify fine is calculated (should be 6.00 based on DATE_SUB(CURRENT_DATE, INTERVAL 6 DAY) as due_at)
        BigDecimal fine = jdbcClient.sql("SELECT fine FROM bookings WHERE user_id = ? AND book_id = ? AND id = ?")
                .param(userId).param(bookId).param(bookingId).query(BigDecimal.class).single();

        assertThat(fine).isGreaterThan(BigDecimal.ZERO);
        assertThat(fine.stripTrailingZeros()).isGreaterThanOrEqualTo(new BigDecimal("6"));
    }

    @Test
    @WithUserDetails(value = TestFixtures.LOGIC_USER_OVERDUE, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void rentBookSuccessOnlyAfterReturnOverdueAndPayFines() throws Exception {
        long userId = testDataHelper.idOfUser(TestFixtures.LOGIC_USER_OVERDUE);
        long bookIdOverdue = testDataHelper.idOfBook(TestFixtures.LOGIC_BOOK_OVERDUE);
        long bookIdClean = testDataHelper.idOfBook(TestFixtures.LOGIC_BOOK_A);

        String requestJsonCleanBook = String.format("{\"bookId\": %d}", bookIdClean);

//      try to rent a book with overdue book, failed with conflict due overdue book
        mockMvc.perform(post("/api/users/{userId}/rent", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonCleanBook))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("User with id " + userId + " has overdue books!")));

//      return overdue book
        String requestJsonOverdueBook = String.format("{\"bookId\": %d}", bookIdOverdue);
        mockMvc.perform(post("/api/users/{userId}/return", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonOverdueBook))
                .andExpect(status().isNoContent());

//        check the fines after returning overdue book
        MvcResult resultUserBookings = mockMvc.perform(get("/api/users/{id}/bookings", userId))
                .andExpect(status().isOk())
                .andReturn();
        String jsonResponseBooking = resultUserBookings.getResponse().getContentAsString();
        List<BookingResponseDto> bookingsByUser = objectMapper.readValue(jsonResponseBooking, new TypeReference<List<BookingResponseDto>>() {
        });

        assertThat(bookingsByUser).anyMatch(booking -> booking.bookId() == bookIdOverdue && !booking.finePaid());

//        check the amount of unpaid fine, it must be $6
        BigDecimal fine = bookingsByUser.stream()
                .filter(el -> el.bookId() == bookIdOverdue)
                .map(BookingResponseDto::fine)
                .findFirst()
                .orElseThrow();
        assertThat(fine).isGreaterThanOrEqualTo(new BigDecimal("6.00"));

//      try to rent a book after returning overdue book but before paying fines, failed with conflict due unpaid fines
        mockMvc.perform(post("/api/users/{userId}/rent", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonCleanBook))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("User with id " + userId + " has unpaid fines!")));

//        pay fines of booking (with overdue returned book)
        long bookingId = jdbcClient.sql("SELECT id FROM bookings WHERE user_id = ? AND book_id = ?")
                .param(userId).param(bookIdOverdue).query(Long.class).single();

        mockMvc.perform(post("/api/users/{userId}/bookings/{bookingId}/pay", userId, bookingId)
                .with(csrf()));
//        check that we do not have unpaid fines by this booking (with overdue book)
        resultUserBookings = mockMvc.perform(get("/api/users/{id}/bookings", userId))
                .andExpect(status().isOk())
                .andReturn();
        jsonResponseBooking = resultUserBookings.getResponse().getContentAsString();
        bookingsByUser = objectMapper.readValue(jsonResponseBooking, new TypeReference<List<BookingResponseDto>>() {
        });

        assertThat(bookingsByUser).noneMatch(booking -> booking.id() == bookingId && !booking.finePaid());

//      after insuring that we returned overdue book and paid the fines, try to rent a new book, must succeed
        mockMvc.perform(post("/api/users/{userId}/rent", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonCleanBook))
                .andExpect(status().isNoContent());
    }
}
