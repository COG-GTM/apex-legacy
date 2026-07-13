package com.cibc.cai.controller.dto;

/**
 * Uniform JSON error body returned by {@code GlobalExceptionHandler}.
 *
 * @param error   short machine-readable error label (e.g. the exception name)
 * @param message human-readable detail
 */
public record ErrorResponse(String error, String message) {
}
