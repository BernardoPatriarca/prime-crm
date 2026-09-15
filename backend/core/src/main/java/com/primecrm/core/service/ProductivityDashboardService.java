package com.primecrm.core.service;

import com.primecrm.core.dto.dashboard.DashboardTaskSummary;
import com.primecrm.core.dto.dashboard.ProductivityAgendaSummary;
import com.primecrm.core.dto.dashboard.ProductivityDashboardResponse;
import com.primecrm.core.dto.dashboard.ProductivityRankingRow;
import com.primecrm.infra.entity.agenda.CalendarEventStatus;
import com.primecrm.infra.entity.task.TaskStatus;
import com.primecrm.infra.repository.CalendarEventRepository;
import com.primecrm.infra.repository.TaskRepository;
import com.primecrm.infra.repository.projection.LabeledCountAggregate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductivityDashboardService {

    public static final int DEFAULT_PERIOD_DAYS = 30;
    public static final int RANKING_SIZE = 5;

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final List<TaskStatus> OPEN_TASK_STATUSES = List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS);
    private static final int RATE_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final TaskRepository taskRepository;
    private final CalendarEventRepository calendarEventRepository;

    @Transactional(readOnly = true)
    public ProductivityDashboardResponse load(LocalDate from, LocalDate to) {
        LocalDate periodEnd = to == null ? LocalDate.now(BUSINESS_ZONE) : to;
        LocalDate periodStart = from == null ? periodEnd.minusDays(DEFAULT_PERIOD_DAYS) : from;
        Instant start = startOfDay(periodStart);
        Instant end = startOfDay(periodEnd.plusDays(1));
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(7);

        return new ProductivityDashboardResponse(
                periodStart,
                periodEnd,
                Instant.now(),
                taskSummary(now, today),
                taskRanking(start, end),
                agendaSummary(now, today, weekStart, weekEnd));
    }

    private DashboardTaskSummary taskSummary(Instant now, LocalDate today) {
        return new DashboardTaskSummary(
                taskRepository.countByStatusAndDeletedAtIsNull(TaskStatus.PENDING),
                taskRepository.countByStatusAndDeletedAtIsNull(TaskStatus.IN_PROGRESS),
                taskRepository.countByStatusInAndDueAtLessThanAndDeletedAtIsNull(OPEN_TASK_STATUSES, now),
                taskRepository.countByStatusInAndDueAtGreaterThanEqualAndDueAtLessThanAndDeletedAtIsNull(
                        OPEN_TASK_STATUSES, startOfDay(today), startOfDay(today.plusDays(1))),
                taskRepository.countByStatusAndCompletedAtGreaterThanEqualAndDeletedAtIsNull(
                        TaskStatus.DONE, startOfDay(today.minusDays(6))));
    }

    private List<ProductivityRankingRow> taskRanking(Instant from, Instant to) {
        List<LabeledCountAggregate> rows = taskRepository.rankAssigneesByCompleted(TaskStatus.DONE, from, to,
                PageRequest.of(0, RANKING_SIZE));
        long total = rows.stream().mapToLong(LabeledCountAggregate::getItemCount).sum();
        return rows.stream()
                .map(row -> new ProductivityRankingRow(row.getLabel(), row.getItemCount(),
                        rate(row.getItemCount(), total)))
                .toList();
    }

    private ProductivityAgendaSummary agendaSummary(Instant now, LocalDate today, LocalDate weekStart,
            LocalDate weekEnd) {
        long scheduledToday = calendarEventRepository
                .countByStatusAndStartAtGreaterThanEqualAndStartAtLessThanAndDeletedAtIsNull(
                        CalendarEventStatus.SCHEDULED, startOfDay(today), startOfDay(today.plusDays(1)));
        long scheduledThisWeek = calendarEventRepository
                .countByStatusAndStartAtGreaterThanEqualAndStartAtLessThanAndDeletedAtIsNull(
                        CalendarEventStatus.SCHEDULED, startOfDay(weekStart), startOfDay(weekEnd));
        long overdue = calendarEventRepository.countOverdue(CalendarEventStatus.SCHEDULED, now);
        return new ProductivityAgendaSummary(scheduledToday, scheduledThisWeek, overdue);
    }

    private BigDecimal rate(long value, long total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value).multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(total), RATE_SCALE, RoundingMode.HALF_UP);
    }

    private Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(BUSINESS_ZONE).toInstant();
    }
}
