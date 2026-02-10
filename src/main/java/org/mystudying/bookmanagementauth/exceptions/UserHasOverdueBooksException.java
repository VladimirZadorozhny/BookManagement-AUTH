package org.mystudying.bookmanagementauth.exceptions;

public class UserHasOverdueBooksException extends RuntimeException {
    public UserHasOverdueBooksException(long userId) {
        super("User with id " + userId + " has overdue books!");
    }
}

