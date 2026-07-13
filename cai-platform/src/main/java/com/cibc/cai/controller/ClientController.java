package com.cibc.cai.controller;

import com.cibc.cai.controller.dto.ClientResponse;
import com.cibc.cai.controller.dto.OnboardingRequestDto;
import com.cibc.cai.entity.Client;
import com.cibc.cai.service.ClientOnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for client onboarding. Exercises the onboarding &rarr; KYC
 * cross-service integration: {@link ClientOnboardingService#onboard} persists a
 * new client and enqueues asynchronous KYC verification via the injected
 * {@code KycVerificationEnqueuer} bean.
 */
@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientOnboardingService onboardingService;

    public ClientController(ClientOnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    /**
     * Onboards (or dedupe-returns) a client. Returns 201 with the created or
     * matched client.
     */
    @PostMapping
    public ResponseEntity<ClientResponse> onboard(@Valid @RequestBody OnboardingRequestDto request) {
        Client client = onboardingService.onboard(request.toServiceRequest());
        return ResponseEntity.status(HttpStatus.CREATED).body(ClientResponse.from(client));
    }
}
