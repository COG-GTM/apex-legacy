package com.cai.platform.onboarding;

import com.cai.platform.domain.Client;
import com.cai.platform.domain.KycStatus;
import com.cai.platform.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KycVerificationServiceTest {

    private static final String BASE_URL = "http://identity-provider.test";

    private ClientRepository clientRepository;
    private MockRestServiceServer server;
    private KycVerificationService service;
    private Client client;

    @BeforeEach
    void setUp() {
        clientRepository = mock(ClientRepository.class);
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new KycVerificationService(clientRepository, builder, BASE_URL);

        client = new Client();
        client.setId(1L);
        client.setSinHash("a".repeat(64));
        client.setDateOfBirth(LocalDate.of(1990, 4, 12));
        client.setKycStatus(KycStatus.PENDING);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
    }

    @Test
    void verifiedResponseSetsVerifiedStatusAndDate() {
        server.expect(requestTo(BASE_URL + "/v2/verify"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sinHash").value(client.getSinHash()))
                .andExpect(jsonPath("$.dob").value("1990-04-12"))
                .andRespond(withSuccess("{\"verified\": true}", MediaType.APPLICATION_JSON));

        service.verifyAsync(1L);

        Client saved = capturedSave();
        assertThat(saved.getKycStatus()).isEqualTo(KycStatus.VERIFIED);
        assertThat(saved.getKycVerifiedDate()).isEqualTo(LocalDate.now());
        server.verify();
    }

    @Test
    void notVerifiedResponseSetsFailedStatusAndClearsDate() {
        client.setKycVerifiedDate(LocalDate.of(2020, 1, 1));
        server.expect(requestTo(BASE_URL + "/v2/verify"))
                .andRespond(withSuccess("{\"verified\": false}", MediaType.APPLICATION_JSON));

        service.verifyAsync(1L);

        Client saved = capturedSave();
        assertThat(saved.getKycStatus()).isEqualTo(KycStatus.FAILED);
        assertThat(saved.getKycVerifiedDate()).isNull();
    }

    @Test
    void non200ResponseSetsErrorStatus() {
        server.expect(requestTo(BASE_URL + "/v2/verify"))
                .andRespond(withServerError());

        service.verifyAsync(1L);

        Client saved = capturedSave();
        assertThat(saved.getKycStatus()).isEqualTo(KycStatus.ERROR);
    }

    @Test
    void badRequestResponseSetsErrorStatus() {
        server.expect(requestTo(BASE_URL + "/v2/verify"))
                .andRespond(request -> {
                    throw new java.io.IOException("connection refused");
                });

        service.verifyAsync(1L);

        Client saved = capturedSave();
        assertThat(saved.getKycStatus()).isEqualTo(KycStatus.ERROR);
    }

    private Client capturedSave() {
        ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(captor.capture());
        return captor.getValue();
    }
}
