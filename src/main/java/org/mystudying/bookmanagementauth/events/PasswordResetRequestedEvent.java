package org.mystudying.bookmanagementauth.events;

public record PasswordResetRequestedEvent(String email, String token) {
}
