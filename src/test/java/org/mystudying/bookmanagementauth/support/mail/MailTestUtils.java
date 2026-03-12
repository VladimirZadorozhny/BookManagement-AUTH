package org.mystudying.bookmanagementauth.support.mail;

import com.icegreen.greenmail.util.GreenMail;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Helper component for mail-related test assertions and operations.
 */
@Component
public class MailTestUtils {

    /**
     * Waits for an email and performs basic verification.
     */

    public MimeMessage verifyEmailReceived(GreenMail greenMail, String toEmail, String expectedSubject) throws MessagingException {
        // Wait for at least one email to arrive
        assertTrue(greenMail.waitForIncomingEmail(10000, 1), "Email to " + toEmail + " should be received within 10 seconds");

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertTrue(messages.length > 0, "No messages received");

        // Collect all messages for the recipient
        List<MimeMessage> recipientMessages = Arrays.stream(messages)
                .filter(m -> {
                    try {
                        // handle "<mail>" formatting by stripping <>
                        String recipient = m.getAllRecipients()[0].toString().replaceAll("[<>]", "");
                        return recipient.equalsIgnoreCase(toEmail);
                    } catch (MessagingException e) {
                        return false;
                    }
                })
                .toList();

        assertFalse(recipientMessages.isEmpty(), "No messages found for recipient: " + toEmail);

        // Find the one with the expected subject
        MimeMessage matched = recipientMessages.stream()
                .filter(m -> {
                    try {
                        return expectedSubject.equals(m.getSubject());
                    } catch (MessagingException e) {
                        return false;
                    }
                })
                .findFirst()
                .orElse(null);

        assertNotNull(matched, "No message with subject '" + expectedSubject + "' found for recipient " + toEmail);

        return matched;
    }

    /**
     * Extracts text content from a MimeMessage (handles both simple and multipart).
     */
    public String getTextFromMessage(MimeMessage message) throws MessagingException, IOException {
        Object content = message.getContent();
        if (content instanceof String) {
            return (String) content;
        } else if (content instanceof MimeMultipart) {
            return getTextFromMultipart((MimeMultipart) content);
        }
        return "";
    }

    private String getTextFromMultipart(MimeMultipart multipart) throws MessagingException, IOException {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain") || bodyPart.isMimeType("text/html")) {
                result.append(bodyPart.getContent());
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result.append(getTextFromMultipart((MimeMultipart) bodyPart.getContent()));
            }
        }
        return result.toString();
    }
}
