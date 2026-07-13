package com.cibc.cai.service;

import java.math.BigDecimal;

/**
 * Outcome of {@link LoanApplicationService#qualify(Long)}. Ports the legacy Apex
 * inner class {@code LoanApplicationService.QualificationResult}.
 *
 * @param qualified        whether the application passed GDS/TDS qualification
 * @param gdsRatio         computed Gross Debt Service ratio
 * @param tdsRatio         computed Total Debt Service ratio
 * @param stressTestedRate offered rate plus the +2% regulatory stress buffer
 * @param declineReason    human-readable decline reason, or {@code null} when qualified
 */
public record QualificationResult(
        boolean qualified,
        BigDecimal gdsRatio,
        BigDecimal tdsRatio,
        BigDecimal stressTestedRate,
        String declineReason) {
}
