package com.primecrm.core.service;

import com.primecrm.core.dto.dashboard.FinanceDashboardMetrics;
import com.primecrm.core.dto.dashboard.FinanceDashboardResponse;
import com.primecrm.core.dto.dashboard.FinanceMonthlyPoint;
import com.primecrm.infra.entity.finance.PayableStatus;
import com.primecrm.infra.entity.finance.ReceivableStatus;
import com.primecrm.infra.repository.PayableRepository;
import com.primecrm.infra.repository.ReceivableRepository;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinanceDashboardService {

    public static final int DEFAULT_PERIOD_DAYS = 30;
    public static final int MONTHLY_SERIES_SIZE = 12;

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int RATE_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final ReceivableRepository receivableRepository;
    private final PayableRepository payableRepository;

    @Transactional(readOnly = true)
    public FinanceDashboardResponse load(LocalDate from, LocalDate to) {
        LocalDate periodEnd = to == null ? LocalDate.now(BUSINESS_ZONE) : to;
        LocalDate periodStart = from == null ? periodEnd.minusDays(DEFAULT_PERIOD_DAYS) : from;

        Instant start = startOfDay(periodStart);
        Instant end = startOfDay(periodEnd.plusDays(1));
        long periodDays = Math.max(1, ChronoUnit.DAYS.between(periodStart, periodEnd.plusDays(1)));
        Instant previousStart = start.minus(periodDays, ChronoUnit.DAYS);
        LocalDate today = LocalDate.now(BUSINESS_ZONE);

        return new FinanceDashboardResponse(
                periodStart,
                periodEnd,
                Instant.now(),
                receivablesMetrics(start, end, previousStart, today),
                payablesMetrics(start, end, previousStart, today),
                monthlySeries());
    }

    private FinanceDashboardMetrics receivablesMetrics(Instant start, Instant end, Instant previousStart,
            LocalDate today) {
        AmountAggregate open = receivableRepository.summarizeOpen(ReceivableStatus.PENDING);
        AmountAggregate overdue = receivableRepository.summarizeOverdue(ReceivableStatus.PENDING, today);
        AmountAggregate received = receivableRepository.summarizePaidBetween(start, end);
        AmountAggregate previousReceived = receivableRepository.summarizePaidBetween(previousStart, start);

        return new FinanceDashboardMetrics(
                open.getItemCount(), amountOf(open),
                overdue.getItemCount(), amountOf(overdue),
                received.getItemCount(), amountOf(received),
                trend(amountOf(received), amountOf(previousReceived)));
    }

    private FinanceDashboardMetrics payablesMetrics(Instant start, Instant end, Instant previousStart,
            LocalDate today) {
        AmountAggregate open = payableRepository.summarizeOpen(PayableStatus.PENDING);
        AmountAggregate overdue = payableRepository.summarizeOverdue(PayableStatus.PENDING, today);
        AmountAggregate paid = payableRepository.summarizePaidBetween(start, end);
        AmountAggregate previousPaid = payableRepository.summarizePaidBetween(previousStart, start);

        return new FinanceDashboardMetrics(
                open.getItemCount(), amountOf(open),
                overdue.getItemCount(), amountOf(overdue),
                paid.getItemCount(), amountOf(paid),
                trend(amountOf(paid), amountOf(previousPaid)));
    }

    private List<FinanceMonthlyPoint> monthlySeries() {
        YearMonth firstMonth = YearMonth.now(BUSINESS_ZONE).minusMonths(MONTHLY_SERIES_SIZE - 1L);
        Instant seriesStart = startOfDay(firstMonth.atDay(1));

        Map<String, LabeledAmountAggregate> received = byLabel(
                receivableRepository.summarizePaidByMonth(seriesStart));
        Map<String, LabeledAmountAggregate> paid = byLabel(
                payableRepository.summarizePaidByMonth(seriesStart));

        return IntStream.range(0, MONTHLY_SERIES_SIZE)
                .mapToObj(index -> firstMonth.plusMonths(index).format(MONTH_FORMAT))
                .map(month -> {
                    BigDecimal receivedAmount = amountOf(received.get(month));
                    BigDecimal paidAmount = amountOf(paid.get(month));
                    return new FinanceMonthlyPoint(month, receivedAmount, paidAmount,
                            receivedAmount.subtract(paidAmount));
                })
                .toList();
    }

    private Map<String, LabeledAmountAggregate> byLabel(List<LabeledAmountAggregate> rows) {
        return rows.stream().collect(Collectors.toMap(LabeledAmountAggregate::getLabel, Function.identity(),
                (first, second) -> first));
    }

    private Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(BUSINESS_ZONE).toInstant();
    }

    private BigDecimal amountOf(AmountAggregate aggregate) {
        if (aggregate == null || aggregate.getTotalAmount() == null) {
            return BigDecimal.ZERO;
        }
        return aggregate.getTotalAmount();
    }

    private BigDecimal trend(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return null;
        }
        return current.subtract(previous).multiply(ONE_HUNDRED).divide(previous, RATE_SCALE, RoundingMode.HALF_UP);
    }
}
