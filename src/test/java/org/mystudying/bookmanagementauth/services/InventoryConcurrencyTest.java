package org.mystudying.bookmanagementauth.services;

import org.junit.jupiter.api.Test;
import org.mystudying.bookmanagementauth.domain.Book;
import org.mystudying.bookmanagementauth.exceptions.BookNotAvailableException;
import org.mystudying.bookmanagementauth.exceptions.BookNotFoundException;
import org.mystudying.bookmanagementauth.exceptions.InsufficientAvailableStockException;
import org.mystudying.bookmanagementauth.repositories.BookRepository;
import org.mystudying.bookmanagementauth.support.concurrency.ConcurrentTestHelper;
import org.mystudying.bookmanagementauth.support.db.TestDataCleanup;
import org.mystudying.bookmanagementauth.support.db.TestDataHelper;
import org.mystudying.bookmanagementauth.support.db.TestFixtures;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import({InventoryService.class, TestDataHelper.class, TestDataCleanup.class, ConcurrentTestHelper.class})
@Sql({"/insertTestRecords.sql"})
public class InventoryConcurrencyTest {

    private final JdbcClient jdbcClient;
    private final InventoryService inventoryService;
    private final TransactionTemplate txTemplate;
    private final BookRepository bookRepository;
    private final TestDataHelper testDataHelper;
    private final TestDataCleanup testDataCleanup;
    private final ConcurrentTestHelper concurrentTestHelper;

    public InventoryConcurrencyTest(JdbcClient jdbcClient,
                                    InventoryService inventoryService,
                                    TransactionTemplate txTemplate,
                                    BookRepository bookRepository,
                                    TestDataHelper testDataHelper,
                                    TestDataCleanup testDataCleanup, ConcurrentTestHelper concurrentTestHelper) {
        this.jdbcClient = jdbcClient;
        this.inventoryService = inventoryService;
        this.txTemplate = txTemplate;
        this.bookRepository = bookRepository;
        this.testDataHelper = testDataHelper;
        this.testDataCleanup = testDataCleanup;
        this.concurrentTestHelper = concurrentTestHelper;
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentReturnsShouldIncreaseStockCorrectly() throws Exception {
        long bookId = testDataHelper.idOfBook(TestFixtures.BOOK_RENTABLE_TITLE);
        try {
            txTemplate.execute(status -> {
                jdbcClient.sql("UPDATE books SET available = 0 WHERE id = ?").param(bookId).update();
                return null;
            });

            int initialAvailable = jdbcClient.sql("SELECT available FROM books WHERE id = ?").param(bookId).query(Integer.class).single();

            int threadCount = 10;

            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                tasks.add(() -> {
                    try {
                        inventoryService.incrementStock(bookId);
                        return true;
                    } catch (BookNotAvailableException e) {
                        return false;
                    }
                });
            }

            List<Boolean> results = concurrentTestHelper.runParallel(tasks, tasks.size());
            long incrementSuccessCount = results.stream().filter(r -> r).count();

            Book book = bookRepository.findById(bookId).orElseThrow(() -> new BookNotFoundException(bookId));
            assertThat(book.getAvailable()).isEqualTo(initialAvailable + incrementSuccessCount);
        } finally {
            testDataCleanup.cleanupAllTestSqlData();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void writeOffFailsIfNotEnoughStock() {
        long bookId = testDataHelper.idOfBook(TestFixtures.BOOK_RENTABLE_TITLE);
        try {
            txTemplate.execute(status -> {
                jdbcClient.sql("UPDATE books SET available = 2 WHERE id = ?").param(bookId).update();
                return null;
            });

            assertThrows(InsufficientAvailableStockException.class, () -> {
                inventoryService.writeOff(bookId, 5);
            });

            Book book = bookRepository.findById(bookId).orElseThrow(() -> new BookNotFoundException(bookId));
            assertThat(book.getAvailable()).isEqualTo(2);
        } finally {
            testDataCleanup.cleanupAllTestSqlData();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentWriteOffsShouldBeSafe() throws Exception {
        long bookId = testDataHelper.idOfBook(TestFixtures.BOOK_RENTABLE_TITLE);
        try {
            txTemplate.execute(status -> {
                jdbcClient.sql("UPDATE books SET available = 10 WHERE id = ?").param(bookId).update();
                return null;
            });

            int attempts = 5;
            int writeOffAmount = 3; // Total 15 requested, only 10 available
            int initialAvailable = jdbcClient.sql("SELECT available FROM books WHERE id = ?").param(bookId).query(Integer.class).single();


            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int i = 0; i < attempts; i++) {
                tasks.add(() -> {
                    try {
                        inventoryService.writeOff(bookId, writeOffAmount);
                        return true;
                    } catch (InsufficientAvailableStockException e) {
                        return false;
                    }
                });
            }

            List<Boolean> results = concurrentTestHelper.runParallel(tasks, tasks.size());
            long writeOffSuccessCount = results.stream().filter(r -> r).count();

            assertThat(writeOffSuccessCount).isEqualTo(initialAvailable / writeOffAmount); // 3 * 3 = 9. 4th would be 12 > 10.
            Book book = bookRepository.findById(bookId).orElseThrow(() -> new BookNotFoundException(bookId));
            assertThat(book.getAvailable()).isEqualTo(initialAvailable - writeOffSuccessCount * writeOffAmount); // 10 - 9 = 1
        } finally {
            testDataCleanup.cleanupAllTestSqlData();
        }
    }


}
