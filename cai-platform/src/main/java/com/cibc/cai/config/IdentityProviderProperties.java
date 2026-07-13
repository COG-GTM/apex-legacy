package com.cibc.cai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code identity-provider.*} keys from {@code application.yml} used by
 * the KYC verification callout (see {@code KYCVerificationService}). Replaces the
 * Salesforce Named Credential {@code callout:Identity_Provider}.
 */
@ConfigurationProperties(prefix = "identity-provider")
public class IdentityProviderProperties {

    /** Base URL of the identity provider (Named Credential host). */
    private String baseUrl;

    /** Verification path appended to {@link #baseUrl} (e.g. {@code /v2/verify}). */
    private String verifyPath;

    /** Bearer token used to authenticate the callout. */
    private String authToken;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getVerifyPath() {
        return verifyPath;
    }

    public void setVerifyPath(String verifyPath) {
        this.verifyPath = verifyPath;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
}
