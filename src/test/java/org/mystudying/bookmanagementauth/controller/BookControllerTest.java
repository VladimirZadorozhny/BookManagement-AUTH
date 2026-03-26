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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("/insertTestRecords.sql")
public class BookControllerTest extends AbstractSecurityIntegrationTest {

    private static final String BOOKS_TABLE = "books";

    private final EntityManager entityManager;

    public BookControllerTest(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Test
    void getAllBooksReturnsAllBooks() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/books")
                        .param("page", "0")
                        .param("size", String.valueOf(Integer.MAX_VALUE))
                        .param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(JdbcTestUtils.countRowsInTable(jdbcClient, BOOKS_TABLE)))
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        List<String> titles = JsonPath.parse(jsonResponse).read("$.content[*].title");

        assertThat(titles)
                .isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER)
                .contains(TestFixtures.BOOK_1_TITLE, TestFixtures.BOOK_2_TITLE, TestFixtures.BOOK_DELETE_TITLE, TestFixtures.BOOK_RENTABLE_TITLE);
    }

    @Test
    void getAllBooksWithAvailableParamReturnsAvailableBooks() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/books").queryParam("available", "true")
                        .param("page", "0")
                        .param("size", String.valueOf(Integer.MAX_VALUE)))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        List<String> titles = JsonPath.parse(jsonResponse).read("$.content[*].title");

        assertThat(titles).contains(TestFixtures.BOOK_1_TITLE, TestFixtures.BOOK_DELETE_TITLE, TestFixtures.BOOK_RENTABLE_TITLE);
        assertThat(titles).doesNotContain(TestFixtures.BOOK_2_TITLE);

        List<Integer> available = JsonPath.parse(jsonResponse).read("$.content[*].available");
        assertThat(available).allSatisfy(amount -> assertThat(amount).isPositive());
    }

    @Test
    void getAllBooksWithUnavailableParamReturnsUnavailableBooks() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/books").queryParam("available", "false")
                        .param("page", "0")
                        .param("size", String.valueOf(Integer.MAX_VALUE)))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        List<String> titles = JsonPath.parse(jsonResponse).read("$.content[*].title");

        assertThat(titles).contains(TestFixtures.BOOK_2_TITLE);
        assertThat(titles).doesNotContain(TestFixtures.BOOK_1_TITLE, TestFixtures.BOOK_DELETE_TITLE, TestFixtures.BOOK_RENTABLE_TITLE);

        List<Integer> available = JsonPath.parse(jsonResponse).read("$.content[*].available");
        assertThat(available).allSatisfy(amount -> assertThat(amount).isZero());
    }

    @Test
    void getAllBooksWithYearParamReturnsBooksByYear() throws Exception {
        mockMvc.perform(get("/api/books").queryParam("year", "2001")
                        .param("page", "0")
                        .param("size", String.valueOf(Integer.MAX_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].year").value(everyItem(is(2001))))
                .andExpect(jsonPath("$.totalElements").value(JdbcTestUtils.countRowsInTableWhere(
                        jdbcClient, BOOKS_TABLE, "year = 2001"
                )));
    }

    @Test
    void getAllBooksReturnsBooksByAuthorName() throws Exception {
        String authorName = TestFixtures.AUTHOR_1_NAME;

        MvcResult result = mockMvc.perform(get("/api/books").queryParam("authorName", authorName)
                        .param("page", "0")
                        .param("size", String.valueOf(Integer.MAX_VALUE)))
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
        List<String> titles = JsonPath.parse(jsonResponse).read("$.content[*].title");

        assertThat(titles).hasSize((int) expectedDbCount);
        assertThat(titles).contains(TestFixtures.BOOK_1_TITLE, TestFixtures.BOOK_RENTABLE_TITLE);
    }

    @Test
    void getBookByIdReturnsCorrectBook() throws Exception {
        long id = testDataHelper.idOfBook(TestFixtures.BOOK_1_TITLE);
        mockMvc.perform(get("/api/books/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value(TestFixtures.BOOK_1_TITLE));
    }

    @Test
    void getBookByIdReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/api/books/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book not found. Id: " + Long.MAX_VALUE));
    }

    @Test
    void getBookByTitleReturnsCorrectBook() throws Exception {
        mockMvc.perform(get("/api/books/title/{title}", TestFixtures.BOOK_1_TITLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(TestFixtures.BOOK_1_TITLE));
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
        String newBookJson = TestJsonUtils.readJsonFile("correctBook.json");

        MvcResult result = mockMvc.perform(post("/api/books")
                        .with(csrf())
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
        String newBookJson = TestJsonUtils.readJsonFile("correctBook.json").replace("\"genreIds\": [1]",
                "\"genreIds\": [" + Long.MAX_VALUE + "]");

        mockMvc.perform(post("/api/books")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newBookJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Genre not found with id: " + Long.MAX_VALUE));

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
        String invalidBookJson = TestJsonUtils.readJsonFile(fileName);
        mockMvc.perform(post("/api/books")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBookJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateBookReturnsUpdatedBook() throws Exception {
        long id = testDataHelper.idOfBook(TestFixtures.BOOK_1_TITLE);
        String updatedBookJson = TestJsonUtils.readJsonFile("updatedBook.json");

        mockMvc.perform(put("/api/books/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedBookJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Updated Book Title"))
                .andExpect(jsonPath("$.year").value(2010));

        entityManager.flush();
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcClient, BOOKS_TABLE, "id = " + id + " AND title = 'Updated Book Title'")).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void updateBookMetaDataConcurrentAccessOneSucceedsOneFails() throws Exception {
        long bookId = testDataHelper.idOfBook(TestFixtures.BOOK_1_TITLE);

        MvcResult resultInitialBook = mockMvc.perform(get("/api/books/{id}", bookId))
                .andExpect(status().isOk())
                .andReturn();
        String jsonResponse = resultInitialBook.getResponse().getContentAsString();
        String initialTitle = JsonPath.read(jsonResponse, "$.title");

        Callable<Integer> task1 = new DelegatingSecurityContextCallable<>(() -> {
            String updateRequestJson = TestJsonUtils.readJsonFile("updatedBook.json").replace("\"title\": \"Updated Book Title\"",
                    "\"title\": \"New Book Title Admin1\"");
            return mockMvc.perform(put("/api/books/{id}", bookId)
                            .with(csrf())
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateRequestJson))
                    .andReturn().getResponse().getStatus();
        });

        Callable<Integer> task2 = new DelegatingSecurityContextCallable<>(() -> {
            String updateRequestJson = TestJsonUtils.readJsonFile("updatedBook.json").replace("\"title\": \"Updated Book Title\"",
                    "\"title\": \"New Book Title Admin2\"");
            return mockMvc.perform(put("/api/books/{id}", bookId)
                            .with(csrf())
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateRequestJson))
                    .andReturn().getResponse().getStatus();
        });

        try {
            List<Integer> statusCodes = concurrentTestHelperBarrier.runParallel(List.of(task1, task2), 2);

            assertThat(statusCodes).containsExactlyInAnyOrder(200, 409);

            assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcClient, BOOKS_TABLE, "id = " + bookId + " AND title = '" + initialTitle + "'")).isEqualTo(0);
            assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcClient, BOOKS_TABLE, "id = " + bookId +
                    " AND (title = 'New Book Title Admin1' OR title = 'New Book Title Admin2')")).isEqualTo(1);

        } finally {
            testDataCleanup.cleanupAllTestSqlData();
        }
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void updateBookReturnsNotFoundForUnknownId() throws Exception {
        String updatedBookJson = TestJsonUtils.readJsonFile("updatedBook.json");
        mockMvc.perform(put("/api/books/{id}", Long.MAX_VALUE)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedBookJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book not found. Id: " + Long.MAX_VALUE));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteBookReturnsNoContent() throws Exception {
        long id = testDataHelper.idOfBook(TestFixtures.BOOK_DELETE_TITLE);
        long initialRowCount = JdbcTestUtils.countRowsInTable(jdbcClient, BOOKS_TABLE);

        mockMvc.perform(delete("/api/books/{id}", id)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/books/{id}", id))
                .andExpect(status().isNotFound());

        entityManager.flush();
        assertThat(JdbcTestUtils.countRowsInTable(jdbcClient, BOOKS_TABLE)).isEqualTo(initialRowCount - 1);
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcClient, BOOKS_TABLE, "id = " + id)).isEqualTo(0);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteBookReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(delete("/api/books/{id}", Long.MAX_VALUE)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book not found. Id: " + Long.MAX_VALUE));
    }

}
