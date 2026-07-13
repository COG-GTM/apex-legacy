package com.cibc.cai.controller.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * REST request body for a loan application status change. The target status is
 * validated by the service-layer state machine.
 */
public record StatusChangeRequest(@NotBlank String status) {
}
