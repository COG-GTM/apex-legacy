package com.cibc.cai.controller.dto;

import com.cibc.cai.entity.TransactionDispute;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * REST response view of a filed {@link TransactionDispute}. Exposes the
 * escalation case id (when escalated) without serializing the lazy relations.
 */
public record DisputeResponse(
        Long id,
        Long clientId,
        BigDecimal amount,
        String status,
        LocalDate slaDueDate,
        Long escalationCaseId) {

    public static DisputeResponse from(TransactionDispute dispute) {
        Long clientId = dispute.getClient() == null ? null : dispute.getClient().getId();
        Long escalationCaseId =
                dispute.getEscalationCase() == null ? null : dispute.getEscalationCase().getId();
        return new DisputeResponse(
                dispute.getId(),
                clientId,
                dispute.getAmount(),
                dispute.getStatus(),
                dispute.getSlaDueDate(),
                escalationCaseId);
    }
}
