package com.primecrm.core.report;

import com.primecrm.core.dto.report.ReportGroupRow;
import com.primecrm.core.dto.report.ReportResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportAggregator {

    public static final int MAX_ROWS = 200;

    private static final int PERCENTAGE_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final EntityManager entityManager;

    public <T> ReportResponse aggregate(ReportQuery<T> query) {
        List<Tuple> tuples = fetchGroups(query);

        long totalCount = tuples.stream().mapToLong(tuple -> tuple.get(1, Long.class)).sum();
        BigDecimal totalAmount = query.measured()
                ? tuples.stream().map(this::measureOf).reduce(BigDecimal.ZERO, BigDecimal::add)
                : null;

        List<ReportGroupRow> rows = tuples.stream()
                .map(tuple -> toRow(tuple, query.measured(), totalCount))
                .toList();

        return new ReportResponse(query.report(), query.groupBy(), query.measured(), totalCount, totalAmount,
                Instant.now(), rows);
    }

    private <T> List<Tuple> fetchGroups(ReportQuery<T> query) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> criteria = cb.createTupleQuery();
        Root<T> root = criteria.from(query.entityType());

        Expression<?> group = query.dimension().expression(root, cb);
        Expression<Long> count = cb.count(root);

        List<Selection<?>> selections = new ArrayList<>(List.of(group, count));
        if (query.measured()) {
            selections.add(cb.sum(root.<BigDecimal>get(query.measureAttribute())));
        }

        criteria.multiselect(selections);
        criteria.groupBy(group);
        criteria.orderBy(cb.desc(count));

        Predicate predicate = query.filter() == null ? null : query.filter().toPredicate(root, criteria, cb);
        if (predicate != null) {
            criteria.where(predicate);
        }

        return entityManager.createQuery(criteria).setMaxResults(MAX_ROWS).getResultList();
    }

    private ReportGroupRow toRow(Tuple tuple, boolean measured, long totalCount) {
        long count = tuple.get(1, Long.class);
        BigDecimal percentage = totalCount == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(count).multiply(ONE_HUNDRED)
                        .divide(BigDecimal.valueOf(totalCount), PERCENTAGE_SCALE, RoundingMode.HALF_UP);
        return new ReportGroupRow(label(tuple.get(0)), count, measured ? measureOf(tuple) : null, percentage);
    }

    private BigDecimal measureOf(Tuple tuple) {
        Object value = tuple.get(2);
        return value instanceof Number number ? new BigDecimal(number.toString()) : BigDecimal.ZERO;
    }

    private String label(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof Enum<?> enumValue ? enumValue.name() : String.valueOf(value);
    }
}
