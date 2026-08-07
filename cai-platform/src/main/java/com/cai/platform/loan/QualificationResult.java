package com.cai.platform.loan;

import java.math.BigDecimal;

/**
 * Outcome of qualifying a loan application (formerly
 * LoanApplicationService.QualificationResult in Apex).
 */
public record QualificationResult(
        boolean qualified,
        BigDecimal gdsRatio,
        BigDecimal tdsRatio,
        BigDecimal stressTestedRate,
        String declineReason) {
}
