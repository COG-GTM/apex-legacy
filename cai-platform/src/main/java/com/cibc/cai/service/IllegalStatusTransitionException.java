package com.cibc.cai.service;

/**
 * Thrown when a loan application status change violates the state machine
 * defined by {@link LoanApplicationStatusValidator}. Ports the legacy Apex
 * {@code addError('Illegal status transition: ...')} behaviour of
 * {@code LoanApplicationTriggerHandler}.
 */
public class IllegalStatusTransitionException extends LoanApplicationException {

    public IllegalStatusTransitionException(String message) {
        super(message);
    }
}
