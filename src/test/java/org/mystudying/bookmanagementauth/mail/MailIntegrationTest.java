package org.mystudying.bookmanagementauth.mail;

import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mystudying.bookmanagementauth.dto.RegisterRequestDto;
import org.mystudying.bookmanagementauth.dto.UserDto;
import org.mystudying.bookmanagementauth.domain.ReminderType;
import org.mystudying.bookmanagementauth.events.BookingReminderEvent;
import org.mystudying.bookmanagementauth.services.AdminMailService;
import org.mystudying.bookmanagementauth.services.ReminderProcessingService;
import org.mystudying.bookmanagementauth.support.AbstractSecurityIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.time.LocalDate;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for various mail templates and event wiring.
 * Note: These tests use Propagation.NOT_SUPPORTED to allow transactions to commit
 * and trigger @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT).
 */
public class MailIntegrationTest extends AbstractSecurityIntegrationTest {

    private static final String TEST_PASSWORD_HASH = "{bcrypt}$2a$10$W8Fh/h9nADK75zl/zXWAeOsq43iLzwrtbeLi/HnZdYwUhzjbOT2Ra";

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private AdminMailService adminMailService;

    @Autowired
    private ReminderProcessingService reminderProcessingService;

    @Value("${app.baseUrl:http://localhost:8080}")
    private String baseUrl;

    private GreenMail greenMail;

    @BeforeEach
    void setup() throws FolderException {
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();
        greenMail.purgeEmailFromAllMailboxes();
    }

    @AfterEach
    void tearDown() {
        greenMail.stop();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldSendVerificationEmailOnRegistration() throws Exception {
        String email = "mail.test" + UUID.randomUUID() + "@example.com";
        RegisterRequestDto request = new RegisterRequestDto("Mail User", email, "password123");

        // Transaction must commit to trigger listener
        transactionTemplate.execute(status -> {
            authLifecycleService.register(request);
            return null;
        });

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            MimeMessage message = mailTestUtils.verifyEmailReceived(greenMail, email, "Welcome to Book Management - Please Verify Your Account");
            String body = mailTestUtils.getTextFromMessage(message);
            assertTrue(body.contains(baseUrl + "/verify?token="));

        });

