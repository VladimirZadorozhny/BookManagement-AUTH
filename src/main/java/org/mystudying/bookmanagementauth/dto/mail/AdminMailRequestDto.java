package org.mystudying.bookmanagementauth.dto.mail;

import jakarta.validation.constraints.NotBlank;

public record AdminMailRequestDto(
        @NotBlank(message = "Subject is required")
        String subject,

        @NotBlank(message = "Body is required")
        String body,

        Long minBooksBorrowed
) {
}
