package com.codewithzea.projecttrackingsystem.exception;


/**
 * Exception thrown when business validation rules are violated.
 * This represents cases where the request is syntactically valid
 * but violates business logic constraints.
 */
public class BusinessValidationException extends RuntimeException {


    public BusinessValidationException(String message) {
        super(message);
    }


    public BusinessValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessValidationException(String format, Object... args) {
        super(String.format(format, args));
    }
}