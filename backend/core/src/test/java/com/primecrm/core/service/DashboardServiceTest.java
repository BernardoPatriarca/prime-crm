package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.primecrm.core.dto.dashboard.DashboardResponse;
import com.primecrm.infra.entity.commercial.OpportunityOutcome;
import com.primecrm.infra.entity.config.Pipeline;
import com.primecrm.infra.repository.CustomerRepository;
import com.primecrm.infra.repository.LeadRepository;
import com.primecrm.infra.repository.OpportunityRepository;
import com.primecrm.infra.repository.PipelineRepository;
import com.primecrm.infra.repository.TaskRepository;
import com.primecrm.infra.repository.projection.AmountAggregate;
import com.primecrm.infra.repository.projection.LabeledAmountAggregate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceTest {

    @Mock
    private OpportunityRepository opportunityRepository;
    @Mock
    private LeadRepository leadRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private PipelineRepository pipelineRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(opportunityRepository, leadRepository, customerRepository,
                taskRepository, pipelineRepository);

        when(opportunityRepository.summarizeByOutcome(any())).thenReturn(aggregate(0, "0"));
        when(opportunityRepository.summarizeClosedBetween(any(), any(), any())).thenReturn(aggregate(0, "0"));
        when(opportunityRepository.summarizeClosedByMonth(any(), any())).thenReturn(List.of());
        when(opportunityRepository.summarizeOpenedByMonth(any())).thenReturn(List.of());
        when(opportunityRepository.summarizeFunnel(any(), any())).thenReturn(List.of());
        when(opportunityRepository.rankOwnersByClosedAmount(any(), any(), any(), any())).thenReturn(List.of());
        when(pipelineRepository.findByActiveIsTrueAndDeletedAtIsNullOrderByNameAsc()).thenReturn(List.of());
    }

    @Test
    void load_buildsTwelveMonthsOfSeriesFillingTheGapsWithZero() {
        DashboardResponse response = load();

        assertThat(response.monthly()).hasSize(DashboardService.MONTHLY_SERIES_SIZE);
        assertThat(response.monthly()).allSatisfy(point -> {
            assertThat(point.wonCount()).isZero();
            assertThat(point.wonAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        });
    }

    @Test
    void load_withoutPreviousPeriodMovement_leavesTheTrendUndefined() {
        when(leadRepository.countByDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any()))
                .thenReturn(0L);

        assertThat(load().metrics().newLeadsTrend()).isNull();
    }

    @Test
    void load_comparesThePeriodWithTheImmediatelyPreviousOne() {
        Instant start = LocalDate.of(2026, 2, 1).atStartOfDay(java.time.ZoneId.of("America/Sao_Paulo")).toInstant();
        when(leadRepository.countByDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any()))
                .thenReturn(30L);
        when(leadRepository.countByDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(start.minus(java.time.Duration.ofDays(28))), eq(start))).thenReturn(20L);

        DashboardResponse response = dashboardService.load(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), null);

        assertThat(response.metrics().newLeadsTrend()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void load_computesWinRateOverClosedOpportunitiesOnly() {
        when(opportunityRepository.summarizeClosedBetween(eq(OpportunityOutcome.WON), any(), any()))
                .thenReturn(aggregate(3, "30000"));
        when(opportunityRepository.summarizeClosedBetween(eq(OpportunityOutcome.LOST), any(), any()))
                .thenReturn(aggregate(1, "5000"));

        var metrics = load().metrics();

        assertThat(metrics.winRate()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(metrics.averageTicket()).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    void load_withoutClosedOpportunities_returnsZeroRatesInsteadOfFailing() {
        var metrics = load().metrics();

        assertThat(metrics.winRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metrics.averageTicket()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metrics.leadConversionRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void load_rankingSharesAddUpToTheOwnersTotal() {
        when(opportunityRepository.rankOwnersByClosedAmount(any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(labeled("Ana", 4, "60000"), labeled("Bruno", 2, "40000")));

        var ranking = load().ranking();

        assertThat(ranking).extracting(row -> row.share().toPlainString())
                .containsExactly("60.00", "40.00");
    }

    @Test
    void load_withoutAnActivePipeline_returnsAnEmptyFunnelInsteadOfFailing() {
        var funnel = load().funnel();

        assertThat(funnel.pipelineId()).isNull();
        assertThat(funnel.stages()).isEmpty();
    }

    @Test
    void load_withAnExplicitPipeline_usesIt() {
        Pipeline pipeline = new Pipeline();
        pipeline.setId(UUID.randomUUID());
        pipeline.setName("Funil Corporativo");
        when(pipelineRepository.findById(pipeline.getId())).thenReturn(Optional.of(pipeline));

        var funnel = dashboardService.load(null, null, pipeline.getId()).funnel();

        assertThat(funnel.pipelineName()).isEqualTo("Funil Corporativo");
    }

    @Test
    void load_countsOverdueTasksOverOpenStatusesOnly() {
        when(taskRepository.countByStatusInAndDueAtLessThanAndDeletedAtIsNull(anyList(), any())).thenReturn(7L);

        assertThat(load().tasks().overdue()).isEqualTo(7L);
    }

    private DashboardResponse load() {
        return dashboardService.load(null, null, null);
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
