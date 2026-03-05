package org.mystudying.bookmanagementauth.repositories;

import org.mystudying.bookmanagementauth.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    @Modifying
    @Query("UPDATE PasswordResetToken prt SET prt.used = true WHERE prt.token = :token AND prt.used = false AND prt.expiryDate > :now")
    int markTokenAsUsed(@Param("token") String token, @Param("now") OffsetDateTime now);
}
