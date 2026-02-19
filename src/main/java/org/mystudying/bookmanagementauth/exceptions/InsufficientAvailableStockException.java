package org.mystudying.bookmanagementauth.exceptions;

public class InsufficientAvailableStockException extends RuntimeException {
    public InsufficientAvailableStockException(int amount, Long bookId) {

        super("Not enough stock for write-off of " + amount + " units of book id: " + bookId);
    }
}
