package org.mystudying.bookmanagementauth.events;

public record UserRegisteredEvent(Long userId, String email, String name, String token) {
}
