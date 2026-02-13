package org.mystudying.bookmanagementauth.controller;

import jakarta.validation.Valid;
import org.mystudying.bookmanagementauth.domain.Book;
import org.mystudying.bookmanagementauth.dto.*;
import org.mystudying.bookmanagementauth.exceptions.UserNotFoundException;
import org.mystudying.bookmanagementauth.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }



    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDto> getAllUsers() {
        return userService.findAll();
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
        Optional<UserDto> user = userService.findByName(by);
        if (user.isEmpty()) {
            user = userService.findByEmail(by);
        }
        return user.orElseThrow(() -> new UserNotFoundException(by));
    }

    /**
     * @deprecated Since introduction of Booking entity.
     * Use BookingRepository instead.
     * Kept temporarily to avoid breaking existing tests.
     */
    @Deprecated
    @GetMapping("/{id}/books")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
    public List<BookDto> getBooksByUser(@PathVariable long id) {
        return userService.findActiveBorrowedBooksByUserId(id).stream()
                .map(this::toBookDto)
                .collect(Collectors.toList());
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
        userService.rentBook(userId, requestDto.bookId());
    }

    @PostMapping("/{userId}/return")
    @PreAuthorize("hasRole('ADMIN') or #userId == principal.id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void returnBook(@PathVariable long userId, @Valid @RequestBody BookActionRequestDto requestDto) {
        userService.returnBook(userId, requestDto.bookId());
    }

    @GetMapping("/{id}/bookings")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
    public List<BookingResponseDto> getUserBookings(@PathVariable long id) {
        return userService.findBookingsByUserId(id);
    }

    @PostMapping("/{userId}/bookings/{bookingId}/pay")
    @PreAuthorize("hasRole('ADMIN') or #userId == principal.id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void payFine(@PathVariable long userId, @PathVariable long bookingId) {
        userService.payFine(userId, bookingId);
    }

    private BookDto toBookDto(Book book) {
        return new BookDto(book.getId(), book.getTitle(), book.getYear(), book.getAvailable());
    }
}


