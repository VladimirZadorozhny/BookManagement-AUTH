package org.mystudying.bookmanagementauth.dto;

import java.time.OffsetDateTime;

public record FailedMailLogDto(
        Long id,
        String toEmail,
        String subject,
        String body,
        String errorMessage,
        OffsetDateTime createdAt,
        OffsetDateTime lastAttemptAt,
        int attemptCount
) {
}
