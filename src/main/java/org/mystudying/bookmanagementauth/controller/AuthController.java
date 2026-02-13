package org.mystudying.bookmanagementauth.controller;

import jakarta.validation.Valid;
import org.mystudying.bookmanagementauth.config.UserPrincipal;
import org.mystudying.bookmanagementauth.dto.RegisterRequestDto;
import org.mystudying.bookmanagementauth.dto.UserDto;
import org.mystudying.bookmanagementauth.exceptions.UnauthorizedException;
import org.mystudying.bookmanagementauth.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto register(@Valid @RequestBody RegisterRequestDto registrationDto) {
        return userService.register(registrationDto);
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
