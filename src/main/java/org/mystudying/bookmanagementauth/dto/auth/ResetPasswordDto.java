package org.mystudying.bookmanagementauth.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordDto(
        @NotBlank(message = "Token is required.")
        String token,

        @NotBlank(message = "New password is required.")
        @Size(min = 6, message = "Password must be at least 6 characters long.")
        String newPassword
) {
}
