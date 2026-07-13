package com.cibc.cai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Relational redesign of the Salesforce {@code Account} Person Account
 * (a merged Account+Contact used for B2C retail-banking clients).
 */
@Entity
@Table(
        name = "client",
        indexes = {
                @Index(name = "idx_client_sin_hash", columnList = "sin_hash"),
                @Index(name = "idx_client_sin_hash_dob", columnList = "sin_hash, date_of_birth")
        }
)
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    /** SHA-256 hex of the client's SIN, used for PII-safe dedupe. */
    @Column(name = "sin_hash", length = 64)
    private String sinHash;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    /** Salesforce {@code BillingState}. */
    @Column(name = "province")
    private String province;

    /** Values used in code: Pending, Verified, Failed, Error. */
    @Column(name = "kyc_status")
    private String kycStatus;

    @Column(name = "kyc_verified_date")
    private LocalDate kycVerifiedDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getSinHash() {
        return sinHash;
    }

    public void setSinHash(String sinHash) {
        this.sinHash = sinHash;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(String kycStatus) {
        this.kycStatus = kycStatus;
    }

    public LocalDate getKycVerifiedDate() {
        return kycVerifiedDate;
    }

    public void setKycVerifiedDate(LocalDate kycVerifiedDate) {
        this.kycVerifiedDate = kycVerifiedDate;
    }
}
