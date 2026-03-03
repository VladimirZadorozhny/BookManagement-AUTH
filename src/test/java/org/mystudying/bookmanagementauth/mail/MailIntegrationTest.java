package org.mystudying.bookmanagementauth.mail;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.BodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mystudying.bookmanagementauth.dto.RegisterRequestDto;
import org.mystudying.bookmanagementauth.repositories.UserRepository;
import org.mystudying.bookmanagementauth.repositories.VerificationTokenRepository;
import org.mystudying.bookmanagementauth.services.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class MailIntegrationTest {


    private final UserService userService;
    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final TransactionTemplate transactionTemplate;
    private final String baseUrl;

    private GreenMail greenMail;

    public MailIntegrationTest(UserService userService, UserRepository userRepository, VerificationTokenRepository verificationTokenRepository, TransactionTemplate transactionTemplate, @Value("${app.baseUrl:http://localhost:8080}") String baseUrl) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.transactionTemplate = transactionTemplate;
        this.baseUrl = baseUrl;
    }

    @BeforeEach
    void setup() {
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();
    }

    @AfterEach
    void tearDown() {
        greenMail.stop();
        // Surgical cleanup because we are not using @Transactional at class level
        userRepository.findByEmail("mailuser@example.com").ifPresent(user -> {
            transactionTemplate.execute(status -> {
                verificationTokenRepository.deleteByUser(user);
                userRepository.delete(user);
                return null;
            });
        });
    }

    private String extractContent(MimeMessage message) throws Exception {
        Object content = message.getContent();

        if (content instanceof String str) {
            return str;
        }

        if (content instanceof MimeMultipart multipart) {
            return extractFromMimeMultipart(multipart);
        }

        return "";
    }

    private String extractFromMimeMultipart(MimeMultipart multipart) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);

            if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
                return part.getContent().toString();
            }

            if (part.getContent() instanceof MimeMultipart nestedMultipart) {
                return extractFromMimeMultipart(nestedMultipart);
            }
        }
        return "";
    }

    @Test
    void shouldSendVerificationEmailOnRegistration() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto(
                "Mail User",
                "mailuser@example.com",
                "password123"
        );

        // We MUST run this in a transaction that COMMITS to trigger @TransactionalEventListener (AFTER_COMMIT)
        transactionTemplate.execute(status -> {
            userService.register(request);
            return null;
        });

        // Wait for async mail delivery
        assertTrue(greenMail.waitForIncomingEmail(5000, 1), "Email should be received within 10 seconds");

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length);
        assertEquals("Welcome to Book Management - Please Verify Your Account", messages[0].getSubject());
        assertEquals("mailuser@example.com", messages[0].getAllRecipients()[0].toString());
// ----------------------------------------------------------
        Object content = messages[0].getContent();
        System.out.println("Content class: " + content.getClass());

        if (content instanceof MimeMultipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                System.out.println("Part " + i + " content type: " + part.getContentType());
                System.out.println("Part " + i + " class: " + part.getContent().getClass());
            }
        }
// ----------------------------------------------------------
        String body = extractContent(messages[0]);
        assertTrue(body.contains(baseUrl + "/verify?token="));
    }
}
