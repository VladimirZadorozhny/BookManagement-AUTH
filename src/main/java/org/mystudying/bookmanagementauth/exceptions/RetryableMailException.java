package org.mystudying.bookmanagementauth.exceptions;

public class RetryableMailException extends RuntimeException {
    public RetryableMailException(String message, Throwable cause) {

        super(message, cause);
    }
}
