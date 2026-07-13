package com.cibc.cai.service;

/**
 * Ports the legacy Apex {@code ClientOnboardingService.OnboardingException}.
 * Thrown when an onboarding request cannot be processed.
 */
public class OnboardingException extends RuntimeException {

    public OnboardingException(String message) {
        super(message);
    }
}
