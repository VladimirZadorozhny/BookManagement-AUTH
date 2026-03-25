package org.mystudying.bookmanagementauth.services.mail;

import org.mystudying.bookmanagementauth.domain.FailedMailLog;
import org.mystudying.bookmanagementauth.dto.mail.FailedMailLogDto;
import org.mystudying.bookmanagementauth.repositories.FailedMailLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Transactional(readOnly = true)
public class FailedMailService {

    private static final Logger log = LoggerFactory.getLogger(FailedMailService.class);
    private final FailedMailLogRepository failedMailLogRepository;
    private final MailService mailService;

    public FailedMailService(FailedMailLogRepository failedMailLogRepository, MailService mailService) {
        this.failedMailLogRepository = failedMailLogRepository;
        this.mailService = mailService;
    }

    @Transactional
    public void logFailedMail(String to, String subject, String body, String error) {
        log.info("Logging failed mail to {} with subject: {}", to, subject);
        FailedMailLog failedMail = new FailedMailLog(to, subject, body, error);
        failedMailLogRepository.save(failedMail);
    }


    public Page<FailedMailLogDto> findAll(String toEmail, OffsetDateTime start, OffsetDateTime end, Pageable pageable) {
        String normalizedEmail = toEmail != null ? toEmail.toLowerCase() : null;
        return failedMailLogRepository.findByFilters(normalizedEmail, start, end, pageable)
                .map(this::toDto);
    }

    @Transactional
    public void deleteById(Long id) {
        failedMailLogRepository.deleteById(id);
    }

    @Transactional
    public boolean retryMail(Long id) {
        FailedMailLog logEntry = failedMailLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Failed mail log entry not found with ID: " + id));

        try {
            log.info("Manually retrying failed mail to {} with subject: {}", logEntry.getToEmail(), logEntry.getSubject());
            mailService.send(logEntry.getToEmail(), logEntry.getSubject(), logEntry.getBody());

            // If successful, delete the log entry
            failedMailLogRepository.deleteById(id);
            log.info("Retry successful. Log entry deleted.");
            return true;
        } catch (Exception e) {
            log.error("Retry failed for mail to {}: {}", logEntry.getToEmail(), e.getMessage());
            logEntry.setAttemptCount(logEntry.getAttemptCount() + 1);
            logEntry.setLastAttemptAt(OffsetDateTime.now());
            logEntry.setErrorMessage(e.getMessage());
            return false;
        }
    }

    private FailedMailLogDto toDto(FailedMailLog log) {
        return new FailedMailLogDto(
                log.getId(),
                log.getToEmail(),
                log.getSubject(),
                log.getBody(),
                log.getErrorMessage(),
                log.getCreatedAt(),
                log.getLastAttemptAt(),
                log.getAttemptCount()
        );
    }
}
