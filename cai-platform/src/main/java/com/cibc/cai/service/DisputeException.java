package com.cibc.cai.service;

/**
 * Thrown when a transaction dispute cannot be filed. Ports the inner Apex
 * {@code TransactionDisputeService.DisputeException}.
 */
public class DisputeException extends RuntimeException {

    public DisputeException(String message) {
        super(message);
    }
}
