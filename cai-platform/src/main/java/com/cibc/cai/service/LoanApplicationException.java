package com.cibc.cai.service;

/**
 * Runtime exception raised by the loan qualification engine and status state
 * machine. Ports the legacy Apex inner class
 * {@code LoanApplicationService.LoanApplicationException}.
 */
public class LoanApplicationException extends RuntimeException {

    public LoanApplicationException(String message) {
        super(message);
    }

    public LoanApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
