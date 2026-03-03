package org.mystudying.bookmanagementauth.events;

import java.time.LocalDateTime;

/**
 * Event published upon successful user login.
 */
public record LoginSuccessEvent(String email, String name, String ip, LocalDateTime loginTime) {
}
