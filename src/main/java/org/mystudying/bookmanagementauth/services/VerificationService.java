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

        if (token.isUsed()) {
            throw new TokenAlreadyUsedException("Verification token has already been used.");
        }

        if (token.isExpired()) {
            throw new TokenExpiredException("Verification token has expired.");
        }

        User user = token.getUser();
        user.setActive(true); // Enable the user account


        token.setUsed(true); // Mark token as used

    }
}
