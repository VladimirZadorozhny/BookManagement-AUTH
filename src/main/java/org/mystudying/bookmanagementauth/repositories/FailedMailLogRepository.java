package org.mystudying.bookmanagementauth.repositories;

import org.mystudying.bookmanagementauth.domain.FailedMailLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public interface FailedMailLogRepository extends JpaRepository<FailedMailLog, Long> {

    Page<FailedMailLog> findByToEmailContainingIgnoreCase(String toEmail, Pageable pageable);

    Page<FailedMailLog> findByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end, Pageable pageable);

    @Query("SELECT f FROM FailedMailLog f WHERE " +
            "(:toEmail IS NULL OR LOWER(f.toEmail) LIKE CONCAT('%', :toEmail, '%')) AND " +
            "(:start IS NULL OR f.createdAt >= :start) AND " +
            "(:end IS NULL OR f.createdAt <= :end)")
    Page<FailedMailLog> findByFilters(@Param("toEmail") String toEmail,
                                      @Param("start") OffsetDateTime start,
                                      @Param("end") OffsetDateTime end,
                                      Pageable pageable);
}
