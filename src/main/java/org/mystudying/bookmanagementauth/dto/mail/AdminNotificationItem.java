package org.mystudying.bookmanagementauth.dto.mail;

public record AdminNotificationItem(
        String bookTitle,
        String dueDate,
        String overdueDays,
        String fine
) {
}
