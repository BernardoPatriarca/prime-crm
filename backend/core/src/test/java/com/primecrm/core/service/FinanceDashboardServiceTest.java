package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.primecrm.core.dto.dashboard.FinanceDashboardResponse;
import com.primecrm.infra.entity.finance.PayableStatus;
import com.primecrm.infra.entity.finance.ReceivableStatus;
import com.primecrm.infra.repository.PayableRepository;
import com.primecrm.infra.repository.ReceivableRepository;
import com.primecrm.infra.repository.projection.AmountAggregate;
import com.primecrm.infra.repository.projection.LabeledAmountAggregate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FinanceDashboardServiceTest {

    @Mock
    private ReceivableRepository receivableRepository;
    @Mock
    private PayableRepository payableRepository;

    private FinanceDashboardService financeDashboardService;

    @BeforeEach
    void setUp() {
        financeDashboardService = new FinanceDashboardService(receivableRepository, payableRepository);

        when(receivableRepository.summarizeOpen(any())).thenReturn(aggregate(0, "0"));
        when(receivableRepository.summarizeOverdue(any(), any())).thenReturn(aggregate(0, "0"));
        when(receivableRepository.summarizePaidBetween(any(), any())).thenReturn(aggregate(0, "0"));
        when(receivableRepository.summarizePaidByMonth(any())).thenReturn(List.of());
        when(payableRepository.summarizeOpen(any())).thenReturn(aggregate(0, "0"));
        when(payableRepository.summarizeOverdue(any(), any())).thenReturn(aggregate(0, "0"));
        when(payableRepository.summarizePaidBetween(any(), any())).thenReturn(aggregate(0, "0"));
        when(payableRepository.summarizePaidByMonth(any())).thenReturn(List.of());
    }

    @Test
    void load_buildsTwelveMonthsOfSeriesFillingTheGapsWithZero() {
        FinanceDashboardResponse response = load();

        assertThat(response.monthly()).hasSize(FinanceDashboardService.MONTHLY_SERIES_SIZE);
        assertThat(response.monthly()).allSatisfy(point -> {
            assertThat(point.receivedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(point.paidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(point.netAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        });
    }

    @Test
    void load_computesNetAmountAsReceivedMinusPaidPerMonth() {
        when(receivableRepository.summarizePaidByMonth(any())).thenReturn(List.of(labeled("2026-09", 2, "1000.00")));
        when(payableRepository.summarizePaidByMonth(any())).thenReturn(List.of(labeled("2026-09", 1, "300.00")));

        var point = load().monthly().stream().filter(p -> p.month().equals("2026-09")).findFirst().orElseThrow();

        assertThat(point.receivedAmount()).isEqualByComparingTo("1000.00");
        assertThat(point.paidAmount()).isEqualByComparingTo("300.00");
        assertThat(point.netAmount()).isEqualByComparingTo("700.00");
    }

    @Test
    void load_usesPendingStatusToSummarizeOpenAndOverdueReceivables() {
        when(receivableRepository.summarizeOpen(eq(ReceivableStatus.PENDING))).thenReturn(aggregate(5, "5000.00"));
        when(receivableRepository.summarizeOverdue(eq(ReceivableStatus.PENDING), any())).thenReturn(aggregate(2, "800.00"));

        var receivables = load().receivables();

        assertThat(receivables.openCount()).isEqualTo(5);
        assertThat(receivables.openAmount()).isEqualByComparingTo("5000.00");
        assertThat(receivables.overdueCount()).isEqualTo(2);
        assertThat(receivables.overdueAmount()).isEqualByComparingTo("800.00");
    }

    @Test
    void load_usesPendingStatusToSummarizeOpenAndOverduePayables() {
        when(payableRepository.summarizeOpen(eq(PayableStatus.PENDING))).thenReturn(aggregate(3, "1200.00"));
        when(payableRepository.summarizeOverdue(eq(PayableStatus.PENDING), any())).thenReturn(aggregate(1, "400.00"));

        var payables = load().payables();

        assertThat(payables.openCount()).isEqualTo(3);
        assertThat(payables.openAmount()).isEqualByComparingTo("1200.00");
        assertThat(payables.overdueCount()).isEqualTo(1);
        assertThat(payables.overdueAmount()).isEqualByComparingTo("400.00");
    }

    @Test
    void load_withoutPreviousPeriodMovement_leavesTheTrendUndefined() {
        assertThat(load().receivables().movementTrend()).isNull();
        assertThat(load().payables().movementTrend()).isNull();
    }

    @Test
    void load_comparesTheReceivedAmountWithTheImmediatelyPreviousPeriod() {
        Instant start = LocalDate.of(2026, 2, 1).atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant();
        when(receivableRepository.summarizePaidBetween(any(), any())).thenReturn(aggregate(1, "150.00"));
        when(receivableRepository.summarizePaidBetween(eq(start.minus(java.time.Duration.ofDays(28))), eq(start)))
                .thenReturn(aggregate(1, "100.00"));

        FinanceDashboardResponse response = financeDashboardService.load(LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 28));

        assertThat(response.receivables().movementTrend()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    private FinanceDashboardResponse load() {
        return financeDashboardService.load(null, null);
    }

    private AmountAggregate aggregate(long count, String amount) {
        return new AmountAggregate() {
            @Override
            public long getItemCount() {
                return count;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return new BigDecimal(amount);
            }
        };
    }

    private LabeledAmountAggregate labeled(String label, long count, String amount) {
        return new LabeledAmountAggregate() {
            @Override
            public String getLabel() {
                return label;
            }

            @Override
            public long getItemCount() {
                return count;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return new BigDecimal(amount);
            }
        };
    }
}
