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

/**
 * Relational model of the Salesforce {@code Loan_Application__c} custom object.
 * Status state machine values: Draft, Submitted, Approved, Declined, Funded,
 * Cancelled.
 */
@Entity
@Table(name = "loan_application")
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mirrors the Salesforce AutoNumber {@code LA-{000000}} name field. */
    @Column(name = "application_number")
    private String applicationNumber;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "offered_rate", precision = 18, scale = 4)
    private BigDecimal offeredRate;

    @Column(name = "amortization_months")
    private Integer amortizationMonths;

    @Column(name = "annual_income", precision = 18, scale = 2)
    private BigDecimal annualIncome;

    @Column(name = "monthly_housing_costs", precision = 18, scale = 2)
    private BigDecimal monthlyHousingCosts;

    @Column(name = "monthly_debt_payments", precision = 18, scale = 2)
    private BigDecimal monthlyDebtPayments;

    /** Values: Draft, Submitted, Approved, Declined, Funded, Cancelled. */
    @Column(name = "status")
    private String status;

    @Column(name = "gds_ratio", precision = 18, scale = 4)
    private BigDecimal gdsRatio;

    @Column(name = "tds_ratio", precision = 18, scale = 4)
    private BigDecimal tdsRatio;

    @Column(name = "decline_reason", length = 255)
    private String declineReason;

    @Column(name = "submitted_date")
    private Instant submittedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getApplicationNumber() {
        return applicationNumber;
    }

    public void setApplicationNumber(String applicationNumber) {
        this.applicationNumber = applicationNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getOfferedRate() {
        return offeredRate;
    }

    public void setOfferedRate(BigDecimal offeredRate) {
        this.offeredRate = offeredRate;
    }

    public Integer getAmortizationMonths() {
        return amortizationMonths;
    }

    public void setAmortizationMonths(Integer amortizationMonths) {
        this.amortizationMonths = amortizationMonths;
    }

    public BigDecimal getAnnualIncome() {
        return annualIncome;
    }

    public void setAnnualIncome(BigDecimal annualIncome) {
        this.annualIncome = annualIncome;
    }

    public BigDecimal getMonthlyHousingCosts() {
        return monthlyHousingCosts;
    }

    public void setMonthlyHousingCosts(BigDecimal monthlyHousingCosts) {
        this.monthlyHousingCosts = monthlyHousingCosts;
    }

    public BigDecimal getMonthlyDebtPayments() {
        return monthlyDebtPayments;
    }

    public void setMonthlyDebtPayments(BigDecimal monthlyDebtPayments) {
        this.monthlyDebtPayments = monthlyDebtPayments;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getGdsRatio() {
        return gdsRatio;
    }

    public void setGdsRatio(BigDecimal gdsRatio) {
        this.gdsRatio = gdsRatio;
    }

    public BigDecimal getTdsRatio() {
        return tdsRatio;
    }

    public void setTdsRatio(BigDecimal tdsRatio) {
        this.tdsRatio = tdsRatio;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public void setDeclineReason(String declineReason) {
        this.declineReason = declineReason;
    }

    public Instant getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(Instant submittedDate) {
        this.submittedDate = submittedDate;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
