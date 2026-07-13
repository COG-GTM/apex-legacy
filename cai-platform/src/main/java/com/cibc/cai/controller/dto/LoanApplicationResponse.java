package com.cibc.cai.controller.dto;

import com.cibc.cai.entity.LoanApplication;
import java.math.BigDecimal;

/**
 * REST response view of a {@link LoanApplication}'s current status and decision.
 * Serialized instead of the JPA entity so the lazy {@code client} relation is
 * never touched during marshalling.
 */
public record LoanApplicationResponse(
        Long id,
        String applicationNumber,
        String status,
        BigDecimal gdsRatio,
        BigDecimal tdsRatio,
        String declineReason) {

    public static LoanApplicationResponse from(LoanApplication app) {
        return new LoanApplicationResponse(
                app.getId(),
                app.getApplicationNumber(),
                app.getStatus(),
                app.getGdsRatio(),
                app.getTdsRatio(),
                app.getDeclineReason());
    }
}
