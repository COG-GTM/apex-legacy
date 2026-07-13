package com.cai.platform.onboarding;

import com.cai.platform.domain.Client;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/clients")
public class ClientOnboardingController {

    private final ClientOnboardingService clientOnboardingService;

    public ClientOnboardingController(ClientOnboardingService clientOnboardingService) {
        this.clientOnboardingService = clientOnboardingService;
    }

    @PostMapping("/onboard")
    public ResponseEntity<ClientResponse> onboard(@RequestBody OnboardingRequest request) {
        Client client = clientOnboardingService.onboard(request);
        return ResponseEntity.ok(ClientResponse.from(client));
    }

    @ExceptionHandler(OnboardingException.class)
    public ResponseEntity<Map<String, String>> handleOnboardingException(OnboardingException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }
}
