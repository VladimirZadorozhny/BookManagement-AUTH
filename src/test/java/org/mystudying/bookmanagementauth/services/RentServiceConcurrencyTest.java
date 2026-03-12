package org.mystudying.bookmanagementauth.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mystudying.bookmanagementauth.domain.Book;
import org.mystudying.bookmanagementauth.exceptions.BookNotAvailableException;
import org.mystudying.bookmanagementauth.exceptions.BookNotFoundException;
import org.mystudying.bookmanagementauth.exceptions.InsufficientAvailableStockException;
import org.mystudying.bookmanagementauth.repositories.BookRepository;
import org.mystudying.bookmanagementauth.repositories.BookingRepository;
import org.mystudying.bookmanagementauth.support.db.TestDataCleanup;
import org.mystudying.bookmanagementauth.support.db.TestDataHelper;
import org.mystudying.bookmanagementauth.support.db.TestFixtures;
import org.mystudying.bookmanagementauth.support.concurrency.ConcurrentTestHelper;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({UserService.class, UserBookingService.class, InventoryService.class, ConcurrentTestHelper.class, TestDataCleanup.class, TestDataHelper.class})
@Sql({"/insertConcurrentUsersTestRecords.sql", "/insertTestRecords.sql"})
public class RentServiceConcurrencyTest {

    private final JdbcClient jdbcClient;
    private final BookingRepository bookingRepository;
    private final UserBookingService userBookingService;
    private final TransactionTemplate txTemplate;
    private final BookRepository bookRepository;
    private final InventoryService inventoryService;
    private final ConcurrentTestHelper concurrentTestHelper;
    private final TestDataCleanup testDataCleanup;
    private final TestDataHelper testDataHelper;

    @MockBean
    private PasswordEncoder passwordEncoder;

    public RentServiceConcurrencyTest(JdbcClient jdbcClient,
                                      BookingRepository bookingRepository,
                                      UserBookingService userBookingService,
                                      TransactionTemplate txTemplate,
                                      BookRepository bookRepository,
                                      InventoryService inventoryService,
                                      ConcurrentTestHelper concurrentTestHelper,
                                      TestDataCleanup testDataCleanup,
                                      TestDataHelper testDataHelper) {
        this.jdbcClient = jdbcClient;
        this.bookingRepository = bookingRepository;
        this.userBookingService = userBookingService;
        this.txTemplate = txTemplate;
        this.bookRepository = bookRepository;
        this.inventoryService = inventoryService;
        this.concurrentTestHelper = concurrentTestHelper;
        this.testDataCleanup = testDataCleanup;
        this.testDataHelper = testDataHelper;
    }

    @AfterEach
    void cleanup() {
        testDataCleanup.cleanupAllTestSqlData();
    }

    //    10 Users competing for 5 Copies of Book.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentRentingShouldNotExceedAvailableStock() throws Exception {
        long bookId = testDataHelper.idOfBook(TestFixtures.BOOK_RENTABLE_TITLE);
        txTemplate.execute(status -> {
            jdbcClient.sql("DELETE FROM bookings WHERE book_id = ?").param(bookId).update();
            jdbcClient.sql("UPDATE books SET available = 5 WHERE id = ?").param(bookId).update();
            return null;
        });

        int initialAvailable = jdbcClient.sql("SELECT available FROM books WHERE id = ?").param(bookId).query(Integer.class).single();

        List<Long> userIds = testDataHelper.userIdsByEmailLike("conc.test%@example.com");
        List<Callable<HttpStatus>> tasks = new ArrayList<>();

        for (Long userId : userIds) {
            tasks.add(() -> {
                try {
                    userBookingService.rentBook(userId, bookId);
                    return HttpStatus.NO_CONTENT;
                } catch (BookNotAvailableException e) {
                    return HttpStatus.CONFLICT;
                }
            });
        }

        List<HttpStatus> results = concurrentTestHelper.runParallel(tasks, userIds.size());

        long successCount = results.stream().filter(s -> s == HttpStatus.NO_CONTENT).count();
        long conflictCount = results.stream().filter(s -> s == HttpStatus.CONFLICT).count();

        assertThat(successCount).isEqualTo(initialAvailable);
        assertThat(conflictCount).isEqualTo(userIds.size() - initialAvailable);

