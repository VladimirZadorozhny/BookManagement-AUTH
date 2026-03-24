package org.mystudying.bookmanagementauth.dto;

public record AdminNotificationItem(
        String bookTitle,
        String dueDate,
        String overdueDays,
        String fine
) {
}
