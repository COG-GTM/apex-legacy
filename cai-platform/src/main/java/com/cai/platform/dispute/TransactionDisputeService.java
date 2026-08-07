package com.cai.platform.dispute;

import com.cai.platform.domain.Client;
import com.cai.platform.domain.DisputeStatus;
import com.cai.platform.domain.EscalationCase;
import com.cai.platform.domain.TransactionDispute;
import com.cai.platform.repository.ClientRepository;
import com.cai.platform.repository.EscalationCaseRepository;
import com.cai.platform.repository.TransactionDisputeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Transaction dispute intake and SLA escalation.
 * Disputes > $500 or card-present fraud escalate to a case with a 48h SLA;
 * everything else gets a 10-business-day SLA.
 */
@Service
public class TransactionDisputeService {

    private static final BigDecimal ESCALATION_AMOUNT = new BigDecimal("500");
    private static final int URGENT_SLA_HOURS = 48;
    private static final int STANDARD_SLA_BUSINESS_DAYS = 10;

    private final TransactionDisputeRepository disputeRepository;
    private final EscalationCaseRepository escalationCaseRepository;
    private final ClientRepository clientRepository;

    public TransactionDisputeService(TransactionDisputeRepository disputeRepository,
                                     EscalationCaseRepository escalationCaseRepository,
                                     ClientRepository clientRepository) {
        this.disputeRepository = disputeRepository;
        this.escalationCaseRepository = escalationCaseRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional
    public TransactionDispute fileDispute(Long clientId, String cardLast4, BigDecimal amount,
                                          String reasonCode, boolean cardPresent) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DisputeException("Dispute amount must be positive");
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new DisputeException("Client not found: " + clientId));

        TransactionDispute dispute = new TransactionDispute();
        dispute.setClient(client);
        dispute.setCardLast4(cardLast4);
        dispute.setAmount(amount);
        dispute.setReasonCode(reasonCode);
        dispute.setCardPresent(cardPresent);
        dispute.setStatus(DisputeStatus.OPEN);
        dispute.setFiledDate(Instant.now());
        dispute = disputeRepository.save(dispute);

        if (requiresUrgentEscalation(amount, reasonCode, cardPresent)) {
            escalate(dispute, URGENT_SLA_HOURS);
        } else {
            dispute.setSlaDueDate(addBusinessDays(LocalDate.now(), STANDARD_SLA_BUSINESS_DAYS));
        }
        return disputeRepository.save(dispute);
    }

    boolean requiresUrgentEscalation(BigDecimal amount, String reasonCode, boolean cardPresent) {
        boolean fraudCode = "FRAUD_10.4".equals(reasonCode) || "FRAUD_10.1".equals(reasonCode);
        return amount.compareTo(ESCALATION_AMOUNT) > 0 || (fraudCode && cardPresent);
    }

    private void escalate(TransactionDispute dispute, int slaHours) {
        EscalationCase escalation = new EscalationCase();
        escalation.setSubject("URGENT dispute " + dispute.getId() + " — $" + dispute.getAmount());
        escalation.setPriority("High");
        escalation.setOrigin("Dispute Intake");
        escalation.setClient(dispute.getClient());
        escalation.setCreatedDate(Instant.now());
        escalation = escalationCaseRepository.save(escalation);

        dispute.setStatus(DisputeStatus.ESCALATED);
        dispute.setEscalationCase(escalation);
        dispute.setSlaDueDate(LocalDate.now().plusDays(slaHours / 24));
    }

    LocalDate addBusinessDays(LocalDate start, int businessDays) {
        LocalDate result = start;
        int added = 0;
        while (added < businessDays) {
            result = result.plusDays(1);
            DayOfWeek dayOfWeek = result.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return result;
    }
}
