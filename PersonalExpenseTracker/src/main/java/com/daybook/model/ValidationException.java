package com.daybook.model;

/**
 * Custom exception thrown when a user inputs invalid transaction data.
 */
public class ValidationException extends Exception {

    public ValidationException(String message) {
        super(message);
    }
}
