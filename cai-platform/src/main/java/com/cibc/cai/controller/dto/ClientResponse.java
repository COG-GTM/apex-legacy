package com.cibc.cai.controller.dto;

import com.cibc.cai.entity.Client;
import java.time.LocalDate;

/**
 * REST response view of a {@link Client}. Serialized instead of the JPA entity
 * so lazy relations are never touched during marshalling.
 */
public record ClientResponse(
        Long id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String email,
        String phone,
        String province,
        String kycStatus,
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
