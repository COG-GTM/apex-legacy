package com.cai.platform.dispute;

import com.cai.platform.domain.Client;
import com.cai.platform.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DisputeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRepository clientRepository;

    private Long makeClient() {
        Client client = new Client();
        client.setFirstName("Dispute");
        client.setLastName("Tester");
        return clientRepository.save(client).getId();
    }

    @Test
    void filesDisputeAndReturnsCreated() throws Exception {
        Long clientId = makeClient();

        mockMvc.perform(post("/api/disputes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId": %d, "cardLast4": "4242", "amount": 120,
                                 "reasonCode": "GOODS_13.1", "cardPresent": false}
                                """.formatted(clientId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.escalationCaseId").doesNotExist());
    }

    @Test
    void escalatedDisputeReturnsEscalationCaseId() throws Exception {
        Long clientId = makeClient();

        mockMvc.perform(post("/api/disputes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId": %d, "cardLast4": "4242", "amount": 750,
                                 "reasonCode": "GOODS_13.1", "cardPresent": false}
                                """.formatted(clientId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ESCALATED"))
                .andExpect(jsonPath("$.escalationCaseId").isNumber());
    }

    @Test
    void rejectsNonPositiveAmountWithBadRequest() throws Exception {
        Long clientId = makeClient();

        mockMvc.perform(post("/api/disputes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId": %d, "cardLast4": "4242", "amount": 0,
                                 "reasonCode": "X", "cardPresent": false}
                                """.formatted(clientId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Dispute amount must be positive"));
    }
}
