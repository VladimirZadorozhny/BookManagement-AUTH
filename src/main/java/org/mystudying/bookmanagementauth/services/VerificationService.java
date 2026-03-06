package org.mystudying.bookmanagementauth.services;

import org.mystudying.bookmanagementauth.domain.User;
import org.mystudying.bookmanagementauth.domain.VerificationToken;
import org.mystudying.bookmanagementauth.exceptions.InvalidTokenException;
import org.mystudying.bookmanagementauth.exceptions.TokenAlreadyUsedException;
import org.mystudying.bookmanagementauth.exceptions.TokenExpiredException;
import org.mystudying.bookmanagementauth.repositories.UserRepository;
import org.mystudying.bookmanagementauth.repositories.VerificationTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class VerificationService {

    private final VerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;

    public VerificationService(VerificationTokenRepository verificationTokenRepository, UserRepository userRepository) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void verifyToken(String tokenValue) {
        VerificationToken token = verificationTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token."));

        if (token.isExpired()) {
            throw new TokenExpiredException("Verification token has expired.");
        }

        // Attempt to mark the token as used atomically
        int updatedRows = verificationTokenRepository.markTokenAsUsed(tokenValue, OffsetDateTime.now());

        if (updatedRows == 0) {
            // This means the token was already used or expired in a race condition
            // Re-fetch to check exact state
            VerificationToken recheckedToken = verificationTokenRepository.findByToken(tokenValue)
                    .orElseThrow(() -> new InvalidTokenException("Token not found during recheck."));

            if (recheckedToken.isUsed()) {
                throw new TokenAlreadyUsedException("Verification token has already been used.");
            } else if (recheckedToken.isExpired()) {
                throw new TokenExpiredException("Verification token has expired.");
            } else {
                // Fallback for unexpected cases
                throw new InvalidTokenException("Could not use token, possibly due to a race condition or invalid state.");
            }
        }
        User user = token.getUser();
        user.setActive(true); // Enable the user account
    }
}