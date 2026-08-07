package com.cai.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Transaction dispute (formerly Salesforce Transaction_Dispute__c).
 */
@Entity
@Table(name = "transaction_dispute")
public class TransactionDispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(name = "card_last4", length = 4)
    private String cardLast4;

    @Column(name = "amount", precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "reason_code")
    private String reasonCode;

    @Column(name = "card_present")
    private Boolean cardPresent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DisputeStatus status;

    @Column(name = "filed_date")
    private Instant filedDate;

    @Column(name = "sla_due_date")
    private LocalDate slaDueDate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escalation_case_id")
    private EscalationCase escalationCase;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Boolean getCardPresent() {
        return cardPresent;
    }

    public void setCardPresent(Boolean cardPresent) {
        this.cardPresent = cardPresent;
    }

    public DisputeStatus getStatus() {
        return status;
    }

    public void setStatus(DisputeStatus status) {
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
