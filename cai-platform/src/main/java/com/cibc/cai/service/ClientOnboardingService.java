package com.cibc.cai.service;

import com.cibc.cai.entity.Client;
import com.cibc.cai.repository.ClientRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Client onboarding with dedupe by SIN hash + date of birth.
 *
 * <p>Ports the legacy Apex {@code ClientOnboardingService.onboard}. New clients
 * start with KYC status {@code Pending} and are queued for verification via the
 * {@link KycVerificationEnqueuer} seam (implementation provided by the KYC phase).
 */
@Service
public class ClientOnboardingService {

    private final ClientRepository clientRepository;
    private final KycVerificationEnqueuer kycVerificationEnqueuer;

    public ClientOnboardingService(ClientRepository clientRepository,
                                   KycVerificationEnqueuer kycVerificationEnqueuer) {
        this.clientRepository = clientRepository;
        this.kycVerificationEnqueuer = kycVerificationEnqueuer;
    }

    /**
     * Returns the existing client if a dedupe match is found, otherwise creates a
     * new one and enqueues KYC verification.
     */
    @Transactional
    public Client onboard(OnboardingRequest req) {
        if (!StringUtils.hasText(req.sinHash()) || req.dateOfBirth() == null) {
            throw new OnboardingException("SIN hash and date of birth are required for dedupe");
        }

        List<Client> matches = clientRepository.bySinHashAndDob(req.sinHash(), req.dateOfBirth());
        if (!matches.isEmpty()) {
            return matches.get(0);
        }

        Client client = new Client();
        client.setFirstName(req.firstName());
        client.setLastName(req.lastName());
        client.setSinHash(req.sinHash());
        client.setDateOfBirth(req.dateOfBirth());
        client.setEmail(req.email());
        client.setPhone(req.phone());
        client.setProvince(req.province());
        client.setKycStatus("Pending");

        Client saved = clientRepository.save(client);
        kycVerificationEnqueuer.enqueueVerification(saved.getId());
        return saved;
    }
}
