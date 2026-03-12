package org.mystudying.bookmanagementauth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mystudying.bookmanagementauth.dto.*;
import org.mystudying.bookmanagementauth.services.UserAuthLifecycleService;
import org.mystudying.bookmanagementauth.services.UserService;
import org.mystudying.bookmanagementauth.support.db.TestFixtures;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Sql("/insertTestRecords.sql")
public class AuthControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private UserAuthLifecycleService authLifecycleService;

    public AuthControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void registerNewUserReturnsCreated() throws Exception {
        RegisterRequestDto registration = new RegisterRequestDto(
                "New User",
                "newuser@example.com",
                "password123"
        );
        UserDto userDto = new UserDto(100L, "New User", "newuser@example.com", false, Set.of("ROLE_USER"));

        when(authLifecycleService.register(any(RegisterRequestDto.class))).thenReturn(userDto);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New User"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"));
    }

    @Test
    void meReturnsUnauthorizedWhenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void loginWithValidCredentialsReturnsOk() throws Exception {

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .param("username", TestFixtures.USER_1_EMAIL)
                        .param("password", TestFixtures.COMMON_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(authenticated())
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void logoutReturnsFoundAndRedirects() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void requestPasswordResetReturnsOk() throws Exception {
        PasswordResetRequestDto request = new PasswordResetRequestDto(TestFixtures.USER_1_EMAIL);

        mockMvc.perform(post("/api/auth/password-reset-request")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password reset link sent to your email if an account exists."));

        verify(authLifecycleService).requestPasswordReset(TestFixtures.USER_1_EMAIL);
    }

    @Test
    void resetPasswordReturnsOk() throws Exception {
        ResetPasswordDto request = new ResetPasswordDto("valid-token", "new-password123");

        mockMvc.perform(post("/api/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Your password has been reset successfully."));

        verify(authLifecycleService).resetPassword("valid-token", "new-password123");
    }
}
