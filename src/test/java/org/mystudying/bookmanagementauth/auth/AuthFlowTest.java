package org.mystudying.bookmanagementauth.auth;

import org.junit.jupiter.api.Test;
import org.mystudying.bookmanagementauth.support.AbstractSecurityIntegrationTest;
import org.mystudying.bookmanagementauth.support.db.TestFixtures;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("/insertTestRecords.sql")
public class AuthFlowTest extends AbstractSecurityIntegrationTest {


    @Test
    void loginThenAccessProtectedEndpoint() throws Exception {

        MockHttpSession session = loginAsUser();

        mockMvc.perform(get("/api/auth/me")
                        .session(session))
                .andExpect(status().isOk());
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        MockHttpSession session = loginAsUser();

        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .session(session))
                .andExpect(status().isFound())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/"));

        mockMvc.perform(get("/api/auth/me")
                        .session(session))
                .andExpect(status().isUnauthorized());

    }

    @Test
    void userCanAccessOwnData() throws Exception {
        long id = testDataHelper.idOfUser(TestFixtures.USER_1_EMAIL);
        MockHttpSession session = loginAsUser();

        mockMvc.perform(get("/api/users/{id}", id)
                        .session(session))
                .andExpect(status().isOk());
    }

    @Test
    void userCannotAccessOtherUserData() throws Exception {
        long otherId = testDataHelper.idOfUser(TestFixtures.USER_2_EMAIL);
        MockHttpSession session = loginAsUser();   // Login as user with "test1@example.com"

        mockMvc.perform(get("/api/users/{id}", otherId)
                        .session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanDeleteUser() throws Exception {
        long userId = testDataHelper.idOfUser(TestFixtures.USER_DELETE_EMAIL);
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(delete("/api/users/{id}", userId)
                        .with(csrf())
                        .session(session))
                .andExpect(status().isNoContent());
    }

    @Test
    void normalUserCannotDeleteUser() throws Exception {
        long userId = testDataHelper.idOfUser(TestFixtures.USER_DELETE_EMAIL);
        MockHttpSession session = loginAsUser();

        mockMvc.perform(delete("/api/users/{id}", userId)
                        .with(csrf())
                        .session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCanViewBooks() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousCannotAccessMe() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
