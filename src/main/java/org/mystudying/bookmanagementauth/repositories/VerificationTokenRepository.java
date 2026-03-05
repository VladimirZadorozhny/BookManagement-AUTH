package org.mystudying.bookmanagementauth.repositories;

import org.mystudying.bookmanagementauth.domain.User;
import org.mystudying.bookmanagementauth.domain.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    void deleteByUser(User user);

    @Modifying
    @Query("UPDATE VerificationToken vt SET vt.used = true WHERE vt.token = :token AND vt.used = false AND vt.expiryDate > :now")
    int markTokenAsUsed(@Param("token") String token, @Param("now") OffsetDateTime now);
}
