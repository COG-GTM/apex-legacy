package com.cai.platform.dispute;

/**
 * Thrown when a dispute cannot be filed (formerly TransactionDisputeService.DisputeException).
 */
public class DisputeException extends RuntimeException {

    public DisputeException(String message) {
        super(message);
    }
}
