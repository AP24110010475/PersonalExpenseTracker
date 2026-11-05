package com.daybook.database;

/**
 * Custom runtime exception representing database connectivity and execution failures.
 * Used across the DAO and Service layers for consistent exception handling.
 */
public class DatabaseException extends Exception {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
