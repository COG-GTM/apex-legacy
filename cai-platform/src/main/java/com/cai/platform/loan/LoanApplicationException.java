package com.cai.platform.loan;

/**
 * Thrown when a loan application cannot be qualified (formerly
 * LoanApplicationService.LoanApplicationException in Apex).
 */
public class LoanApplicationException extends RuntimeException {

    public LoanApplicationException(String message) {
        super(message);
    }

    public LoanApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
