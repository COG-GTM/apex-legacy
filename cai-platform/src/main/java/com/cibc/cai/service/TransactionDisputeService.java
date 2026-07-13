package com.cibc.cai.service;

import com.cibc.cai.entity.Client;
import com.cibc.cai.entity.EscalationCase;
import com.cibc.cai.entity.TransactionDispute;
import com.cibc.cai.repository.ClientRepository;
import com.cibc.cai.repository.EscalationCaseRepository;
import com.cibc.cai.repository.TransactionDisputeRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transaction dispute intake and SLA escalation. Ports the Apex
 * {@code TransactionDisputeService}.
 *
 * <p>Disputes over {@code $500}, or card-present fraud, escalate to an
 * {@link EscalationCase} with a "48h" SLA; everything else gets a
 * 10-business-day SLA.
 */
@Service
public class TransactionDisputeService {

    static final BigDecimal ESCALATION_AMOUNT = BigDecimal.valueOf(500);
    static final int URGENT_SLA_HOURS = 48;
    static final int STANDARD_SLA_BUSINESS_DAYS = 10;

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

        Client client = clientRepository.getReferenceById(clientId);

        TransactionDispute dispute = new TransactionDispute();
        dispute.setClient(client);
        dispute.setCardLast4(cardLast4);
        dispute.setAmount(amount);
        dispute.setReasonCode(reasonCode);
        dispute.setCardPresent(cardPresent);
        dispute.setStatus("Open");
        dispute.setFiledDate(Instant.now());
        dispute = disputeRepository.save(dispute);

        if (requiresUrgentEscalation(amount, reasonCode, cardPresent)) {
            escalate(dispute, URGENT_SLA_HOURS);
        } else {
            dispute.setSlaDueDate(addBusinessDays(LocalDate.now(), STANDARD_SLA_BUSINESS_DAYS));
            dispute = disputeRepository.save(dispute);
        }
        return dispute;
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
        escalation = escalationCaseRepository.save(escalation);

        dispute.setStatus("Escalated");
        dispute.setEscalationCase(escalation);
        // NOTE: legacy SLA granularity bug preserved (48h -> +2 days on a Date); see MIGRATION_NOTES.md
        dispute.setSlaDueDate(LocalDate.now().plusDays(slaHours / 24));
        disputeRepository.save(dispute);
    }

    LocalDate addBusinessDays(LocalDate start, int businessDays) {
        LocalDate result = start;
        int added = 0;
        while (added < businessDays) {
            result = result.plusDays(1);
            DayOfWeek day = result.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return result;
    }
}
