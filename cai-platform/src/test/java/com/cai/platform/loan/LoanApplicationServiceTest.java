package com.cai.platform.loan;

import com.cai.platform.domain.Client;
import com.cai.platform.domain.KycStatus;
import com.cai.platform.domain.LoanApplication;
import com.cai.platform.domain.LoanApplicationStatus;
import com.cai.platform.repository.ClientRepository;
import com.cai.platform.repository.LoanApplicationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ported from Apex LoanApplicationServiceTest, plus status-transition coverage.
 */
@SpringBootTest
@Transactional
class LoanApplicationServiceTest {

    @Autowired
    private LoanApplicationService loanApplicationService;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private LoanApplicationRepository loanApplicationRepository;

    private Client makeVerifiedClient() {
        return makeClient(LocalDate.now().minusDays(30));
    }

    private Client makeClient(LocalDate kycVerifiedDate) {
        Client client = new Client();
        client.setFirstName("Test");
        client.setLastName("Client");
        client.setSinHash(UUID.randomUUID().toString().replace("-", ""));
        client.setDateOfBirth(LocalDate.of(1985, 1, 1));
        client.setKycStatus(KycStatus.VERIFIED);
        client.setKycVerifiedDate(kycVerifiedDate);
        return clientRepository.save(client);
    }

    private LoanApplication makeApp(Client client, String amount, String income) {
        return makeApp(client, amount, income, LoanApplicationStatus.SUBMITTED);
    }

    private LoanApplication makeApp(Client client, String amount, String income,
                                    LoanApplicationStatus status) {
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
    void qualifiesHealthyApplication() {
        Client client = makeVerifiedClient();
        LoanApplication app = makeApp(client, "300000", "160000");

        QualificationResult result = loanApplicationService.qualify(app.getId());

        assertThat(result.qualified()).as("Healthy application should qualify").isTrue();
        assertThat(result.stressTestedRate()).isEqualByComparingTo("0.0749");
        assertThat(result.declineReason()).isNull();

        LoanApplication reloaded = loanApplicationRepository.findById(app.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(LoanApplicationStatus.APPROVED);
        assertThat(reloaded.getGdsRatio()).isNotNull();
        assertThat(reloaded.getTdsRatio()).isNotNull();
        assertThat(reloaded.getDeclineReason()).isNull();
    }

    @Test
    void declinesWhenGdsTooHigh() {
        Client client = makeVerifiedClient();
        LoanApplication app = makeApp(client, "900000", "60000");

        QualificationResult result = loanApplicationService.qualify(app.getId());

        assertThat(result.qualified()).isFalse();
        assertThat(result.declineReason()).contains("GDS").contains("exceeds 39%");

        LoanApplication reloaded = loanApplicationRepository.findById(app.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(LoanApplicationStatus.DECLINED);
        assertThat(reloaded.getDeclineReason()).contains("GDS");
    }

    @Test
    void blocksStaleKyc() {
        Client client = makeClient(LocalDate.now().minusDays(400));
        LoanApplication app = makeApp(client, "300000", "160000");

        assertThatThrownBy(() -> loanApplicationService.qualify(app.getId()))
                .isInstanceOf(LoanApplicationException.class)
                .hasMessageContaining("KYC not current");
    }

    @Test
    void blocksUnverifiedKycStatus() {
        Client client = makeVerifiedClient();
        client.setKycStatus(KycStatus.PENDING);
        clientRepository.save(client);
        LoanApplication app = makeApp(client, "300000", "160000");

        assertThatThrownBy(() -> loanApplicationService.qualify(app.getId()))
                .isInstanceOf(LoanApplicationException.class)
                .hasMessageContaining("KYC not current");
    }

    @Test
    void submittingDraftStampsSubmittedDate() {
        Client client = makeVerifiedClient();
        LoanApplication app = makeApp(client, "300000", "160000", LoanApplicationStatus.DRAFT);

        LoanApplication updated =
                loanApplicationService.updateStatus(app.getId(), LoanApplicationStatus.SUBMITTED);

        assertThat(updated.getStatus()).isEqualTo(LoanApplicationStatus.SUBMITTED);
        assertThat(updated.getSubmittedDate()).isNotNull();
    }

    @Test
    void allowsValidTransitions() {
        Client client = makeVerifiedClient();
        LoanApplication app = makeApp(client, "300000", "160000", LoanApplicationStatus.DRAFT);

        loanApplicationService.updateStatus(app.getId(), LoanApplicationStatus.SUBMITTED);
        loanApplicationService.updateStatus(app.getId(), LoanApplicationStatus.APPROVED);
        LoanApplication funded =
                loanApplicationService.updateStatus(app.getId(), LoanApplicationStatus.FUNDED);

        assertThat(funded.getStatus()).isEqualTo(LoanApplicationStatus.FUNDED);
    }

    @Test
    void rejectsDraftToApproved() {
        Client client = makeVerifiedClient();
        LoanApplication app = makeApp(client, "300000", "160000", LoanApplicationStatus.DRAFT);

        assertThatThrownBy(() ->
                loanApplicationService.updateStatus(app.getId(), LoanApplicationStatus.APPROVED))
                .isInstanceOf(IllegalStatusTransitionException.class)
                .hasMessageContaining("DRAFT")
                .hasMessageContaining("APPROVED");
    }

    @Test
    void rejectsTransitionsOutOfTerminalStatuses() {
        Client client = makeVerifiedClient();
        for (LoanApplicationStatus terminal : new LoanApplicationStatus[]{
                LoanApplicationStatus.DECLINED,
                LoanApplicationStatus.FUNDED,
                LoanApplicationStatus.CANCELLED}) {
            LoanApplication app = makeApp(client, "300000", "160000", terminal);
            assertThatThrownBy(() ->
                    loanApplicationService.updateStatus(app.getId(), LoanApplicationStatus.DRAFT))
                    .isInstanceOf(IllegalStatusTransitionException.class);
        }
    }

    @Test
    void sameStatusIsNoOp() {
        Client client = makeVerifiedClient();
        LoanApplication app = makeApp(client, "300000", "160000", LoanApplicationStatus.DRAFT);

        LoanApplication result =
                loanApplicationService.updateStatus(app.getId(), LoanApplicationStatus.DRAFT);

        assertThat(result.getStatus()).isEqualTo(LoanApplicationStatus.DRAFT);
        assertThat(result.getSubmittedDate()).isNull();
    }

    @Test
    void cancellingFromDraftAndSubmittedAndApproved() {
        Client client = makeVerifiedClient();
        for (LoanApplicationStatus from : new LoanApplicationStatus[]{
                LoanApplicationStatus.DRAFT,
                LoanApplicationStatus.SUBMITTED,
                LoanApplicationStatus.APPROVED}) {
            LoanApplication app = makeApp(client, "300000", "160000", from);
            LoanApplication cancelled =
                    loanApplicationService.updateStatus(app.getId(), LoanApplicationStatus.CANCELLED);
            assertThat(cancelled.getStatus()).isEqualTo(LoanApplicationStatus.CANCELLED);
        }
    }

    @Test
    void throwsWhenApplicationNotFound() {
        assertThatThrownBy(() -> loanApplicationService.qualify(999999L))
                .isInstanceOf(LoanApplicationException.class)
                .hasMessageContaining("not found");
    }
}
