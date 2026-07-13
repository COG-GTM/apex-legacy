package com.cibc.cai.service;

import com.cibc.cai.entity.Client;
import com.cibc.cai.entity.LoanApplication;
import com.cibc.cai.repository.LoanApplicationRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core loan/mortgage qualification engine and status state machine, ported from
 * the Apex {@code LoanApplicationService} and {@code LoanApplicationTriggerHandler}.
 *
 * <p>Encodes GDS/TDS qualification with a +2% stress test on the offered rate and
 * a KYC-currency gate (verified within 365 days).
 *
 * <h2>Decimal semantics</h2>
 * Apex {@code Decimal} arithmetic is emulated with {@link BigDecimal} using
 * {@link MathContext#DECIMAL64} (16 significant digits, {@link RoundingMode#HALF_EVEN})
 * for every division and for the integer {@code (1 + r)^months} power, which keeps
 * the resulting GDS/TDS ratios and the stress-tested rate identical to the legacy
 * engine for the migrated test cases. Ratio strings in decline reasons are rounded
 * to 4 decimal places with {@link RoundingMode#HALF_UP} to mirror the Apex
 * {@code Decimal.setScale(4)} default.
 */
@Service
public class LoanApplicationService {

    static final BigDecimal MAX_GDS = new BigDecimal("0.39");
    static final BigDecimal MAX_TDS = new BigDecimal("0.44");
    static final BigDecimal STRESS_TEST_BUFFER = new BigDecimal("0.02");

    private static final BigDecimal TWELVE = new BigDecimal("12");
    private static final MathContext MC = MathContext.DECIMAL64;
    private static final long MAX_KYC_AGE_DAYS = 365L;

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanApplicationStatusValidator statusValidator;

    public LoanApplicationService(
            LoanApplicationRepository loanApplicationRepository,
            LoanApplicationStatusValidator statusValidator) {
        this.loanApplicationRepository = loanApplicationRepository;
        this.statusValidator = statusValidator;
    }

    /**
     * Qualifies an application using stress-tested payments.
     * Rule: GDS &lt;= 39%, TDS &lt;= 44%, rate stress-tested at offered + 2%.
     *
     * <p>Persistence runs inside a single transaction ({@link Transactional}),
     * replacing the Apex {@code Database.setSavepoint()/rollback} pattern: on a
     * persistence failure the transaction rolls back and the failure is wrapped in
     * a {@link LoanApplicationException}.
     */
    @Transactional
    public QualificationResult qualify(Long applicationId) {
        LoanApplication app = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new LoanApplicationException(
                        "Loan application not found: " + applicationId));

        assertKycCurrent(app);

        BigDecimal stressTestedRate = app.getOfferedRate().add(STRESS_TEST_BUFFER);

        BigDecimal monthlyPayment = monthlyPayment(
                app.getAmount(), stressTestedRate, app.getAmortizationMonths());
        BigDecimal monthlyIncome = app.getAnnualIncome().divide(TWELVE, MC);

        BigDecimal gdsRatio = monthlyPayment
                .add(app.getMonthlyHousingCosts())
                .divide(monthlyIncome, MC);
        BigDecimal tdsRatio = monthlyPayment
                .add(app.getMonthlyHousingCosts())
                .add(app.getMonthlyDebtPayments())
                .divide(monthlyIncome, MC);

        boolean qualified;
        String declineReason;
        if (gdsRatio.compareTo(MAX_GDS) > 0) {
            qualified = false;
            declineReason = "GDS ratio " + gdsRatio.setScale(4, RoundingMode.HALF_UP)
                    + " exceeds 39%";
        } else if (tdsRatio.compareTo(MAX_TDS) > 0) {
            qualified = false;
            declineReason = "TDS ratio " + tdsRatio.setScale(4, RoundingMode.HALF_UP)
                    + " exceeds 44%";
        } else {
            qualified = true;
            declineReason = null;
        }

        QualificationResult result = new QualificationResult(
                qualified, gdsRatio, tdsRatio, stressTestedRate, declineReason);

        persistDecision(app, result);
        return result;
    }

    /**
     * Standard amortized monthly payment: {@code principal * (r*factor)/(factor-1)}
     * with {@code r = annualRate/12} and {@code factor = (1 + r)^months}.
     * Package-private to mirror the Apex {@code @TestVisible} method.
     */
    static BigDecimal monthlyPayment(BigDecimal principal, BigDecimal annualRate, int months) {
        BigDecimal r = annualRate.divide(TWELVE, MC);
        BigDecimal factor = BigDecimal.ONE.add(r).pow(months, MC);
        BigDecimal numerator = principal.multiply(r.multiply(factor, MC), MC);
        return numerator.divide(factor.subtract(BigDecimal.ONE), MC);
    }

    /** KYC gating: verified within the last 365 days (inclusive) or the application is blocked. */
    private void assertKycCurrent(LoanApplication app) {
        Client client = app.getClient();
        boolean verified = client != null
                && "Verified".equals(client.getKycStatus())
                && client.getKycVerifiedDate() != null
                && ChronoUnit.DAYS.between(client.getKycVerifiedDate(), LocalDate.now())
                        <= MAX_KYC_AGE_DAYS;
        if (!verified) {
            Long clientId = client == null ? null : client.getId();
            throw new LoanApplicationException(
                    "KYC not current for client " + clientId + "; application blocked");
        }
    }

    private void persistDecision(LoanApplication app, QualificationResult result) {
        try {
            app.setStatus(result.qualified() ? "Approved" : "Declined");
            app.setGdsRatio(result.gdsRatio());
            app.setTdsRatio(result.tdsRatio());
            app.setDeclineReason(result.declineReason());
            loanApplicationRepository.save(app);
        } catch (DataAccessException e) {
            throw new LoanApplicationException("Failed to persist decision: " + e.getMessage(), e);
        }
    }

    /**
     * Changes an application's status, enforcing the
     * {@link LoanApplicationStatusValidator} state machine. Ports the Apex
     * {@code before update} trigger: an illegal transition throws, and a
     * {@code Draft -> Submitted} transition stamps {@code submittedDate}.
     */
    @Transactional
    public void changeStatus(Long applicationId, String newStatus) {
        LoanApplication app = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new LoanApplicationException(
                        "Loan application not found: " + applicationId));

        String currentStatus = app.getStatus();
        if (newStatus == null || newStatus.equals(currentStatus)) {
            return;
        }

        statusValidator.validateTransition(currentStatus, newStatus);

        if ("Draft".equals(currentStatus) && "Submitted".equals(newStatus)) {
            app.setSubmittedDate(Instant.now());
        }
        app.setStatus(newStatus);
        loanApplicationRepository.save(app);
    }
}
