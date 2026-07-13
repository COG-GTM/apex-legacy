package com.cai.platform.loan;

import com.cai.platform.domain.LoanApplication;
import com.cai.platform.domain.LoanApplicationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * REST endpoints for loan application qualification and status transitions.
 */
@RestController
@RequestMapping("/api/loan-applications")
public class LoanApplicationController {

    public record StatusUpdateRequest(@NotNull LoanApplicationStatus status) {
    }

    public record LoanApplicationResponse(
            Long id,
            LoanApplicationStatus status,
            BigDecimal gdsRatio,
            BigDecimal tdsRatio,
            String declineReason,
            Instant submittedDate) {

        static LoanApplicationResponse from(LoanApplication app) {
            return new LoanApplicationResponse(app.getId(), app.getStatus(), app.getGdsRatio(),
                    app.getTdsRatio(), app.getDeclineReason(), app.getSubmittedDate());
        }
    }

    private final LoanApplicationService loanApplicationService;

    public LoanApplicationController(LoanApplicationService loanApplicationService) {
        this.loanApplicationService = loanApplicationService;
    }

    @PostMapping("/{id}/qualify")
    public QualificationResult qualify(@PathVariable Long id) {
        return loanApplicationService.qualify(id);
    }

    @PatchMapping("/{id}/status")
    public LoanApplicationResponse updateStatus(@PathVariable Long id,
                                                @Valid @RequestBody StatusUpdateRequest request) {
        return LoanApplicationResponse.from(loanApplicationService.updateStatus(id, request.status()));
    }

    @ExceptionHandler(LoanApplicationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, String> handleLoanApplicationException(LoanApplicationException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(IllegalStatusTransitionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleIllegalTransition(IllegalStatusTransitionException e) {
        return Map.of("error", e.getMessage());
    }
}
