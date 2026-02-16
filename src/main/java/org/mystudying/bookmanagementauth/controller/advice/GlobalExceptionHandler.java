package org.mystudying.bookmanagementauth.controller.advice;

import jakarta.servlet.http.HttpServletRequest;
import org.mystudying.bookmanagementauth.dto.ErrorResponse;
import org.mystudying.bookmanagementauth.exceptions.*;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({BookNotFoundException.class, AuthorNotFoundException.class, UserNotFoundException.class,
            GenreNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFoundException(RuntimeException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), "RESOURCE_NOT_FOUND");
    }

    @ExceptionHandler({BookAlreadyBorrowedException.class, BookNotBorrowedException.class, EmailAlreadyExistsException.class,
            BookNotAvailableException.class, BookHasBookingsException.class, AuthorHasBooksException.class,
            UserHasBookingsException.class, UserHasOverdueBooksException.class, UserHasUnpaidFinesException.class,
            GenreHasBooksException.class})
    public ResponseEntity<ErrorResponse> handleConflictException(RuntimeException ex, HttpServletRequest request) {
        String code = "DATA_CONFLICT";
        if (ex instanceof BookNotAvailableException) code = "BOOK_NOT_AVAILABLE";
        if (ex instanceof UserHasOverdueBooksException) code = "USER_HAS_OVERDUE_BOOKS";
        if (ex instanceof UserHasUnpaidFinesException) code = "USER_HAS_UNPAID_FINES";
        if (ex instanceof EmailAlreadyExistsException) code = "EMAIL_ALREADY_EXISTS";

        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), code);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, CannotAcquireLockException.class})
    public ResponseEntity<ErrorResponse> handleConcurrentModificationException(RuntimeException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, "The data was modified by another user or is currently locked. Please refresh and try again.", request.getRequestURI(), "CONCURRENT_MODIFICATION");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getAllErrors().stream()
                .map(error -> ((FieldError) error).getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), "VALIDATION_FAILED");
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI(), "UNAUTHORIZED");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Access denied: " + ex.getMessage(), request.getRequestURI(), "ACCESS_DENIED");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request.getRequestURI(), "INTERNAL_SERVER_ERROR");
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message, String path, String code) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                code
        );
        return new ResponseEntity<>(errorResponse, status);
    }
}
