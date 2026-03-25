package org.mystudying.bookmanagementauth.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequestDto(
        @NotBlank(message = "Email is required.")
        @Email(message = "Invalid email format.")
        String email
) {
}
