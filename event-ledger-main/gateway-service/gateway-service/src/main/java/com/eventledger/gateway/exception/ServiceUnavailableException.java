package com.eventledger.gateway.exception;

/**
 * Thrown when a downstream service is unavailable.
 *
 * Example:
 * - Account Service is down
 * - Request timeout
 * - Retry exhausted
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}