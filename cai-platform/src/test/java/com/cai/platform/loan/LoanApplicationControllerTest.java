package com.cai.platform.loan;

import com.cai.platform.domain.Client;
import com.cai.platform.domain.KycStatus;
import com.cai.platform.domain.LoanApplication;
import com.cai.platform.domain.LoanApplicationStatus;
import com.cai.platform.repository.ClientRepository;
import com.cai.platform.repository.LoanApplicationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LoanApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private LoanApplicationRepository loanApplicationRepository;

    private LoanApplication makeApp(LocalDate kycVerifiedDate, String amount, String income,
                                    LoanApplicationStatus status) {
        Client client = new Client();
        client.setFirstName("Test");
        client.setLastName("Client");
        client.setKycStatus(KycStatus.VERIFIED);
        client.setKycVerifiedDate(kycVerifiedDate);
        client = clientRepository.save(client);

        LoanApplication app = new LoanApplication();
        app.setClient(client);
        app.setAmount(new BigDecimal(amount));
        app.setOfferedRate(new BigDecimal("0.0549"));
        app.setAmortizationMonths(300);
        app.setAnnualIncome(new BigDecimal(income));
        app.setMonthlyHousingCosts(new BigDecimal("450"));
        app.setMonthlyDebtPayments(new BigDecimal("300"));
        app.setStatus(status);
        return loanApplicationRepository.save(app);
    }

    @Test
    void qualifyEndpointApprovesHealthyApplication() throws Exception {
        LoanApplication app = makeApp(LocalDate.now().minusDays(30), "300000", "160000",
                LoanApplicationStatus.SUBMITTED);

        mockMvc.perform(post("/api/loan-applications/{id}/qualify", app.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qualified").value(true))
                .andExpect(jsonPath("$.stressTestedRate").value(0.0749));
    }

    @Test
    void qualifyEndpointReturns422ForStaleKyc() throws Exception {
        LoanApplication app = makeApp(LocalDate.now().minusDays(400), "300000", "160000",
                LoanApplicationStatus.SUBMITTED);

        mockMvc.perform(post("/api/loan-applications/{id}/qualify", app.getId()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error", containsString("KYC not current")));
    }

    @Test
    void statusEndpointSubmitsDraft() throws Exception {
        LoanApplication app = makeApp(LocalDate.now().minusDays(30), "300000", "160000",
                LoanApplicationStatus.DRAFT);

        mockMvc.perform(patch("/api/loan-applications/{id}/status", app.getId())
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":\"SUBMITTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.submittedDate").isNotEmpty());
    }

    @Test
    void statusEndpointReturns409ForIllegalTransition() throws Exception {
        LoanApplication app = makeApp(LocalDate.now().minusDays(30), "300000", "160000",
                LoanApplicationStatus.FUNDED);

        mockMvc.perform(patch("/api/loan-applications/{id}/status", app.getId())
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":\"DRAFT\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("Illegal status transition")));
    }
}
