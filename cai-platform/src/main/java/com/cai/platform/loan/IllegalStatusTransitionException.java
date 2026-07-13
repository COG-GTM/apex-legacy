package com.cai.platform.loan;

import com.cai.platform.domain.LoanApplicationStatus;

/**
 * Thrown on a disallowed loan application status transition (formerly
 * enforced by LoanApplicationTrigger / LoanApplicationTriggerHandler).
 */
public class IllegalStatusTransitionException extends RuntimeException {

    public IllegalStatusTransitionException(LoanApplicationStatus from, LoanApplicationStatus to) {
        super("Illegal status transition: " + from + " -> " + to);
    }
}
