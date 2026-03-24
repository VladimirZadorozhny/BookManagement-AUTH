package org.mystudying.bookmanagementauth.services;

import org.mystudying.bookmanagementauth.domain.Booking;
import org.mystudying.bookmanagementauth.domain.User;
import org.mystudying.bookmanagementauth.dto.AdminNotificationItem;
import org.mystudying.bookmanagementauth.dto.AdminNotificationType;
import org.mystudying.bookmanagementauth.events.AdminUserMailRequestedEvent;
import org.mystudying.bookmanagementauth.exceptions.NonRetryableMailException;
import org.mystudying.bookmanagementauth.repositories.BookingRepository;
import org.mystudying.bookmanagementauth.repositories.UserRepository;
import org.mystudying.bookmanagementauth.services.mail.MailTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminMailService {

    private static final Logger log = LoggerFactory.getLogger(AdminMailService.class);
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MailTemplateService mailTemplateService;

    public AdminMailService(BookingRepository bookingRepository,
                            UserRepository userRepository,
                            ApplicationEventPublisher eventPublisher,
                            MailTemplateService mailTemplateService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.mailTemplateService = mailTemplateService;
    }

    public void sendBulkMailToHeavyUsers(String subject, String body, Long minBooksBorrowed) {
        List<Booking> heavyUserBookings = bookingRepository.findHeavyUserBookingsForMailing(minBooksBorrowed);

        heavyUserBookings.stream()
                .map(Booking::getUser)
                .distinct()
                .forEach(user -> publishAdminUserMailEvent(user.getId(), user.getEmail(), subject, body));
    }

    public void sendBulkMailToOverdueUsers(String subject, String body) {
        List<User> overdueUsers = bookingRepository.findOverdueBookingsForMailing(LocalDate.now(ZoneOffset.UTC))
                .stream()
                .map(Booking::getUser)
                .distinct()
                .toList();

        overdueUsers.forEach(user -> publishAdminUserMailEvent(user.getId(), user.getEmail(), subject, body));
    }

    public void sendBulkMailToUnpaidFinesUsers(String subject, String body) {
        List<User> users = bookingRepository.findUnpaidFinesBookingsForMailing(LocalDate.now(ZoneOffset.UTC))
                .stream()
                .map(Booking::getUser)
                .distinct()
                .toList();

        users.forEach(user -> publishAdminUserMailEvent(user.getId(), user.getEmail(), subject, body));
    }

    public void sendMailToUser(Long userId, String subject, String body) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NonRetryableMailException("User not found with ID: " + userId));
        publishAdminUserMailEvent(user.getId(), user.getEmail(), subject, body);
    }

    public void sendBulkNotificationToHeavyUsers(Long minBooksBorrowed) {
        List<Booking> bookings = bookingRepository.findHeavyUserBookingsForMailing(minBooksBorrowed);
        sendGroupedNotifications(AdminNotificationType.HEAVY_USERS, bookings, minBooksBorrowed);
    }

    public void sendBulkNotificationToOverdueUsers() {
        List<Booking> bookings = bookingRepository.findOverdueBookingsForMailing(LocalDate.now(ZoneOffset.UTC));
        sendGroupedNotifications(AdminNotificationType.OVERDUE, bookings, null);
    }

    public void sendBulkNotificationToUnpaidFinesUsers() {
        List<Booking> bookings = bookingRepository.findUnpaidFinesBookingsForMailing(LocalDate.now(ZoneOffset.UTC));
        sendGroupedNotifications(AdminNotificationType.UNPAID_FINES, bookings, null);
    }


    private void publishAdminUserMailEvent(Long userId, String email, String subject, String body) {
        log.debug("Publishing admin mail event for user {}", userId);
        eventPublisher.publishEvent(new AdminUserMailRequestedEvent(userId, email, subject, body));
    }

    private void sendGroupedNotifications(AdminNotificationType type, List<Booking> bookings, Long minBooksBorrowed) {
        if (bookings.isEmpty()) {
            return;
        }

        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        Map<Long, List<Booking>> byUser = bookings.stream()
                .collect(Collectors.groupingBy(b -> b.getUser().getId()));

        for (List<Booking> userBookings : byUser.values()) {
            User user = userBookings.get(0).getUser();
            List<AdminNotificationItem> items = buildItems(type, userBookings, now);
            if (items.isEmpty()) {
                continue;
            }
            String summary = buildSummary(type, items, minBooksBorrowed);
            String subject = mailTemplateService.getAdminNotificationSubject(type);
            String body = mailTemplateService.buildAdminNotificationMail(user.getName(), type, items, summary);
            publishAdminUserMailEvent(user.getId(), user.getEmail(), subject, body);
        }
    }

    private List<AdminNotificationItem> buildItems(AdminNotificationType type, List<Booking> bookings, LocalDate now) {
        return bookings.stream()
                .map(booking -> {
                    String dueDate = booking.getDueAt() != null ? mailTemplateService.formatDate(booking.getDueAt()) : "-";
                    long overdueDaysValue = calculateOverdueDays(booking, now);
                    String overdueDays = overdueDaysValue > 0 ? overdueDaysValue + " days" : "-";
                    String fine = "-";
                    if (type == AdminNotificationType.UNPAID_FINES) {
                        BigDecimal fineValue = resolveFine(booking, now);
                        fine = formatCurrency(fineValue);
                    }
                    return new AdminNotificationItem(booking.getBook().getTitle(), dueDate, overdueDays, fine);
                })
                .toList();
    }

    private String buildSummary(AdminNotificationType type, List<AdminNotificationItem> items, Long minBooksBorrowed) {
        switch (type) {
            case HEAVY_USERS:
                String threshold = minBooksBorrowed != null ? " (threshold " + minBooksBorrowed + ")" : "";
                return "You currently have " + items.size() + " active borrowed book(s)" + threshold + ".";
            case OVERDUE:
                return "You have " + items.size() + " overdue book(s). Please return them as soon as possible.";
            case UNPAID_FINES:
                BigDecimal totalFine = items.stream()
                        .map(AdminNotificationItem::fine)
                        .map(this::parseCurrency)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                String totalFineDisplay = totalFine.compareTo(BigDecimal.ZERO) > 0
                        ? formatCurrency(totalFine)
                        : "$0.00";
                return "You have " + items.size() + " booking(s) with unpaid or accruing fines. Total outstanding fine: "
                        + totalFineDisplay + ".";
            default:
                return null;
        }
    }

    private long calculateOverdueDays(Booking booking, LocalDate now) {
        if (booking.getDueAt() == null) {
            return 0;
        }
        if (booking.getReturnedAt() != null) {
            if (booking.getReturnedAt().isAfter(booking.getDueAt())) {
                return java.time.temporal.ChronoUnit.DAYS.between(booking.getDueAt(), booking.getReturnedAt());
            }
            return 0;
        }
        if (now.isAfter(booking.getDueAt())) {
            return java.time.temporal.ChronoUnit.DAYS.between(booking.getDueAt(), now);
        }
        return 0;
    }

    private BigDecimal resolveFine(Booking booking, LocalDate now) {
        BigDecimal fine = booking.getFine() == null ? BigDecimal.ZERO : booking.getFine();
        if (fine.compareTo(BigDecimal.ZERO) == 0 && booking.getReturnedAt() == null && now.isAfter(booking.getDueAt())) {
            fine = booking.calculateFine();
        }
        return fine;
    }

    private String formatCurrency(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return "-";
        }
        return "$" + value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal parseCurrency(String value) {
        if (value == null || value.equals("-")) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.replace("$", ""));
    }
}