        testDataCleanup.deleteUserCascade(email);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldSendPasswordResetEmail() throws Exception {
        // GIVEN: A verified user exists
        String email = "mail.test" + UUID.randomUUID() + "@example.com";
        transactionTemplate.execute(status -> {
            signupAndVerify("Mail User", email, "password123");
            return null;
        });

        // WHEN: Requesting password reset
        transactionTemplate.execute(status -> {
            authLifecycleService.requestPasswordReset(email);
            return null;
        });

        // THEN: Email received
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            MimeMessage message = mailTestUtils.verifyEmailReceived(greenMail, email, "Password Reset Request for Book Management");
            String body = mailTestUtils.getTextFromMessage(message);
            assertTrue(body.contains(baseUrl + "/reset-password?token="));

        });
        testDataCleanup.deleteUserCascade(email);

    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldSendLoginAlertEmail() throws Exception {
        // GIVEN: A verified user
        String email = "mail.test" + UUID.randomUUID() + "@example.com";
        transactionTemplate.execute(status -> {
            signupAndVerify("Mail User", email, "password123");
            return null;
        });

        // WHEN: Logging in (Success handler triggers the event)
        loginAs(email, "password123");

        // THEN: Login alert email received
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            mailTestUtils.verifyEmailReceived(greenMail, email, "Security Alert: New Login to Your Account");

        });
        testDataCleanup.deleteUserCascade(email);

    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldSendAdminCustomEmail() throws Exception {
        // GIVEN: A target user
        String email = "mail.test" + UUID.randomUUID() + "@example.com";
        UserDto user = transactionTemplate.execute(status -> signupAndVerify("Mail User", email, "password123"));

        // WHEN: Admin sends a custom mail
        transactionTemplate.execute(status -> {
            adminMailService.sendMailToUser(user.id(), "Admin Subject", "Admin Message Body");
            return null;
        });

        // THEN: Email received
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            MimeMessage message = mailTestUtils.verifyEmailReceived(greenMail, email, "Admin Subject");
            String body = mailTestUtils.getTextFromMessage(message);
            assertTrue(body.contains("Admin Message Body"));

        });
        testDataCleanup.deleteUserCascade(email);

    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldSendReminderEmailsForAllTypes() throws Exception {
        String email = "mail.test" + UUID.randomUUID() + "@example.com";
        String userName = "Mail User";
        String bookTitle = "Reminder Book";
        LocalDate dueDate = LocalDate.now().plusDays(3);
        long bookingId = Math.abs(UUID.randomUUID().getMostSignificantBits());

        transactionTemplate.execute(status -> {
            reminderProcessingService.processReminder(new BookingReminderEvent(
                    bookingId,
                    userName,
                    email,
                    bookTitle,
                    dueDate,
                    ReminderType.THREE_DAYS_LEFT
            ));
            reminderProcessingService.processReminder(new BookingReminderEvent(
                    bookingId,
                    userName,
                    email,
                    bookTitle,
                    dueDate,
                    ReminderType.DUE_TODAY
            ));
            reminderProcessingService.processReminder(new BookingReminderEvent(
                    bookingId,
                    userName,
                    email,
                    bookTitle,
                    dueDate,
                    ReminderType.OVERDUE
            ));
            return null;
        });

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            MimeMessage message = mailTestUtils.verifyEmailReceived(greenMail, email,
                    "Reminder: Your book '" + bookTitle + "' is due soon!");
            String body = mailTestUtils.getTextFromMessage(message);
            assertTrue(body.contains("Book Due Soon"));
            assertTrue(body.contains("Please return it on time to avoid any late fees."));
        });

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            MimeMessage message = mailTestUtils.verifyEmailReceived(greenMail, email,
                    "Reminder: Your book '" + bookTitle + "' is due today!");
            String body = mailTestUtils.getTextFromMessage(message);
            assertTrue(body.contains("Book Due Today"));
            assertTrue(body.contains("Kindly return it as soon as possible."));
        });

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            MimeMessage message = mailTestUtils.verifyEmailReceived(greenMail, email,
                    "Action Required: Your book '" + bookTitle + "' is overdue!");
            String body = mailTestUtils.getTextFromMessage(message);
            assertTrue(body.contains("Book Overdue"));
            assertTrue(body.contains("Please return the book immediately to minimize any accumulated fines."));
        });

        testDataCleanup.deleteRemindersByBookingId(bookingId);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldSendHeavyUsersNotificationWithAggregatedBooks() throws Exception {
        String email = "heavy.mail" + UUID.randomUUID() + "@example.com";
        String userName = "Heavy User";
        String authorName = "Mail Author Heavy";
        String bookTitle1 = "Mail Heavy Book 1";
        String bookTitle2 = "Mail Heavy Book 2";
        try {

            transactionTemplate.execute(status -> {
                long userId = insertUser(userName, email);
                long authorId = insertAuthor(authorName);
                long bookId1 = insertBook(bookTitle1, authorId);
                long bookId2 = insertBook(bookTitle2, authorId);
                insertBooking(userId, bookId1, LocalDate.now().minusDays(1), LocalDate.now().plusDays(7), null, 0.0, false);
                insertBooking(userId, bookId2, LocalDate.now().minusDays(2), LocalDate.now().plusDays(10), null, 0.0, false);

                adminMailService.sendBulkNotificationToHeavyUsers(2L);
                return null;
            });

            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                MimeMessage message = mailTestUtils.verifyEmailReceived(greenMail, email,
                        "Library Notice: Multiple Active Borrowings");
                String body = mailTestUtils.getTextFromMessage(message);
                assertTrue(body.contains("Active Borrowings Summary"));
                assertTrue(body.contains(bookTitle1));
                assertTrue(body.contains(bookTitle2));
                assertEquals(1, mailTestUtils.countMessagesForRecipient(greenMail, email));
            });
        } finally {

            testDataCleanup.deleteUserCascade(email);
            deleteBookByTitle(bookTitle1);
            deleteBookByTitle(bookTitle2);
            deleteAuthorByName(authorName);
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldSendAggregatedUnpaidFinesNotificationWithCalculatedFine() throws Exception {
        String email = "overdue.mail" + UUID.randomUUID() + "@example.com";
        String userName = "Overdue User";
        String authorName = "Mail Author Overdue";
        String bookTitle = "Mail Overdue Book";

        try {

            transactionTemplate.execute(status -> {
                long userId = insertUser(userName, email);
                long authorId = insertAuthor(authorName);
                long bookId = insertBook(bookTitle, authorId);
                insertBooking(userId, bookId, LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), null, 0.0, false);

                adminMailService.sendBulkNotificationToOverdueUsers();
                return null;
            });

            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                MimeMessage message = mailTestUtils.verifyEmailReceived(greenMail, email,
                        "Action Required: Overdue Books");
                String body = mailTestUtils.getTextFromMessage(message);
                assertTrue(body.contains("Overdue Books Notice"));
                assertTrue(body.contains(bookTitle));
            });
        } finally {
            testDataCleanup.deleteUserCascade(email);
            deleteBookByTitle(bookTitle);
            deleteAuthorByName(authorName);
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldSendUnpaidFinesNotification() throws Exception {
        String email = "fine.mail" + UUID.randomUUID() + "@example.com";
        String userName = "Fine User";
        String authorName = "Mail Author Fine";
        String bookTitle = "Mail Fine Book";
        int overdueDays = 6;

        try {

            transactionTemplate.execute(status -> {
                long userId = insertUser(userName, email);
                long authorId = insertAuthor(authorName);
                long bookId = insertBook(bookTitle, authorId);
                insertBooking(userId, bookId, LocalDate.now().minusDays(20), LocalDate.now().minusDays(overdueDays), null, 0, false);  // 6 days overdue, fine must be 6.00

                adminMailService.sendBulkNotificationToUnpaidFinesUsers();
                return null;
            });

            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                MimeMessage message = mailTestUtils.verifyEmailReceived(greenMail, email,
                        "Action Required: Unpaid Fines");
                String body = mailTestUtils.getTextFromMessage(message);
                assertTrue(body.contains("Unpaid Fines Notice"));
                assertTrue(body.contains("6.00"));
                assertTrue(body.contains(bookTitle));
            });
        } finally {
            testDataCleanup.deleteUserCascade(email);
            deleteBookByTitle(bookTitle);
            deleteAuthorByName(authorName);
        }
    }

    private long insertUser(String name, String email) {
        jdbcClient.sql("INSERT INTO users(name, email, password, active) VALUES (?, ?, ?, ?)")
                .params(name, email, TEST_PASSWORD_HASH, true)
                .update();
        return jdbcClient.sql("SELECT id FROM users WHERE email = ?")
                .param(email)
                .query(Long.class)
                .single();
    }

    private long insertAuthor(String name) {
        jdbcClient.sql("INSERT INTO authors(name, birthdate) VALUES (?, ?)")
                .params(name, LocalDate.of(1970, 1, 1))
                .update();
        return jdbcClient.sql("SELECT id FROM authors WHERE name = ?")
                .param(name)
                .query(Long.class)
                .single();
    }

    private long insertBook(String title, long authorId) {
        jdbcClient.sql("INSERT INTO books(title, year, author_id, available) VALUES (?, ?, ?, ?)")
                .params(title, 2024, authorId, 1)
                .update();
        return jdbcClient.sql("SELECT id FROM books WHERE title = ?")
                .param(title)
                .query(Long.class)
                .single();
    }

    private void insertBooking(long userId,
                               long bookId,
                               LocalDate borrowedAt,
                               LocalDate dueAt,
                               LocalDate returnedAt,
                               double fine,
                               boolean finePaid) {
        jdbcClient.sql("INSERT INTO bookings(user_id, book_id, borrowed_at, due_at, returned_at, fine, fine_paid) VALUES (?, ?, ?, ?, ?, ?, ?)")
                .params(userId, bookId, borrowedAt, dueAt, returnedAt, fine, finePaid)
                .update();
    }

    private void deleteBookByTitle(String title) {
        jdbcClient.sql("DELETE FROM book_genres WHERE book_id IN (SELECT id FROM books WHERE title = ?)")
                .param(title).update();
        jdbcClient.sql("DELETE FROM bookings WHERE book_id IN (SELECT id FROM books WHERE title = ?)")
                .param(title).update();
        jdbcClient.sql("DELETE FROM books WHERE title = ?")
                .param(title).update();
    }

    private void deleteAuthorByName(String name) {
        jdbcClient.sql("DELETE FROM authors WHERE name = ?")
                .param(name).update();
    }
}
