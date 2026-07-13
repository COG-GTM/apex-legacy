package com.cibc.cai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cibc.cai.entity.Client;
import com.cibc.cai.repository.ClientRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link ClientOnboardingService} covering the ported Apex
 * onboarding logic: dedupe, new-client creation, and validation. Uses Mockito
 * so no Spring context or database is required.
 */
@ExtendWith(MockitoExtension.class)
class ClientOnboardingServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private KycVerificationEnqueuer kycVerificationEnqueuer;

    @InjectMocks
    private ClientOnboardingService service;

    private static OnboardingRequest request() {
        return new OnboardingRequest(
                "Ada", "Lovelace", "hash-a", LocalDate.of(1985, 3, 14),
                "ada@example.com", "555-0100", "ON");
    }

    @Test
    void onboard_dedupeMatch_returnsExistingClientAndDoesNotEnqueue() {
        Client existing = new Client();
        existing.setId(42L);
        Client other = new Client();
        other.setId(99L);
        when(clientRepository.bySinHashAndDob("hash-a", LocalDate.of(1985, 3, 14)))
                .thenReturn(List.of(existing, other));

        Client result = service.onboard(request());

        assertThat(result).isSameAs(existing);
        verify(clientRepository, never()).save(any());
        verify(kycVerificationEnqueuer, never()).enqueueVerification(anyLong());
    }

    @Test
    void onboard_newClient_savesWithPendingKycAndEnqueues() {
        when(clientRepository.bySinHashAndDob("hash-a", LocalDate.of(1985, 3, 14)))
                .thenReturn(List.of());
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> {
            Client c = inv.getArgument(0);
            c.setId(7L);
            return c;
        });

        Client result = service.onboard(request());

        ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(captor.capture());
        Client saved = captor.getValue();
        assertThat(saved.getFirstName()).isEqualTo("Ada");
        assertThat(saved.getLastName()).isEqualTo("Lovelace");
        assertThat(saved.getSinHash()).isEqualTo("hash-a");
        assertThat(saved.getDateOfBirth()).isEqualTo(LocalDate.of(1985, 3, 14));
        assertThat(saved.getEmail()).isEqualTo("ada@example.com");
        assertThat(saved.getPhone()).isEqualTo("555-0100");
        assertThat(saved.getProvince()).isEqualTo("ON");
        assertThat(saved.getKycStatus()).isEqualTo("Pending");

        assertThat(result.getId()).isEqualTo(7L);
        verify(kycVerificationEnqueuer).enqueueVerification(eq(7L));
    }

    @Test
    void onboard_blankSinHash_throwsAndDoesNotTouchCollaborators() {
        OnboardingRequest req = new OnboardingRequest(
                "Ada", "Lovelace", "  ", LocalDate.of(1985, 3, 14),
                "ada@example.com", "555-0100", "ON");

        assertThatThrownBy(() -> service.onboard(req))
                .isInstanceOf(OnboardingException.class)
                .hasMessage("SIN hash and date of birth are required for dedupe");

        verify(clientRepository, never()).save(any());
        verify(kycVerificationEnqueuer, never()).enqueueVerification(anyLong());
    }

    @Test
    void onboard_nullDateOfBirth_throws() {
        OnboardingRequest req = new OnboardingRequest(
                "Ada", "Lovelace", "hash-a", null,
                "ada@example.com", "555-0100", "ON");

        assertThatThrownBy(() -> service.onboard(req))
                .isInstanceOf(OnboardingException.class)
                .hasMessage("SIN hash and date of birth are required for dedupe");

        verify(clientRepository, never()).save(any());
        verify(kycVerificationEnqueuer, never()).enqueueVerification(anyLong());
    }
}