        Book book = bookRepository.findById(bookId).orElseThrow();
        assertThat(book.getAvailable()).isEqualTo(initialAvailable - successCount);
        assertThat(bookingRepository.countByBookId(bookId)).isEqualTo(successCount);
    }

    //    Scenario: available = 1, 5 rent attempts, 1 replenish attempt (+5). Result should be = 6 - "successful rents".
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentReplenishWithRentShouldBeSafe() throws Exception {
        long bookId = testDataHelper.idOfBook(TestFixtures.BOOK_RENTABLE_TITLE);
        txTemplate.execute(status -> {
            jdbcClient.sql("UPDATE books SET available = 1 WHERE id = ?").param(bookId).update();
            return null;
        });

        int initialAvailable = jdbcClient.sql("SELECT available FROM books WHERE id = ?").param(bookId).query(Integer.class).single();
        List<Long> userIds = testDataHelper.userIdsByEmailLike("conc.test%@example.com").stream().limit(5).toList();
        int replenishAmount = 5;

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (long userId : userIds) {
            tasks.add(() -> {
                try {
                    userBookingService.rentBook(userId, bookId);
                    return true;
                } catch (BookNotAvailableException e) {
                    return false;
                }
            });
        }

        tasks.add(() -> {
            // The replenish operation is much lighter than rentBook (no user loading,
            // no booking creation, fewer DB interactions), so in practice it often
            // reaches the database first.
            //
            // Introducing a small random delay increases the chance of different
            // interleavings (replenish happening before or after some rent attempts),
            // allowing us to observe multiple valid concurrency outcomes.
            Thread.sleep(ThreadLocalRandom.current().nextInt(0, 500));
            inventoryService.replenish(bookId, replenishAmount);
            return true; // Replenish returns void, using null as placeholder
        });

        List<Boolean> results = concurrentTestHelper.runParallel(tasks, tasks.size());

        long rentSuccessCount = results.subList(0, tasks.size() - 1).stream().filter(r -> r).count();
        boolean replenishResult = results.get(tasks.size() - 1); // The replenish task

        long finalAvailable = initialAvailable + (replenishResult ? replenishAmount : 0) - rentSuccessCount;

        Book book = bookRepository.findById(bookId).orElseThrow();
        assertThat(book.getAvailable()).isEqualTo(finalAvailable);
    }

    //    Scenario: available = 5, 4 rent attempts, 1 write-off attempt (-3). Result available shouldn't be negative.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentWriteOffWithRentShouldBeSafe() throws Exception {
        long bookId = testDataHelper.idOfBook(TestFixtures.BOOK_RENTABLE_TITLE);
        txTemplate.execute(status -> {
            jdbcClient.sql("UPDATE books SET available = 5 WHERE id = ?").param(bookId).update();
            return null;
        });

        int initialAvailable = jdbcClient.sql("SELECT available FROM books WHERE id = ?").param(bookId).query(Integer.class).single();
        List<Long> userIds = testDataHelper.userIdsByEmailLike("conc.test%@example.com").stream().limit(4).toList();
        int writeOffAmount = 3;

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (long userId : userIds) {
            tasks.add(() -> {
                try {
                    userBookingService.rentBook(userId, bookId);
                    return true;
                } catch (BookNotAvailableException e) {
                    return false;
                }
            });
        }

        tasks.add(() -> {
            try {
                // The write-off operation is much lighter than rentBook (no user loading,
                // no booking creation, fewer DB interactions), so in practice it often
                // reaches the database first.
                //
                // Introducing a small random delay increases the chance of different
                // interleavings (replenish happening before or after some rent attempts),
                // allowing us to observe multiple valid concurrency outcomes.
                Thread.sleep(ThreadLocalRandom.current().nextInt(0, 500));
                inventoryService.writeOff(bookId, writeOffAmount);
                return true; // Success
            } catch (InsufficientAvailableStockException ex) {
                return false; // Failed due to stock
            }
        });

        List<Boolean> results = concurrentTestHelper.runParallel(tasks, 5);

        // Last task is the write-off task
        boolean writeOffResult = results.get(4);
        long rentSuccessCount = results.subList(0, 4).stream().filter(r -> r).count();

        long finalAvailable = initialAvailable - (writeOffResult ? writeOffAmount : 0) - rentSuccessCount;

        Book book = bookRepository.findById(bookId).orElseThrow(() -> new BookNotFoundException(bookId));
        assertThat(book.getAvailable()).isNotNegative();
        assertThat(book.getAvailable()).isEqualTo(finalAvailable);
    }
}
