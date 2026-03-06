package org.mystudying.bookmanagementauth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.mystudying.bookmanagementauth.domain.FailedMailLog;
import org.mystudying.bookmanagementauth.services.mail.FailedMailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    public Page<FailedMailLog> getFailedMails(Pageable pageable) {
        return failedMailService.findAll(pageable);
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
