package org.mystudying.bookmanagementauth.events;

public record AdminUserMailRequestedEvent(
    Long userId,
    String subject,
    String body
) {
}
