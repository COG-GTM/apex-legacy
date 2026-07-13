package com.cibc.cai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.cibc.cai.entity.Client;
import com.cibc.cai.repository.ClientRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testcontainers (real PostgreSQL + Flyway) integration test for
 * {@link ClientOnboardingService}, persisting through the JPA repository and
 * running the real {@code @Async} {@link KYCVerificationService} enqueuer bean
 * (README rule 4 — onboarding dedupe by SIN hash + DOB).
 *
 * <p>The external identity-provider HTTP client is replaced by a deterministic
 * in-process fake ({@link StubIdentityProviderConfig}) so the async KYC callout
 * never reaches a live endpoint. Assertions cover only the synchronous onboarding
 * contract (dedupe + {@code Pending} bootstrap status); the fire-and-forget KYC
 * transition is out of scope here (it is unit-tested in
 * {@link KYCVerificationServiceTest}).
 */
@SpringBootTest
@Testcontainers
class ClientOnboardingServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    /** Deterministic fake for the external identity provider — no live endpoint, no Mockito reset races. */
    @TestConfiguration
    static class StubIdentityProviderConfig {
        @Bean
        @Primary
        IdentityProviderClient stubIdentityProviderClient() {
            return (sinHash, dob) -> new IdentityProviderClient.VerificationResult(200, true);
        }
    }

    @Autowired
    private ClientOnboardingService onboardingService;

    @Autowired
    private ClientRepository clientRepository;

    private OnboardingRequest request(String sinHash, LocalDate dob) {
        return new OnboardingRequest(
                "Ada", "Lovelace", sinHash, dob,
                "ada@example.com", "555-0100", "ON");
    }

    @Test
    void onboardNewClientPersistsWithPendingKyc() {
        String sinHash = "sin-onboard-" + System.nanoTime();
        LocalDate dob = LocalDate.of(1985, 3, 14);

        Client created = onboardingService.onboard(request(sinHash, dob));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getKycStatus()).isEqualTo("Pending");
        assertThat(created.getSinHash()).isEqualTo(sinHash);
        assertThat(created.getDateOfBirth()).isEqualTo(dob);

        assertThat(clientRepository.bySinHashAndDob(sinHash, dob)).hasSize(1);
    }

    @Test
    void onboardSameSinHashAndDobIsIdempotentAndDoesNotDuplicate() {
        String sinHash = "sin-dedupe-" + System.nanoTime();
        LocalDate dob = LocalDate.of(1979, 7, 22);

        Client first = onboardingService.onboard(request(sinHash, dob));
        Client second = onboardingService.onboard(request(sinHash, dob));

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(clientRepository.bySinHashAndDob(sinHash, dob)).hasSize(1);
    }

    @Test
    void onboardDifferentSinHashOrDobCreatesDistinctClients() {
        long unique = System.nanoTime();
        LocalDate dob = LocalDate.of(1979, 7, 22);

        Client a = onboardingService.onboard(request("sin-a-" + unique, dob));
        // Genuinely new (sinHash, dob) -> a brand-new client, not a dedupe match.
        Client b = onboardingService.onboard(request("sin-b-" + unique, dob));

        assertThat(b.getId()).isNotEqualTo(a.getId());
        assertThat(b.getKycStatus()).isEqualTo("Pending");
    }
}
