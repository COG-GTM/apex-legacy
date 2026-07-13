package com.cai.platform.onboarding;

import java.time.LocalDate;

/**
 * Incoming client onboarding request (formerly ClientOnboardingService.OnboardingRequest in Apex).
 */
public class OnboardingRequest {

    private String firstName;
    private String lastName;

    /** SHA-256 of SIN, never the raw value. */
    private String sinHash;

    private LocalDate dateOfBirth;
    private String email;
    private String phone;
    private String province;

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
}
