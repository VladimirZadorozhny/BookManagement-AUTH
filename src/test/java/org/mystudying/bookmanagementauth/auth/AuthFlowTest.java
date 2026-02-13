package org.mystudying.bookmanagementauth.auth;

import org.junit.jupiter.api.Test;
import org.mystudying.bookmanagementauth.support.AbstractSecurityIntegrationTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
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

        mockMvc.perform(get("/api/auth/logout")
                .session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me")
                        .session(session))
                .andExpect(status().isUnauthorized());

    }
}
