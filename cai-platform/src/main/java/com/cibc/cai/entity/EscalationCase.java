package com.cibc.cai.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Local model of the Salesforce standard {@code Case} created in
 * {@code TransactionDisputeService.escalate}. Only the fields actually set in
 * Apex are captured (Subject, Priority, Origin, AccountId -&gt; Client).
 *
 * <p>Kept as a local table with no external ticketing integration; a follow-up
 * could swap this for a ServiceNow/Jira integration.
 */
@Entity
@Table(name = "escalation_case")
public class EscalationCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @jakarta.persistence.Column(name = "subject")
    private String subject;

    @jakarta.persistence.Column(name = "priority")
    private String priority;

    @jakarta.persistence.Column(name = "origin")
    private String origin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
