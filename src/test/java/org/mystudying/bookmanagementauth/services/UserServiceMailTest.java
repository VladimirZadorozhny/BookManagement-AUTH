package org.mystudying.bookmanagementauth.services;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mystudying.bookmanagementauth.domain.PasswordResetToken;
import org.mystudying.bookmanagementauth.domain.User;
import org.mystudying.bookmanagementauth.events.PasswordResetRequestedEvent;
import org.mystudying.bookmanagementauth.repositories.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class UserServiceMailTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private BookRepository bookRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private InventoryService inventoryService;


    private PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();


    private UserService userService;


    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository,
                bookRepository,
                bookingRepository,
                roleRepository,
                passwordEncoder,
                entityManager,
                inventoryService,
                eventPublisher,
                verificationTokenRepository,
                passwordResetTokenRepository);
    }

    @Test
    void requestPasswordReset_Success() {
        User user = new User(1L, "User", "test@test.com", "old-pass");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        userService.requestPasswordReset("test@test.com");

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));

        verify(eventPublisher).publishEvent(any(PasswordResetRequestedEvent.class));


//        extra assertions
        ArgumentCaptor<PasswordResetRequestedEvent> captor =
                ArgumentCaptor.forClass(PasswordResetRequestedEvent.class);

        verify(eventPublisher).publishEvent(captor.capture());

        PasswordResetRequestedEvent event = captor.getValue();
        assertEquals("test@test.com", event.email());
        assertNotNull(event.token());
    }

    @Test
    void resetPassword_Success() {
        User user = new User(1L, "User", "test@test.com", "old-pass");
        PasswordResetToken token = new PasswordResetToken("reset-token", user, OffsetDateTime.now().plusHours(1));

        when(passwordResetTokenRepository.findByToken("reset-token")).thenReturn(Optional.of(token));

        userService.resetPassword("reset-token", "new-password");

        assertTrue(passwordEncoder.matches("new-password", user.getPassword()));
        assertTrue(token.isUsed());
    }
}
