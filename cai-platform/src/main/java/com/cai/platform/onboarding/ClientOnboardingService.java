package com.cai.platform.onboarding;

import com.cai.platform.domain.Client;
import com.cai.platform.domain.KycStatus;
import com.cai.platform.repository.ClientRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Client onboarding with dedupe by SIN hash + date of birth.
 * New clients start with KYC status PENDING and are queued for verification.
 */
@Service
public class ClientOnboardingService {

    private static final int STALE_KYC_QUERY_LIMIT = 200;

    private final ClientRepository clientRepository;
    private final KycVerificationService kycVerificationService;

    public ClientOnboardingService(ClientRepository clientRepository,
                                   KycVerificationService kycVerificationService) {
        this.clientRepository = clientRepository;
        this.kycVerificationService = kycVerificationService;
    }

    /**
     * Returns the existing client if a match is found (dedupe),
     * otherwise creates a new one and enqueues KYC verification.
     */
    @Transactional
    public Client onboard(OnboardingRequest request) {
        if (request.getSinHash() == null || request.getSinHash().isBlank()
                || request.getDateOfBirth() == null) {
            throw new OnboardingException("SIN hash and date of birth are required for dedupe");
        }

        List<Client> matches = clientRepository.findBySinHashAndDateOfBirth(
                request.getSinHash(), request.getDateOfBirth());
        if (!matches.isEmpty()) {
            return matches.get(0);
        }

        Client client = new Client();
        client.setFirstName(request.getFirstName());
        client.setLastName(request.getLastName());
        client.setSinHash(request.getSinHash());
        client.setDateOfBirth(request.getDateOfBirth());
        client.setEmail(request.getEmail());
        client.setPhone(request.getPhone());
        client.setProvince(request.getProvince());
        client.setKycStatus(KycStatus.PENDING);
        client = clientRepository.save(client);

        kycVerificationService.verifyAsync(client.getId());
        return client;
    }

    /**
     * Clients whose KYC was verified more than {@code olderThanDays} days ago
     * (formerly ClientSelector.withStaleKyc in Apex), oldest first, capped at 200.
     */
    public List<Client> withStaleKyc(int olderThanDays) {
        LocalDate cutoff = LocalDate.now().minusDays(olderThanDays);
        return clientRepository.findByKycStatusAndKycVerifiedDateBefore(
                KycStatus.VERIFIED, cutoff, PageRequest.of(0, STALE_KYC_QUERY_LIMIT));
    }
}
