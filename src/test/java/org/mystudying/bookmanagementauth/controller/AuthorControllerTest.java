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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("/insertTestRecords.sql")
public class AuthorControllerTest extends AbstractSecurityIntegrationTest {

    private static final String AUTHORS_TABLE = "authors";
    private static final String BOOKS_TABLE = "books";

    private final EntityManager entityManager;

    public AuthorControllerTest(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Test
    void getAllAuthorsReturnsAllAuthors() throws Exception {
        var amountAuthors = JdbcTestUtils.countRowsInTable(jdbcClient, AUTHORS_TABLE);
        MvcResult result = mockMvc.perform(get("/api/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(amountAuthors))
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        List<String> names = JsonPath.parse(jsonResponse).read("$[*].name");

        assertThat(names)
                .isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER)
                .contains(TestFixtures.AUTHOR_1_NAME, TestFixtures.AUTHOR_2_NAME, TestFixtures.AUTHOR_DELETE_NAME);
    }

    @Test
    void getAuthorByIdReturnsCorrectAuthor() throws Exception {
        long id = testDataHelper.idOfAuthor(TestFixtures.AUTHOR_1_NAME);
        mockMvc.perform(get("/api/authors/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value(TestFixtures.AUTHOR_1_NAME))
                .andExpect(jsonPath("$.birthdate").value(LocalDate.of(1901, 1, 1).toString()));
    }

    @Test
    void getAuthorByIdReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/api/authors/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Author not found. Id: " + Long.MAX_VALUE));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAuthorReturnsCreatedAuthor() throws Exception {
        long initialRowCount = JdbcTestUtils.countRowsInTable(jdbcClient, AUTHORS_TABLE);
        String newAuthorJson = TestJsonUtils.readJsonFile("correctAuthor.json");

        mockMvc.perform(post("/api/authors")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newAuthorJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("New Author From Test"))
                .andExpect(jsonPath("$.birthdate").value("1980-01-01"));

        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcClient, AUTHORS_TABLE, "name = 'New Author From Test'")).isEqualTo(1);
        assertThat(JdbcTestUtils.countRowsInTable(jdbcClient, AUTHORS_TABLE)).isEqualTo(initialRowCount + 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AuthorWithEmptyName.json", "AuthorWithoutName.json", "AuthorWithoutBirthdate.json", "AuthorWithFutureBirthdate.json"})
    @WithMockUser(roles = "ADMIN")
    void createAuthorReturnsBadRequestForInvalidData(String fileName) throws Exception {
        String invalidAuthorJson = TestJsonUtils.readJsonFile(fileName);
        mockMvc.perform(post("/api/authors")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidAuthorJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAuthorReturnsUpdatedAuthor() throws Exception {
        long id = testDataHelper.idOfAuthor(TestFixtures.AUTHOR_1_NAME);
        String updatedAuthorJson = TestJsonUtils.readJsonFile("updatedAuthor.json");

        mockMvc.perform(put("/api/authors/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedAuthorJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Updated Author Name"))
                .andExpect(jsonPath("$.birthdate").value("1970-05-10"));

        mockMvc.perform(get("/api/authors/name/{name}", "Updated Author Name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        entityManager.flush();
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcClient, AUTHORS_TABLE, "id = " + id + " AND name = 'Updated Author Name'")).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAuthorReturnsNotFoundForUnknownId() throws Exception {
        String updatedAuthorJson = TestJsonUtils.readJsonFile("updatedAuthor.json");
        mockMvc.perform(put("/api/authors/{id}", Long.MAX_VALUE)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedAuthorJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Author not found. Id: " + Long.MAX_VALUE));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAuthorReturnsNoContentIfAuthorHasNoBooks() throws Exception {
        long authorIdToDelete = testDataHelper.idOfAuthor(TestFixtures.AUTHOR_DELETE_NAME);

        // First delete from book_genres (join table)
        jdbcClient.sql("DELETE FROM book_genres WHERE book_id IN (SELECT id FROM books WHERE author_id = ?)")
                .param(authorIdToDelete)
                .update();

        // Then delete from books
        jdbcClient.sql("DELETE FROM " + BOOKS_TABLE + " WHERE author_id = ?")
                .param(authorIdToDelete)
                .update();

        long initialRowCount = JdbcTestUtils.countRowsInTable(jdbcClient, AUTHORS_TABLE);

        mockMvc.perform(delete("/api/authors/{id}", authorIdToDelete)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/authors/{id}", authorIdToDelete))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/authors"))
                .andExpect(jsonPath("$.length()").value(initialRowCount - 1));

        entityManager.flush();
        assertThat(JdbcTestUtils.countRowsInTable(jdbcClient, AUTHORS_TABLE)).isEqualTo(initialRowCount - 1);
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcClient, AUTHORS_TABLE, "id = " + authorIdToDelete)).isEqualTo(0);
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAuthorReturnsConflictIfAuthorHasBooks() throws Exception {
        long authorIdToDelete = testDataHelper.idOfAuthor(TestFixtures.AUTHOR_DELETE_NAME);
        long initialRowCount = JdbcTestUtils.countRowsInTable(jdbcClient, AUTHORS_TABLE);

        mockMvc.perform(delete("/api/authors/{id}", authorIdToDelete)
                        .with(csrf()))
                .andExpect(status().isConflict());

        assertThat(JdbcTestUtils.countRowsInTable(jdbcClient, AUTHORS_TABLE)).isEqualTo(initialRowCount);
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcClient, AUTHORS_TABLE, "id = " + authorIdToDelete)).isOne();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAuthorReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(delete("/api/authors/{id}", Long.MAX_VALUE)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Author not found. Id: " + Long.MAX_VALUE));
    }

}
