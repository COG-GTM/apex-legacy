package com.cibc.cai.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * REST request body for filing a transaction dispute.
 */
public record DisputeRequest(
        @NotNull Long clientId,
        @NotBlank String cardLast4,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String reasonCode,
        boolean cardPresent) {
}
