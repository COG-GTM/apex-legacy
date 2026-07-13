package com.cibc.cai.service;

import java.time.LocalDate;

/**
 * Input for {@link ClientOnboardingService#onboard(OnboardingRequest)}.
 *
 * <p>Ports the legacy Apex inner class {@code ClientOnboardingService.OnboardingRequest}.
 * {@code sinHash} is the SHA-256 of the SIN, never the raw value.
 */
public record OnboardingRequest(
        String firstName,
        String lastName,
        String sinHash,
        LocalDate dateOfBirth,
        String email,
        String phone,
        String province) {
}
