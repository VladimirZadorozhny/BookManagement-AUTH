package org.mystudying.bookmanagementauth.controller;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Sql("/insertTestRecords.sql")
public class BookControllerTest {

    private static final String BOOKS_TABLE = "books";
    private static final String AUTHORS_TABLE = "authors";
    private static final String USERS_TABLE = "users";
    private static final String BOOKINGS_TABLE = "bookings";


    private final MockMvc mockMvc;
    private final JdbcClient jdbcClient;
    private final EntityManager entityManager;
    private final TransactionTemplate txTemplate;

    public BookControllerTest(MockMvc mockMvc, JdbcClient jdbcClient, EntityManager entityManager, TransactionTemplate txTemplate) {
        this.mockMvc = mockMvc;
        this.jdbcClient = jdbcClient;
        this.entityManager = entityManager;
        this.txTemplate = txTemplate;
    }

    private long idOfTestBook1() {
        return jdbcClient.sql("select id from books where title = 'Test Book 1'")
                .query(Long.class)
                .single();
    }

    private long idOfTestAuthor1() {
        return jdbcClient.sql("select id from authors where name = 'Test Author 1'")
                .query(Long.class)
                .single();
    }

    private long idOfAuthorForDeletion() {
        return jdbcClient.sql("select id from authors where name = 'Author For Deletion'")
                .query(Long.class)
                .single();
    }

    private long idOfBookForDeletion() {
        return jdbcClient.sql("SELECT id FROM books WHERE title = 'Book For Deletion'")
                .query(Long.class)
                .single();
    }

    private long idOfRentableBook() {
        return jdbcClient.sql("SELECT id FROM books WHERE title = 'Rentable Book'")
                .query(Long.class)
                .single();
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

    @Test
    void getAllBooksReturnsAllBooks() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(JdbcTestUtils.countRowsInTable(jdbcClient, BOOKS_TABLE)))
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        List<String> titles = JsonPath.parse(jsonResponse).read("$[*].title");

