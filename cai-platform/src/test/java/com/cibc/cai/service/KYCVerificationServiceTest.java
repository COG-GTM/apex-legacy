package com.cibc.cai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cibc.cai.entity.Client;
import com.cibc.cai.repository.ClientRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KYCVerificationServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private IdentityProviderClient identityProviderClient;

    @InjectMocks
    private KYCVerificationService service;

    private Client newClient() {
        Client client = new Client();
        client.setId(1L);
        client.setSinHash("abc123");
        client.setDateOfBirth(LocalDate.of(1990, 1, 15));
        client.setKycStatus("Pending");
        return client;
    }

    @Test
    void marksVerifiedOn200True() {
        Client client = newClient();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(identityProviderClient.verify("abc123", "1990-01-15"))
                .thenReturn(new IdentityProviderClient.VerificationResult(200, true));

        service.enqueueVerification(1L);

        assertThat(client.getKycStatus()).isEqualTo("Verified");
        assertThat(client.getKycVerifiedDate()).isEqualTo(LocalDate.now());
        verify(clientRepository).save(client);
    }

    @Test
    void marksFailedOn200False() {
        Client client = newClient();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(identityProviderClient.verify(anyString(), anyString()))
                .thenReturn(new IdentityProviderClient.VerificationResult(200, false));

        service.enqueueVerification(1L);

        assertThat(client.getKycStatus()).isEqualTo("Failed");
        assertThat(client.getKycVerifiedDate()).isNull();
        verify(clientRepository).save(client);
    }

    @Test
    void marksErrorOnNon200() {
        Client client = newClient();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(identityProviderClient.verify(anyString(), anyString()))
                .thenReturn(new IdentityProviderClient.VerificationResult(503, null));

        service.enqueueVerification(1L);

        assertThat(client.getKycStatus()).isEqualTo("Error");
        assertThat(client.getKycVerifiedDate()).isNull();
        verify(clientRepository).save(client);
    }

    @Test
    void skipsWhenClientMissing() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        service.enqueueVerification(99L);

        verifyNoInteractions(identityProviderClient);
        verify(clientRepository, never()).save(any());
    }

    @Test
    void sendsSinHashAndDobToProvider() {
        Client client = newClient();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(identityProviderClient.verify(anyString(), anyString()))
                .thenReturn(new IdentityProviderClient.VerificationResult(200, true));

        service.enqueueVerification(1L);

        ArgumentCaptor<String> sin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dob = ArgumentCaptor.forClass(String.class);
        verify(identityProviderClient).verify(sin.capture(), dob.capture());
        assertThat(sin.getValue()).isEqualTo("abc123");
        assertThat(dob.getValue()).isEqualTo("1990-01-15");
    }
}
