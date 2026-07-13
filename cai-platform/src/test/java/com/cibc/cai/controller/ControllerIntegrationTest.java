package com.cibc.cai.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cibc.cai.controller.dto.DisputeRequest;
import com.cibc.cai.controller.dto.StatusChangeRequest;
import com.cibc.cai.entity.Client;
import com.cibc.cai.entity.LoanApplication;
import com.cibc.cai.repository.ClientRepository;
import com.cibc.cai.repository.LoanApplicationRepository;
import com.cibc.cai.service.KycVerificationEnqueuer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end REST tests for the loan-application and dispute controllers plus the
 * global exception handler, booted against a real PostgreSQL (Testcontainers).
 * Seeds fixtures through the repositories rather than adding read methods to the
 * services, matching the controller wiring.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private LoanApplicationRepository loanApplicationRepository;

    // The onboarding->KYC path is not exercised here; mock it so no async callout fires.
    @MockBean
    private KycVerificationEnqueuer kycVerificationEnqueuer;

    private Client kycVerifiedClient() {
        Client c = new Client();
        c.setFirstName("Verified");
        c.setLastName("Client");
        c.setSinHash("hash-loan-" + System.nanoTime());
        c.setDateOfBirth(LocalDate.of(1985, 3, 14));
        c.setKycStatus("Verified");
        c.setKycVerifiedDate(LocalDate.now().minusDays(10));
        return clientRepository.save(c);
    }

    private LoanApplication loan(Client client, String status) {
        LoanApplication app = new LoanApplication();
        app.setClient(client);
        app.setStatus(status);
        app.setAmount(new BigDecimal("100000.00"));
        app.setOfferedRate(new BigDecimal("0.0549"));
        app.setAmortizationMonths(300);
        app.setAnnualIncome(new BigDecimal("200000.00"));
        app.setMonthlyHousingCosts(new BigDecimal("1000.00"));
        app.setMonthlyDebtPayments(new BigDecimal("500.00"));
        return loanApplicationRepository.save(app);
    }

    @Test
    void qualify_returnsQualificationResult() throws Exception {
        LoanApplication app = loan(kycVerifiedClient(), "Submitted");

        mockMvc.perform(post("/api/loan-applications/{id}/qualify", app.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qualified").value(true))
                .andExpect(jsonPath("$.stressTestedRate").value(0.0749))
                .andExpect(jsonPath("$.gdsRatio").exists())
                .andExpect(jsonPath("$.tdsRatio").exists());

        mockMvc.perform(get("/api/loan-applications/{id}", app.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Approved"));
    }

    @Test
    void changeStatus_validTransition_returnsUpdatedStatus() throws Exception {
        LoanApplication app = loan(kycVerifiedClient(), "Draft");

        mockMvc.perform(post("/api/loan-applications/{id}/status", app.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StatusChangeRequest("Submitted"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Submitted"));
    }

    @Test
    void changeStatus_illegalTransition_returns409() throws Exception {
        LoanApplication app = loan(kycVerifiedClient(), "Draft");

        mockMvc.perform(post("/api/loan-applications/{id}/status", app.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StatusChangeRequest("Approved"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("IllegalStatusTransitionException"));
    }

    @Test
    void qualify_kycNotCurrent_returns400() throws Exception {
        Client pending = new Client();
        pending.setFirstName("Pending");
        pending.setLastName("Client");
        pending.setSinHash("hash-pending-" + System.nanoTime());
        pending.setDateOfBirth(LocalDate.of(1990, 1, 1));
        pending.setKycStatus("Pending");
        pending = clientRepository.save(pending);
        LoanApplication app = loan(pending, "Submitted");

        mockMvc.perform(post("/api/loan-applications/{id}/qualify", app.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("LoanApplicationException"));
    }

    @Test
    void get_unknownLoan_returns404() throws Exception {
        mockMvc.perform(get("/api/loan-applications/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void fileDispute_largeAmount_escalatesWith201() throws Exception {
        Client client = kycVerifiedClient();
        DisputeRequest req = new DisputeRequest(
                client.getId(), "4242", new BigDecimal("750.00"), "GOODS_13.1", false);

        mockMvc.perform(post("/api/disputes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("Escalated"))
                .andExpect(jsonPath("$.escalationCaseId").isNumber())
                .andExpect(jsonPath("$.slaDueDate").value(LocalDate.now().plusDays(2).toString()));
    }

    @Test
    void fileDispute_nonPositiveAmount_returns400() throws Exception {
        Client client = kycVerifiedClient();
        String body = objectMapper.writeValueAsString(new DisputeRequest(
                client.getId(), "4242", new BigDecimal("-5.00"), "GOODS_13.1", false));

        mockMvc.perform(post("/api/disputes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ValidationException"));
    }
}
