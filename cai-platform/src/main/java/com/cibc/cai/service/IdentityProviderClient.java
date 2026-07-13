package com.cibc.cai.service;

/**
 * Mockable seam for the external identity-provider verification callout.
 *
 * <p>Ports the Apex {@code Http().send(req)} call against
 * {@code callout:Identity_Provider/v2/verify}. Kept behind an interface so unit
 * tests can stub HTTP responses without a live endpoint or Spring context.
 */
public interface IdentityProviderClient {

    /**
     * POST {@code {"sinHash": ..., "dob": ...}} to the identity provider.
     *
     * @param sinHash the client's SHA-256 hashed SIN
     * @param dob     the client's date of birth as a String
     * @return the HTTP status code and parsed {@code verified} flag
     */
    VerificationResult verify(String sinHash, String dob);

    /**
     * Outcome of a verification callout.
     *
     * @param statusCode HTTP status returned by the identity provider
     * @param verified   the {@code verified} flag from the response body, or
     *                   {@code null} when absent / non-200
     */
    record VerificationResult(int statusCode, Boolean verified) {
    }
}
