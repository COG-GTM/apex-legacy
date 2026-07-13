package com.cibc.cai.service;

import com.cibc.cai.entity.Client;
import com.cibc.cai.repository.ClientRepository;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Asynchronous KYC verification against the bank's identity provider.
 *
 * <p>Ports the Apex {@code KYCVerificationService}. The Salesforce
 * {@code @future(callout=true)} method (callouts are forbidden in trigger/DML
 * context) becomes a Spring {@code @Async} method running on the virtual-thread
 * executor. The producer (onboarding) depends only on the shared
 * {@link KycVerificationEnqueuer} interface.
 */
@Service
public class KYCVerificationService implements KycVerificationEnqueuer {

    private static final Logger log = LoggerFactory.getLogger(KYCVerificationService.class);

    private final ClientRepository clientRepository;
    private final IdentityProviderClient identityProviderClient;

    public KYCVerificationService(ClientRepository clientRepository,
                                  IdentityProviderClient identityProviderClient) {
        this.clientRepository = clientRepository;
        this.identityProviderClient = identityProviderClient;
    }

    /**
     * Fire-and-forget enqueue of KYC verification. Runs on the async executor
     * (virtual threads) so callers return promptly, mirroring the Apex
     * {@code enqueueVerification} → {@code @future} seam.
     */
    @Async
    @Override
    public void enqueueVerification(Long clientId) {
        Client client = clientRepository.findById(clientId).orElse(null);
        if (client == null) {
            log.warn("KYC verification skipped: no client found for id {}", clientId);
            return;
        }

        IdentityProviderClient.VerificationResult result = identityProviderClient.verify(
                client.getSinHash(),
                String.valueOf(client.getDateOfBirth()));

        if (result.statusCode() == 200) {
            boolean verified = Boolean.TRUE.equals(result.verified());
            client.setKycStatus(verified ? "Verified" : "Failed");
            client.setKycVerifiedDate(verified ? LocalDate.now() : null);
        } else {
            client.setKycStatus("Error");
        }
        clientRepository.save(client);
    }
}
