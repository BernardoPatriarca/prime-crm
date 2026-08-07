package com.primecrm.core.specification;

import com.primecrm.infra.entity.config.Holiday;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class HolidaySpecifications {

    private HolidaySpecifications() {
    }

    public static Specification<Holiday> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Holiday> hasActive(Boolean active) {
        if (active == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    public static Specification<Holiday> fromDate(LocalDate startDate) {
        if (startDate == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("holidayDate"), startDate);
    }

    public static Specification<Holiday> toDate(LocalDate endDate) {
        if (endDate == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("holidayDate"), endDate);
    }

    public static Specification<Holiday> byYear(Integer year) {
        if (year == null) {
            return null;
        }
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return (root, query, cb) -> cb.between(root.get("holidayDate"), start, end);
    }
}
