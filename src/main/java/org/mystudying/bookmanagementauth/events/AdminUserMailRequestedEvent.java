package org.mystudying.bookmanagementauth.events;

public record AdminUserMailRequestedEvent(
    Long userId,
    String email,
    String subject,
    String body
) {
}
