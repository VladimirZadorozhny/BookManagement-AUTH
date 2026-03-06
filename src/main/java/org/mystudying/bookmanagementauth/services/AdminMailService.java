package org.mystudying.bookmanagementauth.services;

import org.mystudying.bookmanagementauth.domain.Booking;
import org.mystudying.bookmanagementauth.domain.User;
import org.mystudying.bookmanagementauth.events.AdminUserMailRequestedEvent;
import org.mystudying.bookmanagementauth.exceptions.UserNotFoundException;
import org.mystudying.bookmanagementauth.repositories.BookingRepository;
import org.mystudying.bookmanagementauth.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminMailService {

    private static final Logger log = LoggerFactory.getLogger(AdminMailService.class);
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AdminMailService(BookingRepository bookingRepository, UserRepository userRepository, ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    public void sendBulkMailToHeavyUsers(String subject, String body, Long minBooksBorrowed) {
        List<Booking> heavyUserBookings = bookingRepository.findHeavyUserBookingsForMailing(minBooksBorrowed);

        heavyUserBookings.stream()
                .map(Booking::getUser)
                .distinct()
                .forEach(user -> publishAdminUserMailEvent(user.getId(), user.getEmail(), subject, body));
    }

    public void sendBulkMailToOverdueUsers(String subject, String body) {
        List<User> overdueUsers = bookingRepository.findActiveWithDetails(Pageable.unpaged())
                .stream()
                .filter(Booking::isExpired)
                .map(Booking::getUser)
                .distinct()
                .toList();

        overdueUsers.forEach(user -> publishAdminUserMailEvent(user.getId(), user.getEmail(), subject, body));
    }

    public void sendMailToUser(Long userId, String subject, String body) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        publishAdminUserMailEvent(user.getId(), user.getEmail(), subject, body);
    }

    @Transactional
    public void publishAdminUserMailEvent(Long userId, String email, String subject, String body) {
        log.debug("Publishing admin mail event for user {}", userId);
        eventPublisher.publishEvent(new AdminUserMailRequestedEvent(userId, email, subject, body));
    }
}
