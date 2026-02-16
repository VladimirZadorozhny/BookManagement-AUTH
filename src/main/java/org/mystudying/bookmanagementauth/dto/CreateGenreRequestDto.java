package org.mystudying.bookmanagementauth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGenreRequestDto(
    @NotBlank(message = "Genre name cannot be blank")
    @Size(max = 50, message = "Genre name cannot exceed 50 characters")
    String name
) {}
