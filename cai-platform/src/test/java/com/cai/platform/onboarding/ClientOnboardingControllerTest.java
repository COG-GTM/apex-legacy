package com.cai.platform.onboarding;

import com.cai.platform.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClientOnboardingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRepository clientRepository;

    @MockitoBean
    private KycVerificationService kycVerificationService;

    @BeforeEach
    void setUp() {
        clientRepository.deleteAll();
    }

    @Test
    void onboardReturnsClientWithoutSinHash() throws Exception {
        String body = """
                {
                  "firstName": "Avery",
                  "lastName": "Chen",
                  "sinHash": "%s",
                  "dateOfBirth": "1990-04-12",
                  "email": "avery.chen@example.com",
                  "phone": "416-555-0100",
                  "province": "ON"
                }
                """.formatted("a".repeat(64));

        mockMvc.perform(post("/api/clients/onboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.firstName").value("Avery"))
                .andExpect(jsonPath("$.kycStatus").value("PENDING"))
                .andExpect(jsonPath("$.sinHash").doesNotExist());

        verify(kycVerificationService).verifyAsync(anyLong());
    }

    @Test
    void onboardWithMissingSinHashReturnsBadRequest() throws Exception {
        String body = """
                {
                  "firstName": "Avery",
                  "lastName": "Chen",
                  "dateOfBirth": "1990-04-12"
                }
                """;

        mockMvc.perform(post("/api/clients/onboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
