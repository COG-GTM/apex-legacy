package com.cibc.cai.controller;

import com.cibc.cai.controller.dto.LoanApplicationResponse;
import com.cibc.cai.controller.dto.StatusChangeRequest;
import com.cibc.cai.entity.LoanApplication;
import com.cibc.cai.repository.LoanApplicationRepository;
import com.cibc.cai.service.LoanApplicationService;
import com.cibc.cai.service.QualificationResult;
import jakarta.validation.Valid;
import java.util.NoSuchElementException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for loan-application qualification and the status state machine.
 * Delegates to {@link LoanApplicationService}; read-only lookups use the
 * repository directly rather than adding a read method to the service.
 */
@RestController
@RequestMapping("/api/loan-applications")
public class LoanApplicationController {

    private final LoanApplicationService loanApplicationService;
    private final LoanApplicationRepository loanApplicationRepository;

    public LoanApplicationController(LoanApplicationService loanApplicationService,
                                     LoanApplicationRepository loanApplicationRepository) {
        this.loanApplicationService = loanApplicationService;
        this.loanApplicationRepository = loanApplicationRepository;
    }

    /** Runs GDS/TDS qualification (with the +2% stress test) for an application. */
    @PostMapping("/{id}/qualify")
    public QualificationResult qualify(@PathVariable Long id) {
        return loanApplicationService.qualify(id);
    }

    /** Applies a state-machine transition and returns the updated application. */
    @PostMapping("/{id}/status")
    public LoanApplicationResponse changeStatus(@PathVariable Long id,
                                                @Valid @RequestBody StatusChangeRequest request) {
        loanApplicationService.changeStatus(id, request.status());
        return LoanApplicationResponse.from(load(id));
    }

    /** Reads an application's current status and qualification decision. */
    @GetMapping("/{id}")
    public LoanApplicationResponse get(@PathVariable Long id) {
        return LoanApplicationResponse.from(load(id));
    }

    private LoanApplication load(Long id) {
        return loanApplicationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Loan application not found: " + id));
    }
}
