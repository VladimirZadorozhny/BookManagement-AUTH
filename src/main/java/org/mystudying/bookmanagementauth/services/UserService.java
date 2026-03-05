package org.mystudying.bookmanagementauth.services;

import jakarta.persistence.EntityManager;
import org.mystudying.bookmanagementauth.domain.Book;
import org.mystudying.bookmanagementauth.domain.Booking;
import org.mystudying.bookmanagementauth.domain.Role;
import org.mystudying.bookmanagementauth.domain.User;
import org.mystudying.bookmanagementauth.domain.VerificationToken;
import org.mystudying.bookmanagementauth.domain.PasswordResetToken;
import org.mystudying.bookmanagementauth.dto.*;
import org.mystudying.bookmanagementauth.exceptions.*;
import org.mystudying.bookmanagementauth.repositories.BookRepository;
import org.mystudying.bookmanagementauth.repositories.BookingRepository;
import org.mystudying.bookmanagementauth.repositories.RoleRepository;
import org.mystudying.bookmanagementauth.repositories.UserRepository;
import org.mystudying.bookmanagementauth.repositories.VerificationTokenRepository;
import org.mystudying.bookmanagementauth.repositories.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.mystudying.bookmanagementauth.events.UserRegisteredEvent;
import org.mystudying.bookmanagementauth.events.PasswordResetRequestedEvent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BookingRepository bookingRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final InventoryService inventoryService;
    private final ApplicationEventPublisher eventPublisher;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public UserService(UserRepository userRepository,
                       BookRepository bookRepository,
                       BookingRepository bookingRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       EntityManager entityManager,
                       InventoryService inventoryService,
                       ApplicationEventPublisher eventPublisher,
                       VerificationTokenRepository verificationTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.bookingRepository = bookingRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
        this.inventoryService = inventoryService;
        this.eventPublisher = eventPublisher;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    public List<UserDto> findAll() {
        return userRepository.findAll(Sort.by("name")).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * @deprecated Since introduction of Booking entity.
     * Use BookingRepository instead.
     * Kept temporarily to avoid breaking existing tests.
     */
    @Deprecated
    public List<UserDto> findUsersWithMoreThanXBooks(long count) {
        return userRepository.findUsersWithMoreThanXBooks(count).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Optional<UserDto> findById(long id) {
        return userRepository.findById(id).map(this::toDto);
    }

    public Optional<UserDto> findByName(String name) {
        return userRepository.findByName(name).map(this::toDto);
    }

    public Optional<UserDto> findByEmail(String email) {
        return userRepository.findByEmail(email).map(this::toDto);
    }

    /**
     * @deprecated Since introduction of Booking entity.
     * Use BookingRepository instead.
     * Kept temporarily to avoid breaking existing tests.
     */
    @Deprecated
    public List<Book> findActiveBorrowedBooksByUserId(long userId) {
        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return bookingRepository.findActiveBookingsWithBooksByUserId(userId).stream()
                .map(Booking::getBook)
                .collect(Collectors.toList());
    }

    public List<BookingResponseDto> findBookingsByUserId(long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return bookingRepository.findAllByUserIdWithBooks(userId).stream()
                .sorted((b1, b2) -> {
                    if (b1.getReturnedAt() == null && b2.getReturnedAt() != null) return -1;
                    if (b1.getReturnedAt() != null && b2.getReturnedAt() == null) return 1;
                    return b2.getBorrowedAt().compareTo(b1.getBorrowedAt());
                })
                .map(b -> {
                    BigDecimal displayFine = b.getFine();
                    if (b.getReturnedAt() == null && b.isExpired()) {
                        displayFine = b.calculateFine();
                    }
                    return new BookingResponseDto(
                            b.getId(),
                            user.getId(),
                            user.getName(),
                            b.getBook().getId(),
                            b.getBook().getTitle(),
                            b.getBook().getYear(),
                            b.getBorrowedAt(),
                            b.getDueAt(),
                            b.getReturnedAt(),
                            displayFine,
                            b.isFinePaid()
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void payFine(long userId, long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookNotBorrowedException());

        if (booking.getUser().getId() != userId) {
            throw new UserNotFoundException(userId); // Mismatch
        }

        if (booking.getFine().compareTo(BigDecimal.ZERO) > 0 && !booking.isFinePaid()) {
            booking.setFinePaid(true);
        }
    }

    @Transactional
    public UserDto register(RegisterRequestDto registerRequestDto) {
        try {
            User user = new User(null,
                    registerRequestDto.name(),
                    registerRequestDto.email(),
                    passwordEncoder.encode(registerRequestDto.password()));
            user.setActive(false); // New user starts as inactive until verified

            roleRepository.findByName("ROLE_USER").ifPresent(user::addRole);

            User savedUser = userRepository.save(user);

            String token = UUID.randomUUID().toString();
            VerificationToken verificationToken = new VerificationToken(
                    token,
                    savedUser,
                    OffsetDateTime.now().plusHours(24) // Token expires in 24 hours
            );
            verificationTokenRepository.save(verificationToken);

            eventPublisher.publishEvent(new UserRegisteredEvent(savedUser.getId(), savedUser.getEmail(), savedUser.getName(), token)); // Publish event with token and name
            return toDto(savedUser);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyExistsException(registerRequestDto.email());
        }
    }

    @Transactional
    public UserDto save(CreateUserRequestDto createUserRequestDto) {
        try {
            User user = new User(null,
                    createUserRequestDto.name(),
                    createUserRequestDto.email(),
                    passwordEncoder.encode(createUserRequestDto.password()));
            // Admin created users are active by default, no verification email

            roleRepository.findByName("ROLE_USER").ifPresent(user::addRole);

            User savedUser = userRepository.save(user);
            // No event published for admin-created users; no email verification needed
            return toDto(savedUser);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyExistsException(createUserRequestDto.email());
        }
    }

    @Transactional
    public void requestPasswordReset(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            // Log for internal tracking but return success to frontend to prevent user enumeration
            log.info("Password reset requested for non-existent email: {}", email);
            return;
        }

        User user = userOptional.get();
        String token = UUID.randomUUID().toString();
        PasswordResetToken passwordResetToken = new PasswordResetToken(
                token,
                user,
                OffsetDateTime.now().plusHours(2) // Token expires in 2 hours
        );
        passwordResetTokenRepository.save(passwordResetToken);

        eventPublisher.publishEvent(new PasswordResetRequestedEvent(user.getEmail(), token));
    }

    @Transactional
    public void resetPassword(String tokenValue, String newPassword) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token."));

        if (token.isExpired()) {
            throw new TokenExpiredException("Password reset token has expired.");
        }

        // Attempt to mark the token as used atomically
        int updatedRows = passwordResetTokenRepository.markTokenAsUsed(tokenValue, OffsetDateTime.now());

        if (updatedRows == 0) {
            // This means the token was already used or expired in a race condition
            // Re-fetch to check exact state, or assume it was already used
            PasswordResetToken recheckedToken = passwordResetTokenRepository.findByToken(tokenValue)
                    .orElseThrow(() -> new InvalidTokenException("Token not found during recheck."));

            if (recheckedToken.isUsed()) {
                throw new TokenAlreadyUsedException("Password reset token has already been used.");
            } else if (recheckedToken.isExpired()) {
                throw new TokenExpiredException("Password reset token has expired.");
            } else {
                // Fallback for unexpected cases
                throw new InvalidTokenException("Could not use token, possibly due to a race condition or invalid state.");
            }
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));

    }

    @Transactional
    public UserDto update(long id, UpdateUserRequestDto updateUserRequestDto) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        try {
            user.setName(updateUserRequestDto.name());
            user.setEmail(updateUserRequestDto.email());

            if (updateUserRequestDto.password() != null && !updateUserRequestDto.password().isBlank()) {
                user.setPassword(passwordEncoder.encode(updateUserRequestDto.password()));
            }

            if (updateUserRequestDto.active() != null) {
                user.setActive(updateUserRequestDto.active());
            }

            return toDto(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyExistsException(user.getEmail());
        }
    }

    @Transactional
    public void deleteById(long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        if (!user.getBookings().isEmpty()) {
            throw new UserHasBookingsException(id);
        }
        userRepository.delete(user);
    }

    @Transactional
    public void rentBook(long userId, long bookId) {
        User user = userRepository.findUserByIdWithBookings(userId).orElseThrow(() -> new UserNotFoundException(userId));

        if (bookingRepository.findActiveBooking(userId, bookId).isPresent()) {
            throw new BookAlreadyBorrowedException();
        }

        // Extra checking before renting
        boolean hasOverdue = user.getBookings().stream()
                .anyMatch(Booking::isExpired);
        if (hasOverdue) {
            throw new UserHasOverdueBooksException(userId);
        }

        boolean hasFines = user.getBookings().stream()
                .anyMatch(b -> b.getFine().compareTo(BigDecimal.ZERO) > 0 && !b.isFinePaid());
        if (hasFines) {
            throw new UserHasUnpaidFinesException(userId);
        }

        inventoryService.decrementStock(bookId);

        Book bookRef = entityManager.getReference(Book.class, bookId);
        Booking booking = new Booking(user, bookRef, LocalDate.now(), LocalDate.now().plusDays(14));
        user.addBooking(booking);
        bookingRepository.save(booking);
    }

    @Transactional
    public void returnBook(long userId, long bookId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        if (!bookRepository.existsById(bookId)) {
            throw new BookNotFoundException(bookId);
        }

        Booking booking = bookingRepository.findActiveBooking(userId, bookId)
                .orElseThrow(() -> new BookNotBorrowedException());

        booking.setReturnedAt(LocalDate.now());
        booking.setFine(booking.calculateFine());

        inventoryService.incrementStock(bookId);
    }

    private UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.isActive(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet())
        );
    }
}
