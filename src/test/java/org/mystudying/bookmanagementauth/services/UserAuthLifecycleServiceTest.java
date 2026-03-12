package org.mystudying.bookmanagementauth.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mystudying.bookmanagementauth.domain.PasswordResetToken;
import org.mystudying.bookmanagementauth.domain.User;
import org.mystudying.bookmanagementauth.events.PasswordResetRequestedEvent;
import org.mystudying.bookmanagementauth.exceptions.InvalidTokenException;
import org.mystudying.bookmanagementauth.exceptions.TokenAlreadyUsedException;
import org.mystudying.bookmanagementauth.exceptions.TokenExpiredException;
import org.mystudying.bookmanagementauth.repositories.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserAuthLifecycleServiceTest {

    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private VerificationTokenRepository verificationTokenRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private UserAuthLifecycleService authLifecycleService;

    @BeforeEach
    void setUp() {
        authLifecycleService = new UserAuthLifecycleService(
                userRepository,
                roleRepository,
                passwordEncoder,
                verificationTokenRepository,
                passwordResetTokenRepository,
                eventPublisher
        );
    }

    @Test
    void requestPasswordResetSuccess() {
        User user = new User(1L, "User", "test@test.com", "old-pass");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        authLifecycleService.requestPasswordReset("test@test.com");

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        ArgumentCaptor<PasswordResetRequestedEvent> captor = ArgumentCaptor.forClass(PasswordResetRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        assertEquals("test@test.com", captor.getValue().email());
    }

    @Test
    void requestPasswordResetShouldPreventEnumeration() {
        // GIVEN: Email does not exist
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        // WHEN: Requesting reset
        authLifecycleService.requestPasswordReset("unknown@test.com");

        // THEN: No event is published
        verify(eventPublisher, never()).publishEvent(any());

        // AND: No token is actually saved to DB
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void resetPasswordSuccess() {
        User user = new User(1L, "User", "test@test.com", "old-pass");
        PasswordResetToken token = new PasswordResetToken("reset-token", user, OffsetDateTime.now().plusHours(1));

        when(passwordResetTokenRepository.findByToken("reset-token")).thenReturn(Optional.of(token));
        when(passwordResetTokenRepository.markTokenAsUsed(eq("reset-token"), any())).thenReturn(1);

        authLifecycleService.resetPassword("reset-token", "new-password");

        assertTrue(passwordEncoder.matches("new-password", user.getPassword()));
    }

    @Test
    void resetPasswordFailsForInvalidToken() {
        when(passwordResetTokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () ->
                authLifecycleService.resetPassword("invalid", "new-password")
        );
    }

    @Test
    void resetPasswordFailsForExpiredToken() {
        User user = new User(1L, "User", "test@test.com", "old-pass");
        PasswordResetToken token = new PasswordResetToken("expired-token", user, OffsetDateTime.now().minusMinutes(1));

        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThrows(TokenExpiredException.class, () ->
                authLifecycleService.resetPassword("expired-token", "new-password")
        );
    }

    @Test
    void resetPasswordFailsForAlreadyUsedToken() {
        User user = new User(1L, "User", "test@test.com", "old-pass");
        PasswordResetToken token = new PasswordResetToken("used-token", user, OffsetDateTime.now().plusHours(1));

        when(passwordResetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));
        when(passwordResetTokenRepository.markTokenAsUsed(eq("used-token"), any())).thenReturn(0);

        token.setUsed(true);

        assertThrows(TokenAlreadyUsedException.class, () ->
                authLifecycleService.resetPassword("used-token", "new-password")
        );
    }
}
