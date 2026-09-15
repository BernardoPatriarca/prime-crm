package com.primecrm.core.service;

import com.primecrm.core.dto.dashboard.CommercialDashboardMetrics;
import com.primecrm.core.dto.dashboard.CommercialDashboardResponse;
import com.primecrm.core.dto.dashboard.CommercialMonthlyPoint;
import com.primecrm.core.dto.dashboard.ContractDashboardMetrics;
import com.primecrm.infra.entity.contract.ContractStatus;
import com.primecrm.infra.entity.order.OrderStatus;
import com.primecrm.infra.entity.proposal.ProposalStatus;
import com.primecrm.infra.repository.ContractRepository;
import com.primecrm.infra.repository.OrderRepository;
import com.primecrm.infra.repository.ProposalRepository;
import com.primecrm.infra.repository.projection.AmountAggregate;
import com.primecrm.infra.repository.projection.LabeledAmountAggregate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
public class CommercialDashboardService {

    public static final int DEFAULT_PERIOD_DAYS = 30;
    public static final int MONTHLY_SERIES_SIZE = 12;
    public static final int EXPIRING_WINDOW_DAYS = 30;

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int RATE_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final ProposalRepository proposalRepository;
    private final OrderRepository orderRepository;
    private final ContractRepository contractRepository;

    @Transactional(readOnly = true)
    public CommercialDashboardResponse load(LocalDate from, LocalDate to) {
        LocalDate periodEnd = to == null ? LocalDate.now(BUSINESS_ZONE) : to;
        LocalDate periodStart = from == null ? periodEnd.minusDays(DEFAULT_PERIOD_DAYS) : from;
        LocalDate periodEndExclusive = periodEnd.plusDays(1);
        LocalDate today = LocalDate.now(BUSINESS_ZONE);

        return new CommercialDashboardResponse(
                periodStart,
                periodEnd,
                Instant.now(),
                proposalMetrics(periodStart, periodEndExclusive),
                orderMetrics(periodStart, periodEndExclusive),
                contractMetrics(today),
                monthlySeries());
    }

    private CommercialDashboardMetrics proposalMetrics(LocalDate from, LocalDate to) {
        AmountAggregate total = proposalRepository.summarizeIssuedBetween(from, to);
        AmountAggregate accepted = proposalRepository.summarizeByStatusIssuedBetween(ProposalStatus.ACCEPTED, from,
                to);
        return new CommercialDashboardMetrics(
                total.getItemCount(), amountOf(total),
                accepted.getItemCount(), amountOf(accepted),
                rate(accepted.getItemCount(), total.getItemCount()));
    }

    private CommercialDashboardMetrics orderMetrics(LocalDate from, LocalDate to) {
        AmountAggregate total = orderRepository.summarizeOrderedBetween(from, to);
        AmountAggregate delivered = orderRepository.summarizeByStatusOrderedBetween(OrderStatus.DELIVERED, from, to);
        return new CommercialDashboardMetrics(
                total.getItemCount(), amountOf(total),
                delivered.getItemCount(), amountOf(delivered),
                rate(delivered.getItemCount(), total.getItemCount()));
    }

    private ContractDashboardMetrics contractMetrics(LocalDate today) {
        AmountAggregate active = contractRepository.summarizeByStatus(ContractStatus.ACTIVE);
        AmountAggregate expiring = contractRepository.summarizeExpiringBetween(ContractStatus.ACTIVE, today,
                today.plusDays(EXPIRING_WINDOW_DAYS));
        return new ContractDashboardMetrics(
                active.getItemCount(), amountOf(active),
                expiring.getItemCount(), amountOf(expiring));
    }

    private List<CommercialMonthlyPoint> monthlySeries() {
        YearMonth firstMonth = YearMonth.now(BUSINESS_ZONE).minusMonths(MONTHLY_SERIES_SIZE - 1L);
        LocalDate seriesStart = firstMonth.atDay(1);

        Map<String, LabeledAmountAggregate> proposals = byLabel(
                proposalRepository.summarizeIssuedByMonth(seriesStart));
        Map<String, LabeledAmountAggregate> orders = byLabel(
                orderRepository.summarizeOrderedByMonth(seriesStart));

        return IntStream.range(0, MONTHLY_SERIES_SIZE)
                .mapToObj(index -> firstMonth.plusMonths(index).format(MONTH_FORMAT))
                .map(month -> new CommercialMonthlyPoint(month, amountOf(proposals.get(month)),
                        amountOf(orders.get(month))))
                .toList();
    }

    private Map<String, LabeledAmountAggregate> byLabel(List<LabeledAmountAggregate> rows) {
        return rows.stream().collect(Collectors.toMap(LabeledAmountAggregate::getLabel, Function.identity(),
                (first, second) -> first));
    }

    private BigDecimal amountOf(AmountAggregate aggregate) {
        if (aggregate == null || aggregate.getTotalAmount() == null) {
            return BigDecimal.ZERO;
        }
        return aggregate.getTotalAmount();
    }

    private BigDecimal rate(long value, long total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value).multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(total), RATE_SCALE, RoundingMode.HALF_UP);
    }
}
