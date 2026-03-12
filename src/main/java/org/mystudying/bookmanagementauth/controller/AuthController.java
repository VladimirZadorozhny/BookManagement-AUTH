package org.mystudying.bookmanagementauth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.mystudying.bookmanagementauth.config.UserPrincipal;
import org.mystudying.bookmanagementauth.dto.PasswordResetRequestDto;
import org.mystudying.bookmanagementauth.dto.RegisterRequestDto;
import org.mystudying.bookmanagementauth.dto.ResetPasswordDto;
import org.mystudying.bookmanagementauth.dto.UserDto;
import org.mystudying.bookmanagementauth.exceptions.UnauthorizedException;
import org.mystudying.bookmanagementauth.services.UserAuthLifecycleService;
import org.mystudying.bookmanagementauth.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Public endpoints for registration and session info")
public class AuthController {

    private final UserService userService;
    private final UserAuthLifecycleService authLifecycleService;

    public AuthController(UserService userService, UserAuthLifecycleService authLifecycleService) {
        this.userService = userService;
        this.authLifecycleService = authLifecycleService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto register(@Valid @RequestBody RegisterRequestDto registrationDto) {
        return authLifecycleService.register(registrationDto);
    }

    @PostMapping("/password-reset-request")
    public ResponseEntity<String> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDto requestDto) {
        authLifecycleService.requestPasswordReset(requestDto.email());
        return ResponseEntity.ok("Password reset link sent to your email if an account exists.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordDto resetPasswordDto) {
        authLifecycleService.resetPassword(resetPasswordDto.token(), resetPasswordDto.newPassword());
        return ResponseEntity.ok("Your password has been reset successfully.");
    }

    @GetMapping("/me")
    public UserDto getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return userService.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("User session is valid but user not found"));
    }
}
