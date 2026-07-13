package com.cibc.cai.controller;

import com.cibc.cai.controller.dto.DisputeRequest;
import com.cibc.cai.controller.dto.DisputeResponse;
import com.cibc.cai.entity.TransactionDispute;
import com.cibc.cai.service.TransactionDisputeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for filing transaction disputes. Delegates to
 * {@link TransactionDisputeService}, which applies the SLA / escalation rules.
 */
@RestController
@RequestMapping("/api/disputes")
public class DisputeController {

    private final TransactionDisputeService disputeService;

    public DisputeController(TransactionDisputeService disputeService) {
        this.disputeService = disputeService;
    }

    /** Files a dispute and returns the created record (201). */
    @PostMapping
    public ResponseEntity<DisputeResponse> fileDispute(@Valid @RequestBody DisputeRequest request) {
        TransactionDispute dispute = disputeService.fileDispute(
                request.clientId(),
                request.cardLast4(),
                request.amount(),
                request.reasonCode(),
                request.cardPresent());
        return ResponseEntity.status(HttpStatus.CREATED).body(DisputeResponse.from(dispute));
    }
}
