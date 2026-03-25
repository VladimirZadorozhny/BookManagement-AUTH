package org.mystudying.bookmanagementauth.services.user;

import jakarta.persistence.EntityManager;
import org.mystudying.bookmanagementauth.domain.Book;
import org.mystudying.bookmanagementauth.domain.Booking;
import org.mystudying.bookmanagementauth.domain.User;
import org.mystudying.bookmanagementauth.dto.booking.BookingResponseDto;
import org.mystudying.bookmanagementauth.exceptions.*;
import org.mystudying.bookmanagementauth.repositories.BookRepository;
import org.mystudying.bookmanagementauth.repositories.BookingRepository;
import org.mystudying.bookmanagementauth.repositories.UserRepository;
import org.mystudying.bookmanagementauth.services.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserBookingService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BookingRepository bookingRepository;
    private final InventoryService inventoryService;
    private final EntityManager entityManager;

    public UserBookingService(UserRepository userRepository,
                              BookRepository bookRepository,
                              BookingRepository bookingRepository,
                              InventoryService inventoryService,
                              EntityManager entityManager) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.bookingRepository = bookingRepository;
        this.inventoryService = inventoryService;
        this.entityManager = entityManager;
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
    public void rentBook(long userId, long bookId) {
        User user = userRepository.findUserByIdWithBookings(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (bookingRepository.findActiveBooking(userId, bookId).isPresent()) {
            throw new BookAlreadyBorrowedException();
        }

        boolean hasOverdue = user.getBookings().stream().anyMatch(Booking::isExpired);
        if (hasOverdue) throw new UserHasOverdueBooksException(userId);

        boolean hasFines = user.getBookings().stream()
                .anyMatch(b -> b.getFine().compareTo(BigDecimal.ZERO) > 0 && !b.isFinePaid());
        if (hasFines) throw new UserHasUnpaidFinesException(userId);

        inventoryService.decrementStock(bookId);

        Book bookRef = entityManager.getReference(Book.class, bookId);
        Booking booking = new Booking(user, bookRef, LocalDate.now(), LocalDate.now().plusDays(14));
        user.addBooking(booking);
        bookingRepository.save(booking);
    }

    @Transactional
    public void returnBook(long userId, long bookId) {
        if (!userRepository.existsById(userId)) throw new UserNotFoundException(userId);
        if (!bookRepository.existsById(bookId)) throw new BookNotFoundException(bookId);

        Booking booking = bookingRepository.findActiveBooking(userId, bookId)
                .orElseThrow(() -> new BookNotBorrowedException());

        booking.setReturnedAt(LocalDate.now());
        booking.setFine(booking.calculateFine());

        inventoryService.incrementStock(bookId);
    }

    @Transactional
    public void payFine(long userId, long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookNotBorrowedException());

        if (booking.getUser().getId() != userId) throw new UserNotFoundException(userId);

        if (booking.getFine().compareTo(BigDecimal.ZERO) > 0 && !booking.isFinePaid()) {
            booking.setFinePaid(true);
        }
    }
}
