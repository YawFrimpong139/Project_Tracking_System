package com.codewithzea.projecttrackingsystem.exception;


public class AuditLogException extends RuntimeException {
    public AuditLogException(String message) {
        super(message);
    }

    public AuditLogException(String message, Throwable cause) {
        super(message, cause);
    }
}

