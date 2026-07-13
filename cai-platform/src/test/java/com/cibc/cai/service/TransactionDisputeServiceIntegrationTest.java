package com.cibc.cai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cibc.cai.entity.Client;
import com.cibc.cai.entity.TransactionDispute;
import com.cibc.cai.repository.ClientRepository;
import com.cibc.cai.repository.TransactionDisputeRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testcontainers (real PostgreSQL + Flyway) integration tests for
 * {@link TransactionDisputeService}, persisting disputes and escalation cases
 * through the JPA repositories.
 *
 * <p>Faithfully ports the legacy Apex {@code TransactionDisputeServiceTest}
 * scenarios ({@code escalatesLargeDispute}, {@code standardSlaForSmallDispute},
 * {@code cardPresentFraudEscalatesRegardlessOfAmount}, {@code rejectsNonPositiveAmount})
 * and covers README rule 3 (dispute SLA) end-to-end.
 *
 * <p>The legacy SLA date-granularity bug is asserted as <strong>preserved</strong>:
 * a "48-hour" urgent SLA becomes {@code today + (48 / 24) = today + 2 calendar days}
 * on a {@code Date} field (see MIGRATION_NOTES.md).
 */
@SpringBootTest
@Testcontainers
class TransactionDisputeServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TransactionDisputeService disputeService;

    @Autowired
    private TransactionDisputeRepository disputeRepository;

    @Autowired
    private ClientRepository clientRepository;

    private Long makeClient() {
        Client client = new Client();
        client.setFirstName("Dispute");
        client.setLastName("Tester");
        client.setSinHash("sin-dispute-" + System.nanoTime());
        client.setDateOfBirth(LocalDate.of(1990, 1, 1));
        return clientRepository.save(client).getId();
    }

    @Test
    void escalatesLargeDispute() {
        Long clientId = makeClient();

        TransactionDispute dispute = disputeService.fileDispute(
                clientId, "4242", BigDecimal.valueOf(750), "GOODS_13.1", false);

        TransactionDispute reloaded = disputeRepository.findById(dispute.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo("Escalated");
        assertThat(reloaded.getEscalationCase()).isNotNull();
        assertThat(reloaded.getEscalationCase().getId()).isNotNull();
        // Preserved legacy SLA bug: 48h -> +2 calendar days on a Date field.
        assertThat(reloaded.getSlaDueDate()).isEqualTo(LocalDate.now().plusDays(2));
    }

    @Test
    void standardSlaForSmallDispute() {
        Long clientId = makeClient();

        TransactionDispute dispute = disputeService.fileDispute(
                clientId, "4242", BigDecimal.valueOf(120), "GOODS_13.1", false);

        TransactionDispute reloaded = disputeRepository.findById(dispute.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo("Open");
        assertThat(reloaded.getEscalationCase()).isNull();
        // 10 business days out (mirrors Apex assertion: > today + 9 days).
        assertThat(reloaded.getSlaDueDate()).isAfter(LocalDate.now().plusDays(9));
        // Weekends are skipped, so the SLA date never lands on a weekend.
        assertThat(reloaded.getSlaDueDate().getDayOfWeek())
                .isNotIn(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    }

    @Test
    void cardPresentFraudEscalatesRegardlessOfAmount() {
        // Escalation predicate (package-private) is asserted directly.
        assertThat(disputeService.requiresUrgentEscalation(
                BigDecimal.valueOf(100), "FRAUD_10.4", true)).isTrue();
        assertThat(disputeService.requiresUrgentEscalation(
                BigDecimal.valueOf(100), "FRAUD_10.4", false)).isFalse();

        // ...and end-to-end through the persisted service outcomes.
        TransactionDispute escalated = disputeService.fileDispute(
                makeClient(), "4242", BigDecimal.valueOf(100), "FRAUD_10.4", true);
        assertThat(disputeRepository.findById(escalated.getId()).orElseThrow().getStatus())
                .isEqualTo("Escalated");

        TransactionDispute open = disputeService.fileDispute(
                makeClient(), "4242", BigDecimal.valueOf(100), "FRAUD_10.4", false);
        assertThat(disputeRepository.findById(open.getId()).orElseThrow().getStatus())
                .isEqualTo("Open");
    }

    @Test
    void rejectsNonPositiveAmount() {
        Long clientId = makeClient();

        assertThatThrownBy(() -> disputeService.fileDispute(
                clientId, "4242", BigDecimal.ZERO, "X", false))
                .isInstanceOf(DisputeException.class)
                .hasMessageContaining("positive");
    }
}
