package com.cibc.cai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cibc.cai.entity.Client;
import com.cibc.cai.entity.LoanApplication;
import com.cibc.cai.repository.ClientRepository;
import com.cibc.cai.repository.LoanApplicationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testcontainers (real PostgreSQL + Flyway) integration tests for
 * {@link LoanApplicationService}, exercising the full service through the JPA
 * repositories and the production {@code V1__init_schema.sql} migration.
 *
 * <p>Faithfully ports the legacy Apex {@code LoanApplicationServiceTest} scenarios
 * ({@code qualifiesHealthyApplication}, {@code declinesWhenGdsTooHigh},
 * {@code blocksStaleKyc}) and additionally covers the loan-status state machine
 * (README rules 1 &amp; 2). Fixtures mirror the Apex factory methods:
 * {@code makeVerifiedClient} (kycStatus=Verified, verified 30 days ago) and
 * {@code makeApp} (offeredRate=0.0549, amortization=300, housing=450, debt=300,
 * status=Submitted).
 */
@SpringBootTest
@Testcontainers
class LoanApplicationServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private LoanApplicationService loanApplicationService;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private LoanApplicationRepository loanApplicationRepository;

    /** Port of Apex {@code makeVerifiedClient}: Verified KYC, verified {@code today - verifiedDaysAgo}. */
    private Client makeVerifiedClient(int verifiedDaysAgo) {
        Client client = new Client();
        client.setFirstName("Test");
        client.setLastName("Client");
        client.setSinHash("sin-loan-" + System.nanoTime());
        client.setDateOfBirth(LocalDate.of(1985, 3, 14));
        client.setKycStatus("Verified");
        client.setKycVerifiedDate(LocalDate.now().minusDays(verifiedDaysAgo));
        return clientRepository.save(client);
    }

    /** Port of Apex {@code makeApp}. */
    private LoanApplication makeApp(Client client, String amount, String income) {
        LoanApplication app = new LoanApplication();
        app.setClient(client);
        app.setAmount(new BigDecimal(amount));
        app.setOfferedRate(new BigDecimal("0.0549"));
        app.setAmortizationMonths(300);
        app.setAnnualIncome(new BigDecimal(income));
        app.setMonthlyHousingCosts(new BigDecimal("450"));
        app.setMonthlyDebtPayments(new BigDecimal("300"));
        app.setStatus("Submitted");
        return loanApplicationRepository.save(app);
    }

    @Test
    void qualifiesHealthyApplication() {
        LoanApplication app = makeApp(makeVerifiedClient(30), "300000", "160000");

        QualificationResult result = loanApplicationService.qualify(app.getId());

        assertThat(result.qualified()).isTrue();
        // Rate stress-tested at +2%: 0.0549 + 0.02 = 0.0749.
        assertThat(result.stressTestedRate()).isEqualByComparingTo("0.0749");

        LoanApplication reloaded = loanApplicationRepository.findById(app.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo("Approved");
        assertThat(reloaded.getGdsRatio()).isNotNull();
        assertThat(reloaded.getTdsRatio()).isNotNull();
    }

    @Test
    void declinesWhenGdsTooHigh() {
        LoanApplication app = makeApp(makeVerifiedClient(30), "900000", "60000");

        QualificationResult result = loanApplicationService.qualify(app.getId());

        assertThat(result.qualified()).isFalse();
        assertThat(result.declineReason()).contains("GDS");

        LoanApplication reloaded = loanApplicationRepository.findById(app.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo("Declined");
    }

    @Test
    void blocksStaleKyc() {
        // KYC verified 400 days ago -> outside the 365-day gate.
        LoanApplication app = makeApp(makeVerifiedClient(400), "300000", "160000");

        assertThatThrownBy(() -> loanApplicationService.qualify(app.getId()))
                .isInstanceOf(LoanApplicationException.class)
                .hasMessageContaining("KYC not current");

        LoanApplication reloaded = loanApplicationRepository.findById(app.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo("Submitted");
    }

    @Test
    void kycVerifiedWithin365DaysQualifies() {
        // Boundary: verified 364 days ago is still current.
        LoanApplication app = makeApp(makeVerifiedClient(364), "300000", "160000");

        QualificationResult result = loanApplicationService.qualify(app.getId());

        assertThat(result.qualified()).isTrue();
    }

    @Test
    void changeStatusDraftToSubmittedStampsSubmittedDate() {
        Client client = makeVerifiedClient(30);
        LoanApplication app = makeApp(client, "300000", "160000");
        app.setStatus("Draft");
        loanApplicationRepository.save(app);

        Instant before = Instant.now();
        loanApplicationService.changeStatus(app.getId(), "Submitted");

        LoanApplication reloaded = loanApplicationRepository.findById(app.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo("Submitted");
        assertThat(reloaded.getSubmittedDate()).isNotNull();
        assertThat(reloaded.getSubmittedDate()).isAfterOrEqualTo(before);
    }

    @Test
    void changeStatusRejectsIllegalTransitionFromTerminalStatus() {
        Client client = makeVerifiedClient(30);
        LoanApplication app = makeApp(client, "300000", "160000");
        app.setStatus("Declined");
        loanApplicationRepository.save(app);

        assertThatThrownBy(() -> loanApplicationService.changeStatus(app.getId(), "Approved"))
                .isInstanceOf(IllegalStatusTransitionException.class)
                .hasMessageContaining("Illegal status transition: Declined");

        LoanApplication reloaded = loanApplicationRepository.findById(app.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo("Declined");
    }
}
