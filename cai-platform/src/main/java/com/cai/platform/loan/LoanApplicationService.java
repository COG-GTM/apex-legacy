package com.cai.platform.loan;

import com.cai.platform.domain.Client;
import com.cai.platform.domain.KycStatus;
import com.cai.platform.domain.LoanApplication;
import com.cai.platform.domain.LoanApplicationStatus;
import com.cai.platform.repository.LoanApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Core loan/mortgage application logic (formerly Apex LoanApplicationService
 * and LoanApplicationTriggerHandler).
 * Encodes GDS/TDS qualification with a +2% stress test on the offered rate,
 * KYC gating, and status transition enforcement.
 */
@Service
public class LoanApplicationService {

    private static final BigDecimal MAX_GDS = new BigDecimal("0.39");
    private static final BigDecimal MAX_TDS = new BigDecimal("0.44");
    private static final BigDecimal STRESS_TEST_BUFFER = new BigDecimal("0.02");
    private static final BigDecimal TWELVE = new BigDecimal("12");
    private static final long KYC_MAX_AGE_DAYS = 365;
    private static final MathContext MC = new MathContext(20);

    private static final Map<LoanApplicationStatus, Set<LoanApplicationStatus>> ALLOWED_TRANSITIONS;

    static {
        ALLOWED_TRANSITIONS = new EnumMap<>(LoanApplicationStatus.class);
        ALLOWED_TRANSITIONS.put(LoanApplicationStatus.DRAFT,
                EnumSet.of(LoanApplicationStatus.SUBMITTED, LoanApplicationStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(LoanApplicationStatus.SUBMITTED,
                EnumSet.of(LoanApplicationStatus.APPROVED, LoanApplicationStatus.DECLINED,
                        LoanApplicationStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(LoanApplicationStatus.APPROVED,
                EnumSet.of(LoanApplicationStatus.FUNDED, LoanApplicationStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(LoanApplicationStatus.DECLINED, EnumSet.noneOf(LoanApplicationStatus.class));
        ALLOWED_TRANSITIONS.put(LoanApplicationStatus.FUNDED, EnumSet.noneOf(LoanApplicationStatus.class));
        ALLOWED_TRANSITIONS.put(LoanApplicationStatus.CANCELLED, EnumSet.noneOf(LoanApplicationStatus.class));
    }

    private final LoanApplicationRepository loanApplicationRepository;

    public LoanApplicationService(LoanApplicationRepository loanApplicationRepository) {
        this.loanApplicationRepository = loanApplicationRepository;
    }

    /**
     * Qualifies an application using stress-tested payments.
     * Rule: GDS <= 39%, TDS <= 44%, rate stress-tested at offered + 2%.
     */
    @Transactional
    public QualificationResult qualify(Long applicationId) {
        LoanApplication app = loadApplication(applicationId);
        assertKycCurrent(app);

        BigDecimal stressTestedRate = app.getOfferedRate().add(STRESS_TEST_BUFFER);
        BigDecimal payment = monthlyPayment(app.getAmount(), stressTestedRate, app.getAmortizationMonths());
        BigDecimal monthlyIncome = app.getAnnualIncome().divide(TWELVE, MC);

        BigDecimal gds = payment.add(app.getMonthlyHousingCosts()).divide(monthlyIncome, MC);
        BigDecimal tds = payment.add(app.getMonthlyHousingCosts())
                .add(app.getMonthlyDebtPayments()).divide(monthlyIncome, MC);

        boolean qualified;
        String declineReason = null;
        if (gds.compareTo(MAX_GDS) > 0) {
            qualified = false;
            declineReason = "GDS ratio " + gds.setScale(4, RoundingMode.HALF_UP) + " exceeds 39%";
        } else if (tds.compareTo(MAX_TDS) > 0) {
            qualified = false;
            declineReason = "TDS ratio " + tds.setScale(4, RoundingMode.HALF_UP) + " exceeds 44%";
        } else {
            qualified = true;
        }

        app.setStatus(qualified ? LoanApplicationStatus.APPROVED : LoanApplicationStatus.DECLINED);
        app.setGdsRatio(gds.setScale(5, RoundingMode.HALF_UP));
        app.setTdsRatio(tds.setScale(5, RoundingMode.HALF_UP));
        app.setDeclineReason(declineReason);
        loanApplicationRepository.save(app);

        return new QualificationResult(qualified, gds, tds, stressTestedRate, declineReason);
    }

    /**
     * Enforces allowed status transitions; stamps submittedDate on
     * DRAFT -> SUBMITTED.
     */
    @Transactional
    public LoanApplication updateStatus(Long applicationId, LoanApplicationStatus newStatus) {
        LoanApplication app = loadApplication(applicationId);
        LoanApplicationStatus from = app.getStatus();

        if (from != newStatus) {
            Set<LoanApplicationStatus> allowed = ALLOWED_TRANSITIONS.get(from);
            if (allowed == null || !allowed.contains(newStatus)) {
                throw new IllegalStatusTransitionException(from, newStatus);
            }
            if (from == LoanApplicationStatus.DRAFT && newStatus == LoanApplicationStatus.SUBMITTED) {
                app.setSubmittedDate(Instant.now());
            }
            app.setStatus(newStatus);
            loanApplicationRepository.save(app);
        }
        return app;
    }

    /** Standard amortized monthly payment. */
    static BigDecimal monthlyPayment(BigDecimal principal, BigDecimal annualRate, int months) {
        BigDecimal r = annualRate.divide(TWELVE, MC);
        BigDecimal factor = BigDecimal.ONE.add(r).pow(months, MC);
        return principal.multiply(r.multiply(factor, MC), MC)
                .divide(factor.subtract(BigDecimal.ONE), MC);
    }

    /** KYC gating: verified within the last 365 days or the application cannot proceed. */
    private void assertKycCurrent(LoanApplication app) {
        Client client = app.getClient();
        LocalDate verifiedDate = client.getKycVerifiedDate();
        boolean verified = client.getKycStatus() == KycStatus.VERIFIED
                && verifiedDate != null
                && ChronoUnit.DAYS.between(verifiedDate, LocalDate.now()) <= KYC_MAX_AGE_DAYS;
        if (!verified) {
            throw new LoanApplicationException(
                    "KYC not current for client " + client.getId() + "; application blocked");
        }
    }

    private LoanApplication loadApplication(Long applicationId) {
        return loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new LoanApplicationException(
                        "Loan application not found: " + applicationId));
    }
}
