package com.cibc.cai.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cibc.cai.controller.dto.OnboardingRequestDto;
import com.cibc.cai.service.KYCVerificationService;
import com.cibc.cai.service.KycVerificationEnqueuer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Full-context integration test proving the onboarding &rarr; KYC cross-service
 * wiring over REST. Boots the whole application against a real PostgreSQL
 * (Testcontainers, mirroring {@code ClientRepositoryTest}). The concrete
 * {@link KYCVerificationService} bean (a {@link KycVerificationEnqueuer}) is
 * replaced by a Mockito mock so the real {@code @Async} identity-provider
 * callout never fires against a network endpoint, while still verifying that
 * onboarding invokes the enqueuer seam it is DI-wired to.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ClientControllerWiringTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KycVerificationEnqueuer kycVerificationEnqueuer;

    @Test
    void postClient_returns201AndTriggersKycEnqueueForNewClientId() throws Exception {
        OnboardingRequestDto dto = new OnboardingRequestDto(
                "Ada", "Lovelace", "hash-wiring-1", LocalDate.of(1985, 3, 14),
                "ada@example.com", "555-0100", "ON");

        MvcResult result = mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.lastName").value("Lovelace"))
                .andExpect(jsonPath("$.kycStatus").value("Pending"))
                .andReturn();

        long responseId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(kycVerificationEnqueuer).enqueueVerification(captor.capture());
        assertThat(captor.getValue()).isEqualTo(responseId);
    }

    @Test
    void postClient_missingRequiredFields_returns400AndDoesNotEnqueue() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ValidationException"));

        verifyNoInteractions(kycVerificationEnqueuer);
    }
}
