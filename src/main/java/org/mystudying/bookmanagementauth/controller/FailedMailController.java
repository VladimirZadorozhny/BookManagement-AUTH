package org.mystudying.bookmanagementauth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.mystudying.bookmanagementauth.dto.FailedMailLogDto;
import org.mystudying.bookmanagementauth.services.mail.FailedMailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/failed-mails")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Failed Mail Management", description = "Endpoints for managing failed email delivery attempts.")
public class FailedMailController {

    private final FailedMailService failedMailService;

    public FailedMailController(FailedMailService failedMailService) {

        this.failedMailService = failedMailService;
    }

    @GetMapping
    public Page<FailedMailLogDto> getFailedMails(
            @RequestParam Optional<String> toEmail,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<OffsetDateTime> start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<OffsetDateTime> end,
            Pageable pageable) {
        return failedMailService.findAll(toEmail.orElse(null), start.orElse(null), end.orElse(null), pageable);
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Void> retryMail(@PathVariable Long id) {
        if (failedMailService.retryMail(id)) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.internalServerError().build();
        }

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFailedMail(@PathVariable Long id) {
        failedMailService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
