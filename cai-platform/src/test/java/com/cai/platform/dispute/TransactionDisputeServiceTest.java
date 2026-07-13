package com.cai.platform.dispute;

import com.cai.platform.domain.Client;
import com.cai.platform.domain.DisputeStatus;
import com.cai.platform.domain.TransactionDispute;
import com.cai.platform.repository.ClientRepository;
import com.cai.platform.repository.TransactionDisputeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TransactionDisputeServiceTest {

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
        return clientRepository.save(client).getId();
    }

    @Test
    void escalatesLargeDispute() {
        Long clientId = makeClient();

        TransactionDispute dispute = disputeService.fileDispute(
                clientId, "4242", new BigDecimal("750"), "GOODS_13.1", false);

        TransactionDispute reloaded = disputeRepository.findById(dispute.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DisputeStatus.ESCALATED);
        assertThat(reloaded.getEscalationCase()).isNotNull();
        assertThat(reloaded.getSlaDueDate()).isEqualTo(LocalDate.now().plusDays(2));
    }

    @Test
    void standardSlaForSmallDispute() {
        Long clientId = makeClient();

        TransactionDispute dispute = disputeService.fileDispute(
                clientId, "4242", new BigDecimal("120"), "GOODS_13.1", false);

        TransactionDispute reloaded = disputeRepository.findById(dispute.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DisputeStatus.OPEN);
        assertThat(reloaded.getSlaDueDate()).isAfter(LocalDate.now().plusDays(9));
        assertThat(reloaded.getEscalationCase()).isNull();
    }

    @Test
    void cardPresentFraudEscalatesRegardlessOfAmount() {
        assertThat(disputeService.requiresUrgentEscalation(
                new BigDecimal("100"), "FRAUD_10.4", true)).isTrue();
        assertThat(disputeService.requiresUrgentEscalation(
                new BigDecimal("100"), "FRAUD_10.4", false)).isFalse();
    }

    @Test
    void cardPresentFraud101AlsoEscalates() {
        assertThat(disputeService.requiresUrgentEscalation(
                new BigDecimal("100"), "FRAUD_10.1", true)).isTrue();
        assertThat(disputeService.requiresUrgentEscalation(
                new BigDecimal("100"), "OTHER", true)).isFalse();
    }

    @Test
    void cardPresentFraudDisputeEscalatesWithCase() {
        Long clientId = makeClient();

        TransactionDispute dispute = disputeService.fileDispute(
                clientId, "4242", new BigDecimal("100"), "FRAUD_10.4", true);

        assertThat(dispute.getStatus()).isEqualTo(DisputeStatus.ESCALATED);
        assertThat(dispute.getEscalationCase()).isNotNull();
        assertThat(dispute.getEscalationCase().getSubject())
                .isEqualTo("URGENT dispute " + dispute.getId() + " — $100");
        assertThat(dispute.getEscalationCase().getPriority()).isEqualTo("High");
        assertThat(dispute.getEscalationCase().getOrigin()).isEqualTo("Dispute Intake");
    }

    @Test
    void rejectsNonPositiveAmount() {
        Long clientId = makeClient();

        assertThatThrownBy(() -> disputeService.fileDispute(
                clientId, "4242", BigDecimal.ZERO, "X", false))
                .isInstanceOf(DisputeException.class)
                .hasMessageContaining("positive");

        assertThatThrownBy(() -> disputeService.fileDispute(
                clientId, "4242", null, "X", false))
                .isInstanceOf(DisputeException.class)
                .hasMessageContaining("positive");

        assertThatThrownBy(() -> disputeService.fileDispute(
                clientId, "4242", new BigDecimal("-5"), "X", false))
                .isInstanceOf(DisputeException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void addBusinessDaysSkipsWeekends() {
        // Friday 2026-07-10 + 1 business day = Monday 2026-07-13
        LocalDate friday = LocalDate.of(2026, 7, 10);
        assertThat(friday.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
        assertThat(disputeService.addBusinessDays(friday, 1))
                .isEqualTo(LocalDate.of(2026, 7, 13));

        // Friday + 10 business days = two full weeks later (Friday 2026-07-24)
        assertThat(disputeService.addBusinessDays(friday, 10))
                .isEqualTo(LocalDate.of(2026, 7, 24));

        // Saturday start: next business day is Monday
        LocalDate saturday = LocalDate.of(2026, 7, 11);
        assertThat(saturday.getDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);
        assertThat(disputeService.addBusinessDays(saturday, 1))
                .isEqualTo(LocalDate.of(2026, 7, 13));

        // Monday + 5 business days = next Monday
        LocalDate monday = LocalDate.of(2026, 7, 13);
        assertThat(disputeService.addBusinessDays(monday, 5))
                .isEqualTo(LocalDate.of(2026, 7, 20));

        // Zero business days returns the start date
        assertThat(disputeService.addBusinessDays(monday, 0)).isEqualTo(monday);
    }

    @Test
    void exactly500DoesNotEscalate() {
        assertThat(disputeService.requiresUrgentEscalation(
                new BigDecimal("500"), "GOODS_13.1", false)).isFalse();
        assertThat(disputeService.requiresUrgentEscalation(
                new BigDecimal("500.01"), "GOODS_13.1", false)).isTrue();
    }
}
