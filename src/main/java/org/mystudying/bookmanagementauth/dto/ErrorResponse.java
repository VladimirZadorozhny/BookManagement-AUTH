package org.mystudying.bookmanagementauth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record ErrorResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        String code
) {
}


