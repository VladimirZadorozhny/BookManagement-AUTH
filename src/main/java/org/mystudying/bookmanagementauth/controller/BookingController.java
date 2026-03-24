package org.mystudying.bookmanagementauth.controller;

import jakarta.validation.Valid;
import org.mystudying.bookmanagementauth.dto.AdminMailRequestDto;
import org.mystudying.bookmanagementauth.dto.BookingReportDto;
import org.mystudying.bookmanagementauth.dto.BookingReportType;
import org.mystudying.bookmanagementauth.services.AdminMailService;
import org.mystudying.bookmanagementauth.services.BookingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class BookingController {

    private final BookingService bookingService;
    private final AdminMailService adminMailService;

    public BookingController(BookingService bookingService, AdminMailService adminMailService) {
        this.bookingService = bookingService;
        this.adminMailService = adminMailService;
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<BookingReportDto> getBookingReport(
            @RequestParam(name = "type") BookingReportType type,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Long minActiveBooks,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return bookingService.getBookingReport(type, days, minActiveBooks, pageable);
    }

    @PostMapping("/notify-heavy-users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> notifyHeavyUsers(@Valid @RequestBody AdminMailRequestDto requestDto) {
        adminMailService.sendBulkMailToHeavyUsers(
                requestDto.subject(),
                requestDto.body(),
                requestDto.minBooksBorrowed() != null ? requestDto.minBooksBorrowed() : 5L
        );
        return ResponseEntity.ok("Notifications queued for heavy users.");
    }

    @PostMapping("/notify-heavy-users-auto")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> notifyHeavyUsersAuto(@RequestParam(required = false) Long minActiveBooks) {
        adminMailService.sendBulkNotificationToHeavyUsers(minActiveBooks != null ? minActiveBooks : 5L);
        return ResponseEntity.ok("Notifications queued for heavy users.");
    }

    @PostMapping("/notify-overdue-users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> notifyOverdueUsers(@Valid @RequestBody AdminMailRequestDto requestDto) {
        adminMailService.sendBulkMailToOverdueUsers(requestDto.subject(), requestDto.body());
        return ResponseEntity.ok("Notifications queued for users with overdue books.");
    }

    @PostMapping("/notify-overdue-users-auto")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> notifyOverdueUsersAuto() {
        adminMailService.sendBulkNotificationToOverdueUsers();
        return ResponseEntity.ok("Notifications queued for users with overdue books.");
    }

    @PostMapping("/notify-unpaidfines-users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> notifyUnpaidFinesUsers(@Valid @RequestBody AdminMailRequestDto requestDto) {
        adminMailService.sendBulkMailToUnpaidFinesUsers(requestDto.subject(), requestDto.body());
        return ResponseEntity.ok("Notifications queued for users with unpaid fines.");
    }

    @PostMapping("/notify-unpaid-fines-users-auto")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> notifyUnpaidFinesUsersAuto() {
        adminMailService.sendBulkNotificationToUnpaidFinesUsers();
        return ResponseEntity.ok("Notifications queued for users with unpaid fines.");
    }


    @PostMapping("/notify-user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> notifyUser(@PathVariable Long userId, @Valid @RequestBody AdminMailRequestDto requestDto) {
        adminMailService.sendMailToUser(userId, requestDto.subject(), requestDto.body());
        return ResponseEntity.ok("Notification queued for user.");
    }
}
