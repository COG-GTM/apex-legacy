package com.cibc.cai.service;

import com.cibc.cai.config.IdentityProviderProperties;
import java.util.Map;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * {@link RestClient}-backed implementation of {@link IdentityProviderClient}.
 * Performs the JSON POST to {@code identity-provider.verify-path} and surfaces
 * the raw HTTP status plus the parsed {@code verified} flag; non-2xx statuses
 * are returned rather than thrown so the caller can mirror the Apex branching.
 */
@Component
public class RestClientIdentityProviderClient implements IdentityProviderClient {

    private final RestClient restClient;
    private final IdentityProviderProperties properties;

    public RestClientIdentityProviderClient(RestClient identityProviderRestClient,
                                            IdentityProviderProperties properties) {
        this.restClient = identityProviderRestClient;
        this.properties = properties;
    }

    @Override
    public VerificationResult verify(String sinHash, String dob) {
        Map<String, Object> body = Map.of("sinHash", sinHash, "dob", dob);

        ResponseEntity<Map> response = restClient.post()
                .uri(properties.getVerifyPath())
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> { })
                .toEntity(Map.class);

        Boolean verified = null;
        if (response.getStatusCode().value() == 200 && response.getBody() != null) {
            Object raw = response.getBody().get("verified");
            if (raw instanceof Boolean b) {
                verified = b;
            }
        }
        return new VerificationResult(response.getStatusCode().value(), verified);
    }
}
