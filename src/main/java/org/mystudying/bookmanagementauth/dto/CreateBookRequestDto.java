package org.mystudying.bookmanagementauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.List;

@Schema(description = "Request to create a new book")
public record CreateBookRequestDto(
        @NotBlank(message = "Title cannot be blank")
        @Schema(example = "The Great Gatsby")
        String title,

        @NotNull(message = "Year cannot be null")
        @Positive(message = "Year must be a positive number")
        @YearValid
        @Schema(example = "1925")
        Integer year,

        @NotNull(message = "Author ID cannot be null")
        @Positive(message = "Author ID must be a positive number")
        @Schema(example = "1")
        Long authorId,

        @NotNull(message = "Available count cannot be null")
        @Min(value = 0, message = "Available count cannot be negative")
        @Schema(example = "10")
        Integer available,

        @NotEmpty(message = "At least one genre must be selected")
        @Schema(example = "[1, 2]")
        List<Long> genreIds
) {
}


