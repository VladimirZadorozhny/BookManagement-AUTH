package org.mystudying.bookmanagementauth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.mystudying.bookmanagementauth.dto.*;
import org.mystudying.bookmanagementauth.exceptions.UserNotFoundException;
import org.mystudying.bookmanagementauth.services.UserBookingService;
import org.mystudying.bookmanagementauth.services.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management and booking operations")
public class UserController {

    private final UserService userService;
    private final UserBookingService bookingService;

    public UserController(UserService userService, UserBookingService bookingService) {
        this.userService = userService;
        this.bookingService = bookingService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserDto> getAllUsers(@PageableDefault(size = 10) Pageable pageable) {
        return userService.findAll(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
    public UserDto getUserById(@PathVariable long id) {
        return userService.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto searchUser(@RequestParam String by) {
        return userService.findByNameOrEmail(by)
                .orElseThrow(() -> new UserNotFoundException(by));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody CreateUserRequestDto userDto) {
        return userService.save(userDto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
    public UserDto updateUser(@PathVariable long id, @Valid @RequestBody UpdateUserRequestDto userDto) {
        return userService.update(id, userDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable long id) {
        userService.deleteById(id);
    }

    @PostMapping("/{userId}/rent")
    @PreAuthorize("hasRole('ADMIN') or #userId == principal.id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rentBook(@PathVariable long userId, @Valid @RequestBody BookActionRequestDto requestDto) {
        bookingService.rentBook(userId, requestDto.bookId());
    }

    @PostMapping("/{userId}/return")
    @PreAuthorize("hasRole('ADMIN') or #userId == principal.id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void returnBook(@PathVariable long userId, @Valid @RequestBody BookActionRequestDto requestDto) {
        bookingService.returnBook(userId, requestDto.bookId());
    }

    @GetMapping("/{id}/bookings")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
    public List<BookingResponseDto> getUserBookings(@PathVariable long id) {
        return bookingService.findBookingsByUserId(id);
    }

    @PostMapping("/{userId}/bookings/{bookingId}/pay")
    @PreAuthorize("hasRole('ADMIN') or #userId == principal.id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void payFine(@PathVariable long userId, @PathVariable long bookingId) {
        bookingService.payFine(userId, bookingId);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activateUser(@PathVariable long id) {
        userService.activateUser(id);
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateUser(@PathVariable long id) {
        userService.deactivateUser(id);
    }
}
