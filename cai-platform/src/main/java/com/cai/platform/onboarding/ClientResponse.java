package com.cai.platform.onboarding;

import com.cai.platform.domain.Client;
import com.cai.platform.domain.KycStatus;

import java.time.LocalDate;

/**
 * Client representation returned by the API. Deliberately excludes sinHash.
 */
public record ClientResponse(
        Long id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String email,
        String phone,
        String province,
        KycStatus kycStatus,
        LocalDate kycVerifiedDate) {

    public static ClientResponse from(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getFirstName(),
                client.getLastName(),
                client.getDateOfBirth(),
                client.getEmail(),
                client.getPhone(),
                client.getProvince(),
                client.getKycStatus(),
                client.getKycVerifiedDate());
    }
}
