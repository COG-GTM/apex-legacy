package com.cai.platform.dispute;

import com.cai.platform.domain.TransactionDispute;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/disputes")
public class DisputeController {

    private final TransactionDisputeService disputeService;

    public DisputeController(TransactionDisputeService disputeService) {
        this.disputeService = disputeService;
    }

    public record FileDisputeRequest(
            @NotNull Long clientId,
            String cardLast4,
            BigDecimal amount,
            String reasonCode,
            boolean cardPresent) {
    }

    public record DisputeResponse(
            Long id,
            Long clientId,
            String cardLast4,
            BigDecimal amount,
            String reasonCode,
            Boolean cardPresent,
            String status,
            Instant filedDate,
            LocalDate slaDueDate,
            Long escalationCaseId) {

        static DisputeResponse from(TransactionDispute dispute) {
            return new DisputeResponse(
                    dispute.getId(),
                    dispute.getClient().getId(),
                    dispute.getCardLast4(),
                    dispute.getAmount(),
                    dispute.getReasonCode(),
                    dispute.getCardPresent(),
                    dispute.getStatus().name(),
                    dispute.getFiledDate(),
                    dispute.getSlaDueDate(),
                    dispute.getEscalationCase() != null ? dispute.getEscalationCase().getId() : null);
        }
    }

    @PostMapping
    public ResponseEntity<DisputeResponse> fileDispute(@Valid @RequestBody FileDisputeRequest request) {
        TransactionDispute dispute = disputeService.fileDispute(
                request.clientId(), request.cardLast4(), request.amount(),
                request.reasonCode(), request.cardPresent());
        return ResponseEntity.status(HttpStatus.CREATED).body(DisputeResponse.from(dispute));
    }

    @ExceptionHandler(DisputeException.class)
    public ResponseEntity<Map<String, String>> handleDisputeException(DisputeException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
