package com.cibc.cai.controller.dto;

import com.cibc.cai.service.OnboardingRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * REST request body for client onboarding. Decouples the HTTP layer from the
 * service-layer {@link OnboardingRequest} record and applies bean validation.
 */
public record OnboardingRequestDto(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String sinHash,
        @NotNull LocalDate dateOfBirth,
        String email,
        String phone,
        String province) {

    public OnboardingRequest toServiceRequest() {
        return new OnboardingRequest(
                firstName, lastName, sinHash, dateOfBirth, email, phone, province);
    }
}
