package org.mystudying.bookmanagementauth.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.mystudying.bookmanagementauth.dto.CreateGenreRequestDto;
import org.mystudying.bookmanagementauth.support.AbstractSecurityIntegrationTest;
import org.mystudying.bookmanagementauth.support.db.TestFixtures;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("/insertTestRecords.sql")
public class GenreControllerTest extends AbstractSecurityIntegrationTest {

    private static final String GENRES_TABLE = "genres";
    private static final String BOOK_GENRES_TABLE = "book_genres";

    @Test
    void getAllGenresReturnsAllGenresSorted() throws Exception {
        var amountGenres = JdbcTestUtils.countRowsInTable(jdbcClient, GENRES_TABLE);
        MvcResult result = mockMvc.perform(get("/api/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(amountGenres))
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        List<String> names = JsonPath.parse(jsonResponse).read("$[*].name");

        assertThat(names).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
    }

    @Test
    void getGenreByIdReturnsCorrectGenre() throws Exception {
        long id = testDataHelper.idOfGenre(TestFixtures.GENRE_1_NAME);
        mockMvc.perform(get("/api/genres/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value(TestFixtures.GENRE_1_NAME));
    }

    @Test
    void getGenreByIdReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/api/genres/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBooksByGenreIdReturnsCorrectBooks() throws Exception {
        long id = testDataHelper.idOfGenre(TestFixtures.GENRE_1_NAME);
        int amountBooksOfGenre = JdbcTestUtils.countRowsInTableWhere(jdbcClient, BOOK_GENRES_TABLE, "genre_id = " + id);
        mockMvc.perform(get("/api/genres/{id}/books", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(amountBooksOfGenre));
    }

    @Test
    void getBooksByGenreNameReturnsCorrectBooks() throws Exception {
        long id = testDataHelper.idOfGenre(TestFixtures.GENRE_1_NAME);
        int amountBooksOfGenre = JdbcTestUtils.countRowsInTableWhere(jdbcClient, BOOK_GENRES_TABLE, "genre_id = " + id);
        mockMvc.perform(get("/api/genres/name/{name}/books", TestFixtures.GENRE_1_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(amountBooksOfGenre));
    }

    @Test
    void getAllGenresWithBooksReturnsGroupedData() throws Exception {
        mockMvc.perform(get("/api/genres/with-books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].books").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createGenreReturnsCreated() throws Exception {
        CreateGenreRequestDto request = new CreateGenreRequestDto("New Genre");
        mockMvc.perform(post("/api/genres")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Genre"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateGenreReturnsOk() throws Exception {
        long id = testDataHelper.idOfGenre(TestFixtures.GENRE_1_NAME);
        CreateGenreRequestDto request = new CreateGenreRequestDto("Updated Genre");
        mockMvc.perform(put("/api/genres/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Genre"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteGenreFailsIfBooksExist() throws Exception {
        long id = testDataHelper.idOfGenre(TestFixtures.GENRE_1_NAME);
        mockMvc.perform(delete("/api/genres/{id}", id)
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteGenreSucceedsIfNoBooks() throws Exception {
        // 1. Create a genre via API
        CreateGenreRequestDto request = new CreateGenreRequestDto("Temporary Genre");
        String jsonResponse = mockMvc.perform(post("/api/genres")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long id = JsonPath.parse(jsonResponse).read("$.id", Long.class);

        // 2. Delete it via API
        mockMvc.perform(delete("/api/genres/{id}", id)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotDeleteGenre() throws Exception {
        mockMvc.perform(delete("/api/genres/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
