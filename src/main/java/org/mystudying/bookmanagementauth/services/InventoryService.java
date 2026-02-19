package org.mystudying.bookmanagementauth.services;

import org.mystudying.bookmanagementauth.exceptions.BookNotAvailableException;
import org.mystudying.bookmanagementauth.exceptions.BookNotFoundException;
import org.mystudying.bookmanagementauth.exceptions.InsufficientAvailableStockException;
import org.mystudying.bookmanagementauth.repositories.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryService {

    private final BookRepository bookRepository;

    public InventoryService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public void decrementStock(long bookId) {
        int updated = bookRepository.decrementAvailableIfInStock(bookId);
        if (updated == 0) {
            if (!bookRepository.existsById(bookId)) {
                throw new BookNotFoundException(bookId);
            } else {
                throw new BookNotAvailableException(bookId);
            }
        }
    }

    public void incrementStock(long bookId) {
        int updated = bookRepository.incrementAvailable(bookId);
        if (updated == 0) {
            throw new BookNotFoundException(bookId);
        }
    }

    public void replenish(long bookId, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        int updated = bookRepository.incrementAvailableBy(bookId, amount);
        if (updated == 0) {
            throw new BookNotFoundException(bookId);
        }
    }

    public void writeOff(long bookId, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        int updated = bookRepository.decrementAvailableBy(bookId, amount);
        if (updated == 0) {
            if (!bookRepository.existsById(bookId)) {
                throw new BookNotFoundException(bookId);
            } else {
                // Not enough stock to write off the full amount
                throw new InsufficientAvailableStockException(amount, bookId);
            }
        }
    }
}