        assertThat(titles)
                .isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER) // Books are sorted by title in the repository
                .contains("Test Book 1", "Test Book 2", "Book For Deletion", "Rentable Book"); // Check specific content
    }

    @Test
    void getAllBooksReturnsAvailableBooks() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/books").queryParam("available", "true"))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        List<String> titles = JsonPath.parse(jsonResponse).read("$[*].title");

        // "Test Book 1" has 5 available, "Test Book 2" has 0 available, "Book For Deletion" has 1, "Rentable Book" has 1
        assertThat(titles).contains("Test Book 1", "Book For Deletion", "Rentable Book");
        assertThat(titles).doesNotContain("Test Book 2");

        List<Integer> available = JsonPath.parse(jsonResponse).read("$[*].available");
        assertThat(available)
                .allSatisfy(amount -> assertThat(amount).isPositive());
    }

    @Test
    void getAllBooksReturnsUnavailableBooks() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/books").queryParam("available", "false"))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        List<String> titles = JsonPath.parse(jsonResponse).read("$[*].title");

        assertThat(titles).contains("Test Book 2");
        assertThat(titles).doesNotContain("Test Book 1", "Book For Deletion", "Rentable Book");

        List<Integer> available = JsonPath.parse(jsonResponse).read("$[*].available");
        assertThat(available)
                .allSatisfy(amount -> assertThat(amount).isZero());
    }

    @Test
    void getAllBooksReturnsBooksByYear() throws Exception {
        mockMvc.perform(get("/api/books").queryParam("year", "2001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].year").value(everyItem(is(2001))))
                .andExpect(jsonPath("$.length()").value(JdbcTestUtils.countRowsInTableWhere(
                        jdbcClient, BOOKS_TABLE, "year = 2001"
                )));
    }

    @Test
    void getAllBooksReturnsBooksByAuthorName() throws Exception {
        String authorName = "Test Author 1";
        long authorId = idOfTestAuthor1();

        MvcResult result = mockMvc.perform(get("/api/books").queryParam("authorName", authorName))
                .andExpect(status().isOk())
                .andReturn();

        long expectedDbCount = jdbcClient.sql("""
                        SELECT count(b.id)
                        FROM books b
                        JOIN authors a ON b.author_id = a.id
                        WHERE a.name = ?
                        """)
                .param(authorName)
                .query(Long.class)
                .single();

        String jsonResponse = result.getResponse().getContentAsString();
        List<String> titles = JsonPath.parse(jsonResponse).read("$[*].title");

        assertThat(titles).hasSize((int) expectedDbCount);
        assertThat(titles).contains("Test Book 1", "Rentable Book");
    }

    @Test
    void getBookByIdReturnsCorrectBook() throws Exception {
        long id = idOfTestBook1();
        mockMvc.perform(get("/api/books/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Test Book 1"));
    }

    @Test
    void getBookByIdReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/api/books/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book not found. Id: " + Long.MAX_VALUE));
    }

    @Test
    void getBookByTitleReturnsCorrectBook() throws Exception {
        mockMvc.perform(get("/api/books/title/{title}", "Test Book 1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Book 1"));
    }

    @Test
    void getBookByTitleReturnsNotFoundForUnknownTitle() throws Exception {
        mockMvc.perform(get("/api/books/title/{title}", "Non Existent Title"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book not found. Title: Non Existent Title"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createBookReturnsCreatedBook() throws Exception {
        long initialRowCount = JdbcTestUtils.countRowsInTable(jdbcClient, BOOKS_TABLE);
        String newBookJson = readJsonFile("correctBook.json");

        MvcResult result = mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newBookJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("New Book From Test"))
                .andReturn();
        String jsonResponse = result.getResponse().getContentAsString();
        int newId = JsonPath.parse(jsonResponse).read("$.id");

        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcClient, BOOKS_TABLE, "title = 'New Book From Test' and id = " + newId)).isEqualTo(1);
        assertThat(JdbcTestUtils.countRowsInTable(jdbcClient, BOOKS_TABLE)).isEqualTo(initialRowCount + 1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createBookWithNotExistingGenreReturnsNotFoundGenre() throws Exception {
        long initialRowCount = JdbcTestUtils.countRowsInTable(jdbcClient, BOOKS_TABLE);
        String newBookJson = readJsonFile("correctBook.json").replace("\"genreIds\": [1]",
                "\"genreIds\": [" + Long.MAX_VALUE + "]");

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newBookJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Genre not found with id: " + Long.MAX_VALUE))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/books"));


        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcClient, BOOKS_TABLE, "title = 'New Book From Test'")).isEqualTo(0);
        assertThat(JdbcTestUtils.countRowsInTable(jdbcClient, BOOKS_TABLE)).isEqualTo(initialRowCount);
    }


    @ParameterizedTest
    @ValueSource(strings = {
            "BookWithEmptyTitle.json",
            "BookWithoutTitle.json",
            "BookWithFutureYear.json",
            "BookWithoutYear.json",
            "BookWithZeroAuthorId.json",
            "BookWithoutAuthorId.json",
            "BookWithNegativeAvailable.json",
            "BookWithoutAvailable.json",
            "BookWithNoGenres.json"
    })
    @WithMockUser(roles = "ADMIN")
    void createBookReturnsBadRequestForInvalidData(String fileName) throws Exception {
        String invalidBookJson = readJsonFile(fileName);
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBookJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateBookReturnsUpdatedBook() throws Exception {
        long id = idOfTestBook1();
        String updatedBookJson = readJsonFile("updatedBook.json");

        mockMvc.perform(put("/api/books/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedBookJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Updated Book Title"))
                .andExpect(jsonPath("$.year").value(2010));

        mockMvc.perform(get("/api/books/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Updated Book Title"))
                .andExpect(jsonPath("$.year").value(2010));

//        extra test to see the changes in DB
        entityManager.flush();
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcClient, BOOKS_TABLE, "id = " + id + " AND title = 'Updated Book Title'")).isEqualTo(1);
    }

    //    Scenario: 2 Admins load same book, both edit title, one saves and	second → OptimisticLockException (in random order)
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    // Important for concurrency tests due @Transactional also in Service class that is used here in test
//    @WithMockUser(roles = "ADMIN") - not necessary, direct injection in each request because of more strict rules on the URL-level. MockUser will not work with other Threads
    void updateBookMetaDataConcurrentAccessOneSucceedsOneFails() throws Exception {
        long bookId = idOfTestBook1();

        MvcResult resultInitialBook = mockMvc.perform(get("/api/books/{id}", bookId))
                .andExpect(status().isOk())
                .andReturn();
        String jsonResponse = resultInitialBook.getResponse().getContentAsString();
        String initialTitle = JsonPath.read(jsonResponse, "$.title");

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Define two tasks, one for each Admin that are trying to update the same book's metadata (title).
        Callable<Integer> task1 = new DelegatingSecurityContextCallable<>(() -> {
            String updateRequestJson = readJsonFile("updatedBook.json").replace("\"title\": \"Updated Book Title\"",
                    "\"title\": \"New Book Title Admin1\"");
            return mockMvc.perform(put("/api/books/{id}", bookId)
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateRequestJson))
                    .andReturn().getResponse().getStatus();
        });

        Callable<Integer> task2 = new DelegatingSecurityContextCallable<>(() -> {
            String updateRequestJson = readJsonFile("updatedBook.json").replace("\"title\": \"Updated Book Title\"",
                    "\"title\": \"New Book Title Admin2\"");
            return mockMvc.perform(put("/api/books/{id}", bookId)
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateRequestJson))
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

            // Assert that one request succeeded (200) and the other - failed (409), but without guaranty which task did the job first; we check the set content but not the codes' order
            assertThat(statusCodes).containsExactlyInAnyOrder(200, 409);

            // Verify final state in the database
            assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcClient, BOOKS_TABLE, "id = " + bookId + " AND title = '" + initialTitle + "'")).isEqualTo(0);
            assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcClient, BOOKS_TABLE, "id = " + bookId +
                    " AND (title = 'New Book Title Admin1' OR title = 'New Book Title Admin2')")).isEqualTo(1);


        } finally {
            executor.shutdown();
            txTemplate.execute(status -> {
                dbCleanup(idOfRentUser(), idOfTestUser2(), bookId);
                return null;
            });
        }
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void updateBookReturnsNotFoundForUnknownId() throws Exception {
        String updatedBookJson = readJsonFile("updatedBook.json");
        mockMvc.perform(put("/api/books/{id}", Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedBookJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book not found. Id: " + Long.MAX_VALUE));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteBookReturnsNoContent() throws Exception {
        long id = idOfBookForDeletion();
        long initialRowCount = JdbcTestUtils.countRowsInTable(jdbcClient, BOOKS_TABLE);

        mockMvc.perform(delete("/api/books/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/books/{id}", id))
                .andExpect(status().isNotFound());

//        extra test to see changes in DB
        entityManager.flush();
        assertThat(JdbcTestUtils.countRowsInTable(jdbcClient, BOOKS_TABLE)).isEqualTo(initialRowCount - 1);
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcClient, BOOKS_TABLE, "id = " + id)).isEqualTo(0);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteBookReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(delete("/api/books/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book not found. Id: " + Long.MAX_VALUE));
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
        jdbcClient.sql("DELETE FROM " + BOOKINGS_TABLE + " WHERE user_id IN (SELECT id FROM " + USERS_TABLE + " WHERE email IN ('delete@example.com', 'rent@example.com', 'test1@example.com', 'test2@example.com'))").update();

        // Clear book_genres
        jdbcClient.sql("DELETE FROM book_genres WHERE book_id IN (SELECT id FROM " + BOOKS_TABLE + " WHERE title IN ('Book For Deletion', 'Rentable Book', 'Test Book 1', 'Test Book 2')) OR book_id = ?")
                .param(bookId)
                .update();

        // Clear users_roles
        jdbcClient.sql("DELETE FROM users_roles WHERE user_id IN (SELECT id FROM " + USERS_TABLE + " WHERE email IN ('delete@example.com', 'rent@example.com', 'test1@example.com', 'test2@example.com'))").update();

        // Clear books
        jdbcClient.sql("DELETE FROM " + BOOKS_TABLE + " WHERE title IN ('Book For Deletion', 'Rentable Book', 'Test Book 1', 'Test Book 2') OR id = ?")
                .param(bookId)
                .update();

        // Clear authors, users, genres
        jdbcClient.sql("DELETE FROM " + AUTHORS_TABLE + " WHERE name IN ('Author For Deletion', 'Test Author 1', 'Test Author 2')").update();
        jdbcClient.sql("DELETE FROM " + USERS_TABLE + " WHERE email IN ('delete@example.com', 'rent@example.com', 'test1@example.com', 'test2@example.com')").update();
        jdbcClient.sql("DELETE FROM genres WHERE name IN ('Test Genre 1', 'Test Genre 2', 'Test Genre 3')").update();
    }


}
