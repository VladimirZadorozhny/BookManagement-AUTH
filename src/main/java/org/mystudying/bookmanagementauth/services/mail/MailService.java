package org.mystudying.bookmanagementauth.services.mail;

public interface MailService {
    void send(String to, String subject, String body);
}
