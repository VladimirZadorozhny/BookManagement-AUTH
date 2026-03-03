package org.mystudying.bookmanagementauth.repositories;

import org.mystudying.bookmanagementauth.domain.User;
import org.mystudying.bookmanagementauth.domain.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    void deleteByUser(User user);
}
