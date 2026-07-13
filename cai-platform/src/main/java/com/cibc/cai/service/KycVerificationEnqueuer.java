package com.cibc.cai.service;

/**
 * Contract for enqueuing asynchronous KYC verification for a client.
 *
 * <p>Ports the legacy Apex {@code KYCVerificationService.enqueueVerification(Id)}
 * seam. Onboarding (producer) depends only on this interface; the async
 * verification implementation (KYC service) provides it. This lets the two be
 * developed and wired independently.
 */
public interface KycVerificationEnqueuer {

    /**
     * Enqueue asynchronous KYC verification for the given client id.
     * Implementations must return promptly (fire-and-forget / {@code @Async}).
     */
    void enqueueVerification(Long clientId);
}
