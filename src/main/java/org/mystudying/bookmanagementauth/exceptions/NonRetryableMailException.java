package org.mystudying.bookmanagementauth.exceptions;

public class NonRetryableMailException extends RuntimeException {
    public NonRetryableMailException(String message) {

        super(message);
    }
}
