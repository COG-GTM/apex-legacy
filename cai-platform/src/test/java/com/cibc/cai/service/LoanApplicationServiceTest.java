package com.cibc.cai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cibc.cai.entity.Client;
import com.cibc.cai.entity.LoanApplication;
import com.cibc.cai.repository.LoanApplicationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pure Mockito unit tests for the ported loan qualification engine and status
 * state machine. No Spring context or database is required. Full Testcontainers
 * integration tests are Phase 4's responsibility.
 */
@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceTest {

    @Mock
    private LoanApplicationRepository repository;

    private final LoanApplicationStatusValidator validator = new LoanApplicationStatusValidator();

    private LoanApplicationService service() {
        return new LoanApplicationService(repository, validator);
    }

    private Client verifiedClient(int verifiedDaysAgo) {
        Client client = new Client();
        client.setId(1L);
        client.setKycStatus("Verified");
        client.setKycVerifiedDate(LocalDate.now().minusDays(verifiedDaysAgo));
        return client;
    }

    private LoanApplication app(Client client, String amount, String income) {
        LoanApplication app = new LoanApplication();
        app.setId(42L);
        app.setClient(client);
        app.setAmount(new BigDecimal(amount));
        app.setOfferedRate(new BigDecimal("0.0549"));
        app.setAmortizationMonths(300);
        app.setAnnualIncome(new BigDecimal(income));
        app.setMonthlyHousingCosts(new BigDecimal("450"));
        app.setMonthlyDebtPayments(new BigDecimal("300"));
        app.setStatus("Submitted");
        return app;
    }

    @Test
    void qualifiesHealthyApplication() {
        LoanApplication app = app(verifiedClient(30), "300000", "160000");
        when(repository.findById(42L)).thenReturn(Optional.of(app));

        QualificationResult result = service().qualify(42L);

        assertThat(result.qualified()).isTrue();
        assertThat(result.stressTestedRate()).isEqualByComparingTo("0.0749");
        assertThat(result.declineReason()).isNull();

        ArgumentCaptor<LoanApplication> saved = ArgumentCaptor.forClass(LoanApplication.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo("Approved");
        assertThat(saved.getValue().getGdsRatio()).isNotNull();
        assertThat(saved.getValue().getTdsRatio()).isNotNull();
    }

    @Test
    void declinesWhenGdsTooHigh() {
        LoanApplication app = app(verifiedClient(30), "900000", "60000");
        when(repository.findById(42L)).thenReturn(Optional.of(app));

        QualificationResult result = service().qualify(42L);

        assertThat(result.qualified()).isFalse();
        assertThat(result.declineReason()).contains("GDS");

        ArgumentCaptor<LoanApplication> saved = ArgumentCaptor.forClass(LoanApplication.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo("Declined");
    }

    @Test
    void blocksStaleKyc() {
        LoanApplication app = app(verifiedClient(400), "300000", "160000");
        when(repository.findById(42L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> service().qualify(42L))
                .isInstanceOf(LoanApplicationException.class)
                .hasMessageContaining("KYC not current");

        verify(repository, never()).save(any());
    }

    @Test
    void throwsWhenApplicationMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().qualify(99L))
                .isInstanceOf(LoanApplicationException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void monthlyPaymentMatchesAmortizationFormula() {
        // 300k @ 7.49% over 300 months -> ~2214/mo (sanity check of the formula).
        BigDecimal payment = LoanApplicationService.monthlyPayment(
                new BigDecimal("300000"), new BigDecimal("0.0749"), 300);
        assertThat(payment).isBetween(new BigDecimal("2200"), new BigDecimal("2230"));
    }

    @Test
    void changeStatusStampsSubmittedDateOnDraftToSubmitted() {
        LoanApplication app = new LoanApplication();
        app.setId(7L);
        app.setStatus("Draft");
        when(repository.findById(7L)).thenReturn(Optional.of(app));

        Instant before = Instant.now();
        service().changeStatus(7L, "Submitted");

        assertThat(app.getStatus()).isEqualTo("Submitted");
        assertThat(app.getSubmittedDate()).isNotNull();
        assertThat(app.getSubmittedDate()).isAfterOrEqualTo(before);
        verify(repository).save(app);
    }

    @Test
    void changeStatusRejectsIllegalTransition() {
        LoanApplication app = new LoanApplication();
        app.setId(8L);
        app.setStatus("Declined");
        when(repository.findById(8L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> service().changeStatus(8L, "Approved"))
                .isInstanceOf(IllegalStatusTransitionException.class)
                .hasMessageContaining("Illegal status transition: Declined");

        verify(repository, never()).save(any());
    }

    @Test
    void validatorAllowsSubmittedToApprovedAndRejectsFundedFromSubmitted() {
        // Submitted -> Approved is legal (this is what qualify() persists).
        validator.validateTransition("Submitted", "Approved");

        assertThatThrownBy(() -> validator.validateTransition("Submitted", "Funded"))
                .isInstanceOf(IllegalStatusTransitionException.class)
                .hasMessageContaining("Illegal status transition: Submitted \u2192 Funded");
    }
}
