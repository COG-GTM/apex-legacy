package com.cibc.cai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Relational model of the Salesforce {@code Transaction_Dispute__c} custom
 * object. Status values used: Open, Escalated.
 */
@Entity
@Table(name = "transaction_dispute")
public class TransactionDispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mirrors the Salesforce AutoNumber {@code TD-{000000}} name field. */
    @Column(name = "dispute_number")
    private String disputeNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "card_last4", length = 4)
    private String cardLast4;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "reason_code", length = 40)
    private String reasonCode;

    @Column(name = "card_present", nullable = false)
    private boolean cardPresent = false;

    /** Values: Open, Escalated. */
    @Column(name = "status")
    private String status;

    @Column(name = "filed_date")
    private Instant filedDate;

    /**
     * SLA due date. Intentionally a DATE (not a timestamp): the Apex service
     * computes {@code Date.today().addDays(slaHours / 24)} (48/24 = +2 days),
     * and the service phase preserves that behavior. Do not promote to a
     * timestamp here.
     */
    @Column(name = "sla_due_date")
    private LocalDate slaDueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escalation_case_id")
    private EscalationCase escalationCase;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDisputeNumber() {
        return disputeNumber;
    }

    public void setDisputeNumber(String disputeNumber) {
        this.disputeNumber = disputeNumber;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getCardLast4() {
        return cardLast4;
    }

    public void setCardLast4(String cardLast4) {
        this.cardLast4 = cardLast4;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public boolean isCardPresent() {
        return cardPresent;
    }

    public void setCardPresent(boolean cardPresent) {
        this.cardPresent = cardPresent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getFiledDate() {
        return filedDate;
    }

    public void setFiledDate(Instant filedDate) {
        this.filedDate = filedDate;
    }

    public LocalDate getSlaDueDate() {
        return slaDueDate;
    }

    public void setSlaDueDate(LocalDate slaDueDate) {
        this.slaDueDate = slaDueDate;
    }

    public EscalationCase getEscalationCase() {
        return escalationCase;
    }

    public void setEscalationCase(EscalationCase escalationCase) {
        this.escalationCase = escalationCase;
    }
}
