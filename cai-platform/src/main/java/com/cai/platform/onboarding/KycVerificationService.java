package com.cai.platform.onboarding;

import com.cai.platform.domain.Client;
import com.cai.platform.domain.KycStatus;
import com.cai.platform.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Map;

/**
 * Asynchronous KYC verification against the bank's identity provider
 * (formerly a Salesforce @future callout).
 */
@Service
public class KycVerificationService {

    private final ClientRepository clientRepository;
    private final RestClient restClient;

    public KycVerificationService(ClientRepository clientRepository,
                                  RestClient.Builder restClientBuilder,
                                  @Value("${cai.identity-provider.base-url}") String identityProviderBaseUrl) {
        this.clientRepository = clientRepository;
        this.restClient = restClientBuilder.baseUrl(identityProviderBaseUrl).build();
    }

    @Async
    public void verifyAsync(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        try {
            ResponseEntity<VerifyResponse> response = restClient.post()
                    .uri("/v2/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "sinHash", client.getSinHash(),
                            "dob", String.valueOf(client.getDateOfBirth())))
                    .retrieve()
                    .toEntity(VerifyResponse.class);

            if (response.getStatusCode().value() == 200) {
                boolean verified = response.getBody() != null
                        && Boolean.TRUE.equals(response.getBody().verified());
                client.setKycStatus(verified ? KycStatus.VERIFIED : KycStatus.FAILED);
                client.setKycVerifiedDate(verified ? LocalDate.now() : null);
            } else {
                client.setKycStatus(KycStatus.ERROR);
            }
        } catch (Exception e) {
            client.setKycStatus(KycStatus.ERROR);
        }
        clientRepository.save(client);
    }

    record VerifyResponse(Boolean verified) {
    }
}
