package com.cibc.cai.controller;

import com.cibc.cai.controller.dto.ErrorResponse;
import com.cibc.cai.service.DisputeException;
import com.cibc.cai.service.IllegalStatusTransitionException;
import com.cibc.cai.service.LoanApplicationException;
import com.cibc.cai.service.OnboardingException;
import jakarta.persistence.EntityNotFoundException;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps domain and validation exceptions to consistent JSON error responses so
 * controllers never leak stack traces or serialize raw exceptions.
 *
 * <ul>
 *   <li>{@link OnboardingException}, {@link DisputeException},
 *       {@link LoanApplicationException} and validation failures &rarr; 400</li>
 *   <li>{@link IllegalStatusTransitionException} (illegal state machine move) &rarr; 409</li>
 *   <li>entity-not-found &rarr; 404</li>
 *   <li>FK / integrity violations (e.g. unknown client id) &rarr; 409</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({OnboardingException.class, DisputeException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) {
        return build(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(IllegalStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleIllegalTransition(IllegalStatusTransitionException ex) {
        return build(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(LoanApplicationException.class)
    public ResponseEntity<ErrorResponse> handleLoanApplication(LoanApplicationException ex) {
        return build(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler({NoSuchElementException.class, EntityNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        return build(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DataIntegrityViolationException",
                        "Request violates a data integrity constraint"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("ValidationException",
                        message.isEmpty() ? "Validation failed" : message));
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, RuntimeException ex) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(ex.getClass().getSimpleName(), ex.getMessage()));
    }
}
