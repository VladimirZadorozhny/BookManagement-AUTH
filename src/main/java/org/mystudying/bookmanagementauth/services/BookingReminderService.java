package org.mystudying.bookmanagementauth.services;

import org.mystudying.bookmanagementauth.domain.Booking;
import org.mystudying.bookmanagementauth.domain.ReminderType;
import org.mystudying.bookmanagementauth.events.BookingReminderEvent;
import org.mystudying.bookmanagementauth.repositories.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;

@Service
public class BookingReminderService {

    private static final Logger log = LoggerFactory.getLogger(BookingReminderService.class);
    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BookingReminderService(BookingRepository bookingRepository, ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void processReminder(Long bookingId, ReminderType type) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        log.debug("Publishing {} reminder for booking {}", type, bookingId);

        eventPublisher.publishEvent(new BookingReminderEvent(
                booking.getId(),
                booking.getUser().getEmail(),
                booking.getBook().getTitle(),
                booking.getDueAt().atStartOfDay().atOffset(ZoneOffset.UTC),
                type
        ));
    }
}
