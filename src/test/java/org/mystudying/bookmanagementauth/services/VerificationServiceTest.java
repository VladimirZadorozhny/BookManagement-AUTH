package org.mystudying.bookmanagementauth.services;

import org.junit.jupiter.api.Test;
import org.mystudying.bookmanagementauth.domain.User;
import org.mystudying.bookmanagementauth.domain.VerificationToken;
import org.mystudying.bookmanagementauth.exceptions.InvalidTokenException;
import org.mystudying.bookmanagementauth.exceptions.TokenAlreadyUsedException;
import org.mystudying.bookmanagementauth.exceptions.TokenExpiredException;
import org.mystudying.bookmanagementauth.repositories.UserRepository;
import org.mystudying.bookmanagementauth.repositories.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class VerificationServiceTest {

    @Autowired
    private VerificationService verificationService;

    @MockBean
    private VerificationTokenRepository tokenRepository;

    @MockBean
    private UserRepository userRepository;

    @Test
    void verifyTokenSuccess() {
        User user = new User(1L, "User", "test@test.com", "pass");
        user.setActive(false);
        VerificationToken token = new VerificationToken("valid-token", user, OffsetDateTime.now().plusHours(1));

        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        verificationService.verifyToken("valid-token");

        assertTrue(user.isActive());
        assertTrue(token.isUsed());
    }

    @Test
    void verifyTokenInvalid() {
        when(tokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> verificationService.verifyToken("invalid"));
    }

    @Test
    void verifyTokenExpired() {
        User user = new User(1L, "User", "test@test.com", "pass");
        VerificationToken token = new VerificationToken("expired-token", user, OffsetDateTime.now().minusHours(1));

        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThrows(TokenExpiredException.class, () -> verificationService.verifyToken("expired-token"));
    }

    @Test
    void verifyTokenAlreadyUsed() {
        User user = new User(1L, "User", "test@test.com", "pass");
        VerificationToken token = new VerificationToken("used-token", user, OffsetDateTime.now().plusHours(1));
        token.setUsed(true);

        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        assertThrows(TokenAlreadyUsedException.class, () -> verificationService.verifyToken("used-token"));
    }
}
