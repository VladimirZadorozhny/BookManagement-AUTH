package org.mystudying.bookmanagementauth.auth;

import org.junit.jupiter.api.Test;
import org.mystudying.bookmanagementauth.support.AbstractSecurityIntegrationTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.jdbc.Sql;

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
    void logoutInvalidatesSession() throws  Exception {
        MockHttpSession session = loginAsUser();

        mockMvc.perform(post("/api/auth/logout")
                .session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me")
                        .session(session))
                .andExpect(status().isUnauthorized());

    }

    @Test
    void userCanAccessOwnData() throws Exception {
        long id = idOfUser("test1@example.com");
        MockHttpSession session = loginAsUser();

        mockMvc.perform(get("/api/users/{id}", id)
                        .session(session))
                .andExpect(status().isOk());
    }

    @Test
    void userCannotAccessOtherUserData() throws Exception {
        long otherId = idOfUser("test2@example.com");
        MockHttpSession session = loginAsUser();   // Login as user with "test1@example.com"

        mockMvc.perform(get("/api/users/{id}", otherId)
                        .session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanDeleteUser() throws Exception {
        long userId = idOfUser("delete@example.com");
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(delete("/api/users/{id}", userId)
                        .session(session))
                .andExpect(status().isNoContent());
    }

    @Test
    void normalUserCannotDeleteUser() throws Exception {
        long userId = idOfUser("delete@example.com");
        MockHttpSession session = loginAsUser();

        mockMvc.perform(delete("/api/users/{id}", userId)
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
