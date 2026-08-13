package com.primecrm.core.service;

import com.primecrm.core.dto.dashboard.DashboardFunnel;
import com.primecrm.core.dto.dashboard.DashboardFunnelStage;
import com.primecrm.core.dto.dashboard.DashboardMetrics;
import com.primecrm.core.dto.dashboard.DashboardMonthlyPoint;
import com.primecrm.core.dto.dashboard.DashboardRankingRow;
import com.primecrm.core.dto.dashboard.DashboardResponse;
import com.primecrm.core.dto.dashboard.DashboardTaskSummary;
import com.primecrm.infra.entity.commercial.OpportunityOutcome;
import com.primecrm.infra.entity.config.Pipeline;
import com.primecrm.infra.entity.task.TaskStatus;
import com.primecrm.infra.repository.CustomerRepository;
import com.primecrm.infra.repository.LeadRepository;
import com.primecrm.infra.repository.OpportunityRepository;
import com.primecrm.infra.repository.PipelineRepository;
import com.primecrm.infra.repository.TaskRepository;
import com.primecrm.infra.repository.projection.AmountAggregate;
import com.primecrm.infra.repository.projection.LabeledAmountAggregate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    public static final int DEFAULT_PERIOD_DAYS = 30;
    public static final int MONTHLY_SERIES_SIZE = 12;
    public static final int RANKING_SIZE = 5;

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final List<TaskStatus> OPEN_TASK_STATUSES = List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS);
    private static final int RATE_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final OpportunityRepository opportunityRepository;
    private final LeadRepository leadRepository;
    private final CustomerRepository customerRepository;
    private final TaskRepository taskRepository;
    private final PipelineRepository pipelineRepository;

    @Transactional(readOnly = true)
    public DashboardResponse load(LocalDate from, LocalDate to, UUID pipelineId) {
        LocalDate periodEnd = to == null ? LocalDate.now(BUSINESS_ZONE) : to;
        LocalDate periodStart = from == null ? periodEnd.minusDays(DEFAULT_PERIOD_DAYS) : from;

        Instant start = startOfDay(periodStart);
        Instant end = startOfDay(periodEnd.plusDays(1));
        long periodDays = Math.max(1, ChronoUnit.DAYS.between(periodStart, periodEnd.plusDays(1)));
        Instant previousStart = start.minus(periodDays, ChronoUnit.DAYS);

        return new DashboardResponse(
                periodStart,
                periodEnd,
                Instant.now(),
                metrics(start, end, previousStart),
                funnel(pipelineId),
                monthlySeries(),
                ranking(start, end),
                tasks());
    }

    private DashboardMetrics metrics(Instant start, Instant end, Instant previousStart) {
        long newLeads = leadRepository
                .countByDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end);
        long previousLeads = leadRepository
                .countByDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(previousStart, start);
        long convertedLeads = leadRepository
                .countByDeletedAtIsNullAndConvertedAtGreaterThanEqualAndConvertedAtLessThan(start, end);

        AmountAggregate open = opportunityRepository.summarizeByOutcome(OpportunityOutcome.OPEN);
        AmountAggregate won = opportunityRepository.summarizeClosedBetween(OpportunityOutcome.WON, start, end);
        AmountAggregate previousWon = opportunityRepository
                .summarizeClosedBetween(OpportunityOutcome.WON, previousStart, start);
        AmountAggregate lost = opportunityRepository.summarizeClosedBetween(OpportunityOutcome.LOST, start, end);

        long newCustomers = customerRepository
                .countByDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end);
        long previousCustomers = customerRepository
                .countByDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(previousStart, start);

        long closed = won.getItemCount() + lost.getItemCount();

        return new DashboardMetrics(
                newLeads,
                trend(newLeads, previousLeads),
                convertedLeads,
                rate(convertedLeads, newLeads),
                open.getItemCount(),
                amountOf(open),
                won.getItemCount(),
                amountOf(won),
                trend(amountOf(won), amountOf(previousWon)),
                lost.getItemCount(),
                rate(won.getItemCount(), closed),
                average(amountOf(won), won.getItemCount()),
                customerRepository.countByActiveIsTrueAndDeletedAtIsNull(),
                newCustomers,
                trend(newCustomers, previousCustomers));
    }

    private DashboardFunnel funnel(UUID pipelineId) {
        Pipeline pipeline = resolvePipeline(pipelineId);
        if (pipeline == null) {
            return new DashboardFunnel(null, null, List.of());
        }
        List<DashboardFunnelStage> stages = opportunityRepository
                .summarizeFunnel(pipeline.getId(), OpportunityOutcome.OPEN).stream()
                .map(stage -> new DashboardFunnelStage(stage.getLabel(), stage.getColor(), stage.getDisplayOrder(),
                        stage.getItemCount(), amountOf(stage)))
                .toList();
        return new DashboardFunnel(pipeline.getId(), pipeline.getName(), stages);
    }

    private Pipeline resolvePipeline(UUID pipelineId) {
        if (pipelineId != null) {
            return pipelineRepository.findById(pipelineId).filter(pipeline -> !pipeline.isDeleted()).orElse(null);
        }
        return pipelineRepository.findByActiveIsTrueAndDeletedAtIsNullOrderByNameAsc().stream()
                .findFirst()
                .orElse(null);
    }

    private List<DashboardMonthlyPoint> monthlySeries() {
        YearMonth firstMonth = YearMonth.now(BUSINESS_ZONE).minusMonths(MONTHLY_SERIES_SIZE - 1L);
        Instant seriesStart = startOfDay(firstMonth.atDay(1));

        Map<String, LabeledAmountAggregate> won = byLabel(
                opportunityRepository.summarizeClosedByMonth(OpportunityOutcome.WON, seriesStart));
        Map<String, LabeledAmountAggregate> opened = byLabel(
                opportunityRepository.summarizeOpenedByMonth(seriesStart));

        return IntStream.range(0, MONTHLY_SERIES_SIZE)
                .mapToObj(index -> firstMonth.plusMonths(index).format(MONTH_FORMAT))
                .map(month -> new DashboardMonthlyPoint(
                        month,
                        countOf(opened.get(month)),
                        amountOf(opened.get(month)),
                        countOf(won.get(month)),
                        amountOf(won.get(month))))
                .toList();
    }

    private List<DashboardRankingRow> ranking(Instant start, Instant end) {
        List<LabeledAmountAggregate> rows = opportunityRepository.rankOwnersByClosedAmount(
                OpportunityOutcome.WON, start, end, PageRequest.of(0, RANKING_SIZE));
        BigDecimal total = rows.stream().map(this::amountOf).reduce(BigDecimal.ZERO, BigDecimal::add);
        return rows.stream()
                .map(row -> new DashboardRankingRow(row.getLabel(), row.getItemCount(), amountOf(row),
                        rate(amountOf(row), total)))
                .toList();
    }

    private DashboardTaskSummary tasks() {
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        return new DashboardTaskSummary(
                taskRepository.countByStatusAndDeletedAtIsNull(TaskStatus.PENDING),
                taskRepository.countByStatusAndDeletedAtIsNull(TaskStatus.IN_PROGRESS),
                taskRepository.countByStatusInAndDueAtLessThanAndDeletedAtIsNull(OPEN_TASK_STATUSES, now),
                taskRepository.countByStatusInAndDueAtGreaterThanEqualAndDueAtLessThanAndDeletedAtIsNull(
                        OPEN_TASK_STATUSES, startOfDay(today), startOfDay(today.plusDays(1))),
                taskRepository.countByStatusAndCompletedAtGreaterThanEqualAndDeletedAtIsNull(
                        TaskStatus.DONE, startOfDay(today.minusDays(6))));
    }

    private Map<String, LabeledAmountAggregate> byLabel(List<LabeledAmountAggregate> rows) {
        return rows.stream().collect(Collectors.toMap(LabeledAmountAggregate::getLabel, Function.identity(),
                (first, second) -> first));
    }

    private Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(BUSINESS_ZONE).toInstant();
    }

    private long countOf(AmountAggregate aggregate) {
        return aggregate == null ? 0 : aggregate.getItemCount();
    }

    private BigDecimal amountOf(AmountAggregate aggregate) {
        if (aggregate == null || aggregate.getTotalAmount() == null) {
            return BigDecimal.ZERO;
        }
        return aggregate.getTotalAmount();
    }

    private BigDecimal average(BigDecimal total, long count) {
        if (count == 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(BigDecimal.valueOf(count), RATE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(long value, long total) {
        return rate(BigDecimal.valueOf(value), BigDecimal.valueOf(total));
    }

    private BigDecimal rate(BigDecimal value, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return value.multiply(ONE_HUNDRED).divide(total, RATE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal trend(long current, long previous) {
        return trend(BigDecimal.valueOf(current), BigDecimal.valueOf(previous));
    }

    private BigDecimal trend(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return null;
        }
        return current.subtract(previous).multiply(ONE_HUNDRED).divide(previous, RATE_SCALE, RoundingMode.HALF_UP);
    }
}
