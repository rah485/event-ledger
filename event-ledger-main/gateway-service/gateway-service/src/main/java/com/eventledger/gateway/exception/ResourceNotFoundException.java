package com.eventledger.gateway.exception;

/**
 * Thrown when a requested resource does not exist.
 *
 * Examples:
 * - Event not found
 * - Account not found (if required)
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

}