package org.mystudying.bookmanagementauth.controller;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mystudying.bookmanagementauth.support.AbstractSecurityIntegrationTest;
import org.mystudying.bookmanagementauth.support.TestJsonUtils;
import org.mystudying.bookmanagementauth.support.db.TestFixtures;
import org.springframework.http.MediaType;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("/insertTestRecords.sql")
public class UserControllerTest extends AbstractSecurityIntegrationTest {

    private static final String USERS_TABLE = "users";
    private static final String BOOKINGS_TABLE = "bookings";
    private static final String BOOKS_TABLE = "books";

    private final EntityManager entityManager;

    public UserControllerTest(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsersReturnsAllUsers() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/users")
                        .param("page", "0")
                        .param("size", String.valueOf(Integer.MAX_VALUE))
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(JdbcTestUtils.countRowsInTable(jdbcClient, USERS_TABLE)))
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        List<String> names = JsonPath.parse(jsonResponse).read("$.content[*].name");

        assertThat(names)
                .isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER)
                .contains(TestFixtures.USER_1_NAME, TestFixtures.USER_2_NAME, TestFixtures.USER_DELETE_NAME, TestFixtures.USER_RENT_NAME);
    }

    @Test
    @WithUserDetails(value = TestFixtures.USER_1_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getUserByIdReturnsCorrectUser() throws Exception {
        long id = testDataHelper.idOfUser(TestFixtures.USER_1_EMAIL);
        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value(TestFixtures.USER_1_NAME));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserByIdReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/api/users/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found. Id: " + Long.MAX_VALUE));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchUserByNameReturnsCorrectUser() throws Exception {
        mockMvc.perform(get("/api/users/search").queryParam("by", TestFixtures.USER_1_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(TestFixtures.USER_1_NAME))
                .andExpect(jsonPath("$.id").value(testDataHelper.idOfUser(TestFixtures.USER_1_EMAIL)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchUserByEmailReturnsCorrectUser() throws Exception {
        mockMvc.perform(get("/api/users/search").queryParam("by", TestFixtures.USER_2_EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TestFixtures.USER_2_EMAIL))
                .andExpect(jsonPath("$.id").value(testDataHelper.idOfUser(TestFixtures.USER_2_EMAIL)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchUserReturnsNotFoundForUnknownUser() throws Exception {
        mockMvc.perform(get("/api/users/search").queryParam("by", "unknown@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found. Name or email: unknown@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUserReturnsCreatedUser() throws Exception {
        long initialRowCount = JdbcTestUtils.countRowsInTable(jdbcClient, USERS_TABLE);
        String newUserJson = TestJsonUtils.readJsonFile("correctUser.json");

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUserJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("New User From Test"))
                .andExpect(jsonPath("$.email").value("new.test@example.com"));

        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcClient, USERS_TABLE, "email = 'new.test@example.com'")).isEqualTo(1);
        assertThat(JdbcTestUtils.countRowsInTable(jdbcClient, USERS_TABLE)).isEqualTo(initialRowCount + 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "UserWithEmptyName.json",
            "UserWithoutName.json",
            "UserWithEmptyEmail.json",
            "UserWithoutEmail.json",
            "UserWithInvalidEmail.json"
    })
    @WithMockUser(roles = "ADMIN")
    void createUserReturnsBadRequestForInvalidData(String fileName) throws Exception {
        String invalidUserJson = TestJsonUtils.readJsonFile(fileName);
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidUserJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails(value = TestFixtures.USER_1_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void updateUserReturnsUpdatedUser() throws Exception {
        long id = testDataHelper.idOfUser(TestFixtures.USER_1_EMAIL);
        String updatedUserJson = TestJsonUtils.readJsonFile("updatedUser.json");

        mockMvc.perform(put("/api/users/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedUserJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Updated User Name"))
                .andExpect(jsonPath("$.email").value("updated.user@example.com"));

        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcClient, USERS_TABLE, "id = " + id + " AND email = 'updated.user@example.com'")).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUserReturnsNoContent() throws Exception {
        long id = testDataHelper.idOfUser(TestFixtures.USER_DELETE_EMAIL);
        long initialRowCount = JdbcTestUtils.countRowsInTable(jdbcClient, USERS_TABLE);

        mockMvc.perform(delete("/api/users/{id}", id)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isNotFound());

        entityManager.flush();
        assertThat(JdbcTestUtils.countRowsInTable(jdbcClient, USERS_TABLE)).isEqualTo(initialRowCount - 1);
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcClient, USERS_TABLE, "id = " + id)).isEqualTo(0);
    }

    @Test
    @WithUserDetails(value = TestFixtures.USER_RENT_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void rentBookReturnsNoContent() throws Exception {
        long rentUserId = testDataHelper.idOfUser(TestFixtures.USER_RENT_EMAIL);
        long rentableBookId = testDataHelper.idOfBook(TestFixtures.BOOK_RENTABLE_TITLE);
        int initialAvailable = jdbcClient.sql("SELECT available FROM books WHERE id = ?").param(rentableBookId).query(Integer.class).single();

        String rentRequestJson = String.format("{\"bookId\": %d}", rentableBookId);

        mockMvc.perform(post("/api/users/{userId}/rent", rentUserId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rentRequestJson))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/{id}/bookings", rentUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.bookId == " + rentableBookId + ")]").isNotEmpty());

        mockMvc.perform(get("/api/books/{id}", rentableBookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(initialAvailable - 1));
    }

    @Test
    @WithUserDetails(value = TestFixtures.USER_RENT_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void returnBookReturnsNoContent() throws Exception {
        long rentUserId = testDataHelper.idOfUser(TestFixtures.USER_RENT_EMAIL);
        long rentableBookId = testDataHelper.idOfBook(TestFixtures.BOOK_RENTABLE_TITLE);

        // 1. Rent first
        mockMvc.perform(post("/api/users/{userId}/rent", rentUserId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"bookId\": %d}", rentableBookId)))
                .andExpect(status().isNoContent());

        int availableAfterRent = jdbcClient.sql("SELECT available FROM books WHERE id = ?").param(rentableBookId).query(Integer.class).single();

        // 2. Return
        mockMvc.perform(post("/api/users/{userId}/return", rentUserId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"bookId\": %d}", rentableBookId)))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/books/{id}", rentableBookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(availableAfterRent + 1));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @WithMockUser(roles = "ADMIN")
    void rentBook_concurrentAccess_oneSucceedsOneFails() throws Exception {
        long user1Id = testDataHelper.idOfUser(TestFixtures.USER_RENT_EMAIL);
        long user2Id = testDataHelper.idOfUser(TestFixtures.USER_2_EMAIL);
        long bookId = testDataHelper.idOfBook(TestFixtures.BOOK_RENTABLE_TITLE);

        jdbcClient.sql("DELETE FROM " + BOOKINGS_TABLE + " WHERE book_id = ?").param(bookId).update();
        jdbcClient.sql("UPDATE " + BOOKS_TABLE + " SET available = 1 WHERE id = ?").param(bookId).update();

        Callable<Integer> task1 = new DelegatingSecurityContextCallable<>(() ->
                mockMvc.perform(post("/api/users/{userId}/rent", user1Id)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.format("{\"bookId\": %d}", bookId)))
                        .andReturn().getResponse().getStatus()
        );

        Callable<Integer> task2 = new DelegatingSecurityContextCallable<>(() ->
                mockMvc.perform(post("/api/users/{userId}/rent", user2Id)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.format("{\"bookId\": %d}", bookId)))
                        .andReturn().getResponse().getStatus()
        );

        try {
//            List<Integer> statusCodes = concurrentTestHelperLatch.runParallel(List.of(task1, task2), 2);
            List<Integer> statusCodes = concurrentTestHelperBarrier.runParallel(List.of(task1, task2), 2);
            assertThat(statusCodes).containsExactlyInAnyOrder(204, 409);
        } finally {
            testDataCleanup.cleanupAllTestSqlData();
        }
    }

}
