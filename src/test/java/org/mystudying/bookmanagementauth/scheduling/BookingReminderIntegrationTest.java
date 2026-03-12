package org.mystudying.bookmanagementauth.scheduling;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mystudying.bookmanagementauth.domain.BookingReminderLog;
import org.mystudying.bookmanagementauth.domain.ReminderType;
import org.mystudying.bookmanagementauth.events.BookingReminderEvent;
import org.mystudying.bookmanagementauth.listeners.BookingReminderListener;
import org.mystudying.bookmanagementauth.repositories.BookingReminderLogRepository;
import org.mystudying.bookmanagementauth.services.mail.MailService;
import org.mystudying.bookmanagementauth.support.AbstractSecurityIntegrationTest;
import org.mystudying.bookmanagementauth.support.db.TestFixtures;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@Sql("/insertTestRecords.sql")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class BookingReminderIntegrationTest extends AbstractSecurityIntegrationTest {

    @Autowired
    private BookingReminderListener bookingReminderListener;

    @Autowired
    private BookingReminderLogRepository bookingReminderLogRepository;

    @MockBean
    private MailService mailService;

    private long bookingId;

    @BeforeEach
    void setup() {
        long userId = testDataHelper.idOfUser(TestFixtures.USER_1_EMAIL);
        long bookId = testDataHelper.idOfBook(TestFixtures.BOOK_1_TITLE);
        this.bookingId = testDataHelper.idOfBooking(userId, bookId);

        // Pre-clean in case of previous failed run
        testDataCleanup.deleteRemindersByBookingId(bookingId);
    }

    @AfterEach
    void cleanup() {
        // Complete surgical cleanup of SQL records and side-effect logs
        testDataCleanup.deleteRemindersByBookingId(bookingId);
        testDataCleanup.cleanupAllTestSqlData();
    }

    @Test
    void shouldNotSendEmailIfAlreadyLogged() throws Exception {
        // GIVEN: A reminder is already logged for this specific booking and type
        bookingReminderLogRepository.save(new BookingReminderLog(bookingId, ReminderType.DUE_TODAY, OffsetDateTime.now()));

        BookingReminderEvent event = new BookingReminderEvent(
                bookingId,
                "Test User 1",
                TestFixtures.USER_1_EMAIL,
                TestFixtures.BOOK_1_TITLE,
                OffsetDateTime.now(),
                ReminderType.DUE_TODAY
        );

        // WHEN: Triggering the listener
        bookingReminderListener.handleBookingReminder(event);

        // THEN: Mail should NOT be sent
        await().during(2, SECONDS).atMost(3, SECONDS).untilAsserted(() ->
                verify(mailService, never()).send(anyString(), anyString(), anyString())
        );

        assertEquals(
                1,
                bookingReminderLogRepository.countByBookingIdAndReminderType(
                        bookingId,
                        ReminderType.DUE_TODAY
                )
        );
    }
}
