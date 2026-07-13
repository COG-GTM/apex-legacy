package com.cai.platform.onboarding;

import com.cai.platform.domain.Client;
import com.cai.platform.domain.KycStatus;
import com.cai.platform.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class ClientOnboardingServiceTest {

    @Autowired
    private ClientOnboardingService clientOnboardingService;

    @Autowired
    private ClientRepository clientRepository;

    @MockitoBean
    private KycVerificationService kycVerificationService;

    @BeforeEach
    void setUp() {
        clientRepository.deleteAll();
    }

    private OnboardingRequest validRequest() {
        OnboardingRequest request = new OnboardingRequest();
        request.setFirstName("Avery");
        request.setLastName("Chen");
        request.setSinHash("a".repeat(64));
        request.setDateOfBirth(LocalDate.of(1990, 4, 12));
        request.setEmail("avery.chen@example.com");
        request.setPhone("416-555-0100");
        request.setProvince("ON");
        return request;
    }

    @Test
    void missingSinHashThrows() {
        OnboardingRequest request = validRequest();
        request.setSinHash("  ");
        assertThatThrownBy(() -> clientOnboardingService.onboard(request))
                .isInstanceOf(OnboardingException.class)
                .hasMessageContaining("SIN hash and date of birth are required");
    }

    @Test
    void missingDateOfBirthThrows() {
        OnboardingRequest request = validRequest();
        request.setDateOfBirth(null);
        assertThatThrownBy(() -> clientOnboardingService.onboard(request))
                .isInstanceOf(OnboardingException.class);
    }

    @Test
    void newClientStartsPendingAndTriggersVerification() {
        Client client = clientOnboardingService.onboard(validRequest());

        assertThat(client.getId()).isNotNull();
        assertThat(client.getKycStatus()).isEqualTo(KycStatus.PENDING);
        assertThat(client.getKycVerifiedDate()).isNull();
        verify(kycVerificationService).verifyAsync(client.getId());
    }

    @Test
    void dedupeReturnsExistingClientWithoutTriggeringVerification() {
        Client existing = clientOnboardingService.onboard(validRequest());

        OnboardingRequest duplicate = validRequest();
        duplicate.setFirstName("Different");
        Client result = clientOnboardingService.onboard(duplicate);

        assertThat(result.getId()).isEqualTo(existing.getId());
        assertThat(clientRepository.count()).isEqualTo(1);
        verify(kycVerificationService, times(1)).verifyAsync(anyLong());
    }

    @Test
    void withStaleKycReturnsOnlyVerifiedClientsOlderThanCutoff() {
        Client stale = persistClient("b".repeat(64), KycStatus.VERIFIED, LocalDate.now().minusDays(400));
        persistClient("c".repeat(64), KycStatus.VERIFIED, LocalDate.now().minusDays(10));
        persistClient("d".repeat(64), KycStatus.FAILED, null);

        List<Client> results = clientOnboardingService.withStaleKyc(365);

        assertThat(results).extracting(Client::getId).containsExactly(stale.getId());
    }

    private Client persistClient(String sinHash, KycStatus status, LocalDate verifiedDate) {
        Client client = new Client();
        client.setFirstName("Test");
        client.setLastName("Client");
        client.setSinHash(sinHash);
        client.setDateOfBirth(LocalDate.of(1985, 1, 1));
        client.setKycStatus(status);
        client.setKycVerifiedDate(verifiedDate);
        return clientRepository.save(client);
    }
}
