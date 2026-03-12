package org.mystudying.bookmanagementauth.services;

import org.mystudying.bookmanagementauth.domain.User;
import org.mystudying.bookmanagementauth.domain.VerificationToken;
import org.mystudying.bookmanagementauth.domain.PasswordResetToken;
import org.mystudying.bookmanagementauth.dto.RegisterRequestDto;
import org.mystudying.bookmanagementauth.dto.UserDto;
import org.mystudying.bookmanagementauth.events.PasswordResetRequestedEvent;
import org.mystudying.bookmanagementauth.events.UserRegisteredEvent;
import org.mystudying.bookmanagementauth.exceptions.*;
import org.mystudying.bookmanagementauth.repositories.PasswordResetTokenRepository;
import org.mystudying.bookmanagementauth.repositories.RoleRepository;
import org.mystudying.bookmanagementauth.repositories.UserRepository;
import org.mystudying.bookmanagementauth.repositories.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserAuthLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(UserAuthLifecycleService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UserAuthLifecycleService(UserRepository userRepository,
                                    RoleRepository roleRepository,
                                    PasswordEncoder passwordEncoder,
                                    VerificationTokenRepository verificationTokenRepository,
                                    PasswordResetTokenRepository passwordResetTokenRepository,
                                    ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UserDto register(RegisterRequestDto registerRequestDto) {
        try {
            User user = new User(null,
                    registerRequestDto.name(),
                    registerRequestDto.email(),
                    passwordEncoder.encode(registerRequestDto.password()));
            user.setActive(false);

            roleRepository.findByName("ROLE_USER").ifPresent(user::addRole);

            User savedUser = userRepository.save(user);

            String token = UUID.randomUUID().toString();
            VerificationToken verificationToken = new VerificationToken(
                    token,
                    savedUser,
                    OffsetDateTime.now().plusHours(24)
            );
            verificationTokenRepository.save(verificationToken);

            eventPublisher.publishEvent(new UserRegisteredEvent(savedUser.getId(), savedUser.getEmail(), savedUser.getName(), token));
            return toDto(savedUser);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyExistsException(registerRequestDto.email());
        }
    }

    @Transactional
    public void requestPasswordReset(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            log.info("Password reset requested for non-existent email: {}", email);
            return;
        }

        User user = userOptional.get();
        String token = UUID.randomUUID().toString();
        PasswordResetToken passwordResetToken = new PasswordResetToken(
                token,
                user,
                OffsetDateTime.now().plusHours(2)
        );
        passwordResetTokenRepository.save(passwordResetToken);

        eventPublisher.publishEvent(new PasswordResetRequestedEvent(user.getEmail(), token));
    }

    @Transactional
    public void resetPassword(String tokenValue, String newPassword) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token."));

        if (token.isExpired()) {
            throw new TokenExpiredException("Password reset token has expired.");
        }

        int updatedRows = passwordResetTokenRepository.markTokenAsUsed(tokenValue, OffsetDateTime.now());

        if (updatedRows == 0) {
            PasswordResetToken recheckedToken = passwordResetTokenRepository.findByToken(tokenValue)
                    .orElseThrow(() -> new InvalidTokenException("Token not found during recheck."));

            if (recheckedToken.isUsed()) {
                throw new TokenAlreadyUsedException("Password reset token has already been used.");
            } else if (recheckedToken.isExpired()) {
                throw new TokenExpiredException("Password reset token has expired.");
            } else {
                throw new InvalidTokenException("Could not use token, possibly due to a race condition or invalid state.");
            }
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
    }

    private UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.isActive(),
                user.getRoles().stream()
                        .map(r -> r.getName())
                        .collect(Collectors.toSet())
        );
    }
}
