package org.mystudying.bookmanagementauth.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record UpdateBookRequestDto(
        @NotBlank(message = "Title cannot be blank")
        String title,

        @NotNull(message = "Year cannot be null")
        @Positive(message = "Year must be a positive number")
        @YearValid
        Integer year,

        @NotNull(message = "Author ID cannot be null")
        @Positive(message = "Author ID must be a positive number")
        Long authorId,

        @NotEmpty(message = "At least one genre must be selected")
        List<Long> genreIds
) {
}


