package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.primecrm.core.dto.dashboard.CommercialDashboardResponse;
import com.primecrm.infra.entity.contract.ContractStatus;
import com.primecrm.infra.entity.order.OrderStatus;
import com.primecrm.infra.entity.proposal.ProposalStatus;
import com.primecrm.infra.repository.ContractRepository;
import com.primecrm.infra.repository.OrderRepository;
import com.primecrm.infra.repository.ProposalRepository;
import com.primecrm.infra.repository.projection.AmountAggregate;
import java.math.BigDecimal;
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
class CommercialDashboardServiceTest {

    @Mock
    private ProposalRepository proposalRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ContractRepository contractRepository;

    private CommercialDashboardService commercialDashboardService;

    @BeforeEach
    void setUp() {
        commercialDashboardService = new CommercialDashboardService(proposalRepository, orderRepository,
                contractRepository);

        when(proposalRepository.summarizeIssuedBetween(any(), any())).thenReturn(aggregate(0, "0"));
        when(proposalRepository.summarizeByStatusIssuedBetween(any(), any(), any())).thenReturn(aggregate(0, "0"));
        when(proposalRepository.summarizeIssuedByMonth(any())).thenReturn(List.of());
        when(orderRepository.summarizeOrderedBetween(any(), any())).thenReturn(aggregate(0, "0"));
        when(orderRepository.summarizeByStatusOrderedBetween(any(), any(), any())).thenReturn(aggregate(0, "0"));
        when(orderRepository.summarizeOrderedByMonth(any())).thenReturn(List.of());
        when(contractRepository.summarizeByStatus(any())).thenReturn(aggregate(0, "0"));
        when(contractRepository.summarizeExpiringBetween(any(), any(), any())).thenReturn(aggregate(0, "0"));
    }

    @Test
    void load_buildsTwelveMonthsOfSeriesFillingTheGapsWithZero() {
        CommercialDashboardResponse response = load();

        assertThat(response.monthly()).hasSize(CommercialDashboardService.MONTHLY_SERIES_SIZE);
        assertThat(response.monthly()).allSatisfy(point -> {
            assertThat(point.proposalsAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(point.ordersAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        });
    }

    @Test
    void load_computesProposalConversionRateFromAcceptedOverTotal() {
        when(proposalRepository.summarizeIssuedBetween(any(), any())).thenReturn(aggregate(4, "40000.00"));
        when(proposalRepository.summarizeByStatusIssuedBetween(eq(ProposalStatus.ACCEPTED), any(), any()))
                .thenReturn(aggregate(1, "10000.00"));

        var proposals = load().proposals();

        assertThat(proposals.totalCount()).isEqualTo(4);
        assertThat(proposals.closedCount()).isEqualTo(1);
        assertThat(proposals.conversionRate()).isEqualByComparingTo("25.00");
    }

    @Test
    void load_computesOrderConversionRateFromDeliveredOverTotal() {
        when(orderRepository.summarizeOrderedBetween(any(), any())).thenReturn(aggregate(5, "50000.00"));
        when(orderRepository.summarizeByStatusOrderedBetween(eq(OrderStatus.DELIVERED), any(), any()))
                .thenReturn(aggregate(2, "20000.00"));

        var orders = load().orders();

        assertThat(orders.totalCount()).isEqualTo(5);
        assertThat(orders.closedCount()).isEqualTo(2);
        assertThat(orders.conversionRate()).isEqualByComparingTo("40.00");
    }

    @Test
    void load_withoutAnyProposalsOrOrders_returnsZeroRateInsteadOfFailing() {
        var proposals = load().proposals();
        var orders = load().orders();

        assertThat(proposals.conversionRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(orders.conversionRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void load_summarizesActiveAndExpiringContracts() {
        when(contractRepository.summarizeByStatus(eq(ContractStatus.ACTIVE))).thenReturn(aggregate(10, "5000.00"));
        when(contractRepository.summarizeExpiringBetween(eq(ContractStatus.ACTIVE), any(), any()))
                .thenReturn(aggregate(2, "800.00"));

        var contracts = load().contracts();

        assertThat(contracts.activeCount()).isEqualTo(10);
        assertThat(contracts.activeRecurringAmount()).isEqualByComparingTo("5000.00");
        assertThat(contracts.expiringCount()).isEqualTo(2);
        assertThat(contracts.expiringAmount()).isEqualByComparingTo("800.00");
    }

    private CommercialDashboardResponse load() {
        return commercialDashboardService.load(null, null);
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
}
