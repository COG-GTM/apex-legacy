package com.cibc.cai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.cibc.cai.entity.Client;
import com.cibc.cai.entity.EscalationCase;
import com.cibc.cai.entity.TransactionDispute;
import com.cibc.cai.repository.ClientRepository;
import com.cibc.cai.repository.EscalationCaseRepository;
import com.cibc.cai.repository.TransactionDisputeRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionDisputeServiceTest {

    @Mock
    private TransactionDisputeRepository disputeRepository;

    @Mock
    private EscalationCaseRepository escalationCaseRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private TransactionDisputeService service;

    @BeforeEach
    void setUp() {
        lenient().when(clientRepository.getReferenceById(1L)).thenReturn(new Client());
        lenient().when(disputeRepository.save(any(TransactionDispute.class))).thenAnswer(inv -> {
            TransactionDispute d = inv.getArgument(0);
            if (d.getId() == null) {
                d.setId(10L);
            }
            return d;
        });
        lenient().when(escalationCaseRepository.save(any(EscalationCase.class))).thenAnswer(inv -> {
            EscalationCase c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(20L);
            }
            return c;
        });
    }

    @Test
    void escalatesLargeDispute() {
        TransactionDispute dispute = service.fileDispute(
                1L, "4242", BigDecimal.valueOf(750), "GOODS_13.1", false);

        assertThat(dispute.getStatus()).isEqualTo("Escalated");
        assertThat(dispute.getEscalationCase()).isNotNull();
        // legacy SLA granularity bug preserved: 48h -> +2 days
        assertThat(dispute.getSlaDueDate()).isEqualTo(LocalDate.now().plusDays(2));
    }

    @Test
    void standardSlaForSmallDispute() {
        TransactionDispute dispute = service.fileDispute(
                1L, "4242", BigDecimal.valueOf(120), "GOODS_13.1", false);

        assertThat(dispute.getStatus()).isEqualTo("Open");
        assertThat(dispute.getEscalationCase()).isNull();
        assertThat(dispute.getSlaDueDate()).isAfter(LocalDate.now().plusDays(9));
    }

    @Test
    void cardPresentFraudEscalatesRegardlessOfAmount() {
        assertThat(service.requiresUrgentEscalation(
                BigDecimal.valueOf(100), "FRAUD_10.4", true)).isTrue();
        assertThat(service.requiresUrgentEscalation(
                BigDecimal.valueOf(100), "FRAUD_10.4", false)).isFalse();
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> service.fileDispute(1L, "4242", BigDecimal.ZERO, "X", false))
                .isInstanceOf(DisputeException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void rejectsNullAmount() {
        assertThatThrownBy(() -> service.fileDispute(1L, "4242", null, "X", false))
                .isInstanceOf(DisputeException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void addBusinessDaysSkipsWeekend() {
        LocalDate friday = LocalDate.of(2024, 1, 5);
        assertThat(friday.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);

        LocalDate oneBusinessDay = service.addBusinessDays(friday, 1);

        assertThat(oneBusinessDay).isEqualTo(LocalDate.of(2024, 1, 8));
        assertThat(oneBusinessDay.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    void addBusinessDaysSpansMultipleWeekends() {
        LocalDate monday = LocalDate.of(2024, 1, 1);
        assertThat(monday.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);

        LocalDate tenBusinessDays = service.addBusinessDays(monday, 10);

        assertThat(tenBusinessDays).isEqualTo(LocalDate.of(2024, 1, 15));
    }
}
