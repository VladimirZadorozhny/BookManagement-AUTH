package org.mystudying.bookmanagementauth.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class AbstractSecurityIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;


    protected MockHttpSession loginAs(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(
                post("/api/auth/login")
                        .param("username", username)
                        .param("password", password)
        ).andExpect(status().isOk()).andReturn();

        return (MockHttpSession)  result.getRequest().getSession(false);
    }

    protected MockHttpSession loginAsAdmin() throws Exception {
        return  loginAs("admin@library.com", "admin");
    }

    protected MockHttpSession loginAsUser() throws Exception {
        return  loginAs("test1@example.com", "password");
    }
}
