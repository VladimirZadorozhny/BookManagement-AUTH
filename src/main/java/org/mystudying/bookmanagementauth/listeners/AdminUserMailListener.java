package org.mystudying.bookmanagementauth.listeners;

import jakarta.mail.MessagingException;
import org.mystudying.bookmanagementauth.domain.User;
import org.mystudying.bookmanagementauth.events.AdminUserMailRequestedEvent;
import org.mystudying.bookmanagementauth.exceptions.UserNotFoundException;
import org.mystudying.bookmanagementauth.repositories.UserRepository;
import org.mystudying.bookmanagementauth.services.mail.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;

@Component
public class AdminUserMailListener {

    private static final Logger log = LoggerFactory.getLogger(AdminUserMailListener.class);
    private final UserRepository userRepository;
    private final MailService mailService;

    public AdminUserMailListener(UserRepository userRepository, MailService mailService) {
        this.userRepository = userRepository;
        this.mailService = mailService;
    }

    @Async("mailExecutor")
    @EventListener
    @Transactional
    @Retryable(
            retryFor = MessagingException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void handleAdminUserMail(AdminUserMailRequestedEvent event) throws MessagingException {
        User user = userRepository.findById(event.userId())
                .orElseThrow(() -> new UserNotFoundException(event.userId()));

        log.info("Sending admin mail to user {} with subject: {}", user.getEmail(), event.subject());
        mailService.send(user.getEmail(), event.subject(), event.body());
        log.info("Admin mail sent to user {}", user.getEmail());
    }

    @Recover
    public void recover(MessagingException e, AdminUserMailRequestedEvent event) {
        log.error("Failed to send admin mail to user ID {} after multiple retries: {}", event.userId(), e.getMessage());
    }
}
