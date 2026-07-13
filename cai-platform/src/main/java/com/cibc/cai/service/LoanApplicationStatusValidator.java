package com.cibc.cai.service;

import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Loan application status state machine, ported from the Apex
 * {@code LoanApplicationTriggerHandler.beforeUpdate} / {@code validateTransition}
 * logic.
 *
 * <p>In the legacy org the state machine ran as a {@code before update} trigger
 * on {@code Loan_Application__c}. Here it is a plain service-layer validator that
 * is invoked explicitly from {@link LoanApplicationService#changeStatus}. It is
 * intentionally <em>not</em> wired as a JPA {@code @PreUpdate} entity listener so
 * that {@link LoanApplicationService#qualify(Long)} — which writes an allowed
 * {@code Submitted -> Approved/Declined} outcome directly — cannot falsely trip
 * the validator during persistence.
 */
@Component
public class LoanApplicationStatusValidator {

    /** Mirrors the Apex {@code ALLOWED_TRANSITIONS} map exactly. */
    static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "Draft", Set.of("Submitted", "Cancelled"),
            "Submitted", Set.of("Approved", "Declined", "Cancelled"),
            "Approved", Set.of("Funded", "Cancelled"),
            "Declined", Set.of(),
            "Funded", Set.of(),
            "Cancelled", Set.of());

    /**
     * Validates a status change against {@link #ALLOWED_TRANSITIONS}.
     *
     * @throws IllegalStatusTransitionException if {@code from -> to} is not allowed
     */
    public void validateTransition(String from, String to) {
        Set<String> allowed = ALLOWED_TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new IllegalStatusTransitionException(
                    "Illegal status transition: " + from + " \u2192 " + to);
        }
    }
}
