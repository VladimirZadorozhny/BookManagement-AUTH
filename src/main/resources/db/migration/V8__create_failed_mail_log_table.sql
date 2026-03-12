CREATE TABLE failed_mail_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    to_email VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    error_message TEXT,
    created_at DATETIME NOT NULL,
    last_attempt_at DATETIME NOT NULL,
    attempt_count INT NOT NULL
);
