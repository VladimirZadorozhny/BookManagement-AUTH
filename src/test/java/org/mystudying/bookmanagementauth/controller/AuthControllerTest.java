package org.mystudying.bookmanagementauth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mystudying.bookmanagementauth.dto.RegisterRequestDto;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Sql("/insertTestRecords.sql")
public class AuthControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public AuthControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

//    @Autowired
//    PasswordEncoder encoder;
//
//    @Test
//    void debugPasswordMatch() {
//        String raw = "password";
//        String stored = "{bcrypt}$2a$10$W8Fh/h9nADK75zl/zXWAeOsq43iLzwrtbeLi/HnZdYwUhzjbOT2Ra";
//
//        System.out.println(encoder.matches(raw, stored));
//        System.out.println(encoder.encode("admin"));
//    }

    @Test
    void registerNewUserReturnsCreated() throws Exception {
        RegisterRequestDto registration = new RegisterRequestDto(
                "New User",
                "newuser@example.com",
                "password123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New User"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.roles").value("ROLE_USER"));
    }

    @Test
    void registerExistingUserReturnsConflict() throws Exception {
        RegisterRequestDto registration = new RegisterRequestDto(
                "Test User 1",
                "test1@example.com",
                "password"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isConflict());
    }

    @Test
    void meReturnsUnauthorizedWhenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User is not authenticated"));
    }

    @Test
    @WithUserDetails("test1@example.com")
    void meReturnsCurrentUserWhenLoggedIn() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test1@example.com"))
                .andExpect(jsonPath("$.name").value("Test User 1"));
    }

    @Test
    void loginWithValidCredentialsReturnsOk() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .param("username", "test1@example.com")
                        .param("password", "password"))
                .andExpect(status().isOk())
                .andExpect(authenticated())
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void loginWithInvalidCredentialsReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .param("username", "test1@example.com")
                        .param("password", "wrongpassword"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    @WithUserDetails("test1@example.com")
    void logoutReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());
    }



}
