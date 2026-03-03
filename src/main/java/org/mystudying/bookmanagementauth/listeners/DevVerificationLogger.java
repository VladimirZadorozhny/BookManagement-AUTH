package org.mystudying.bookmanagementauth.listeners;

import org.mystudying.bookmanagementauth.events.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevVerificationLogger {

    private static final Logger log = LoggerFactory.getLogger(DevVerificationLogger.class);

    @Value("${app.baseUrl:http://localhost:8080}")
    private String baseUrl;

    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        String verificationLink = String.format("%s/verify?token=%s", baseUrl, event.token());
        log.warn("DEV PROFILE: Verification link for {} is: {}", event.email(), verificationLink);
    }
}
