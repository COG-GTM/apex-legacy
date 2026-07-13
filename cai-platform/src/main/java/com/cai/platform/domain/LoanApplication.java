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
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Loan/mortgage application (formerly Salesforce Loan_Application__c).
 */
@Entity
@Table(name = "loan_application")
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(name = "amount", precision = 14, scale = 2)
    private BigDecimal amount;

    /** Annual offered interest rate as a decimal fraction (e.g. 0.0549). */
    @Column(name = "offered_rate", precision = 7, scale = 5)
    private BigDecimal offeredRate;

    @Column(name = "amortization_months")
    private Integer amortizationMonths;

    @Column(name = "annual_income", precision = 14, scale = 2)
    private BigDecimal annualIncome;

    @Column(name = "monthly_housing_costs", precision = 12, scale = 2)
    private BigDecimal monthlyHousingCosts;

    @Column(name = "monthly_debt_payments", precision = 12, scale = 2)
    private BigDecimal monthlyDebtPayments;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private LoanApplicationStatus status;

    @Column(name = "gds_ratio", precision = 8, scale = 5)
    private BigDecimal gdsRatio;

    @Column(name = "tds_ratio", precision = 8, scale = 5)
    private BigDecimal tdsRatio;

    @Column(name = "decline_reason")
    private String declineReason;

    @Column(name = "submitted_date")
    private Instant submittedDate;

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

    public LoanApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(LoanApplicationStatus status) {
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
}
