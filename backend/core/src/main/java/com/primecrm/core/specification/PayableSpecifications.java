package com.primecrm.core.specification;

import com.primecrm.infra.entity.finance.Payable;
import com.primecrm.infra.entity.finance.PayableStatus;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class PayableSpecifications {

    private static final String[] TO_ONE_PATHS = {"supplier", "category", "paymentMethod"};

    private PayableSpecifications() {
    }

    public static Specification<Payable> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Payable> withReferencesFetched() {
        return SpecificationUtils.fetchToOne(TO_ONE_PATHS);
    }

    public static Specification<Payable> hasStatus(PayableStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Payable> hasSupplier(UUID supplierId) {
        return byReferenceId("supplier", supplierId);
    }

    public static Specification<Payable> hasCategory(UUID categoryId) {
        return byReferenceId("category", categoryId);
    }

    public static Specification<Payable> dueFrom(LocalDate from) {
        if (from == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dueDate"), from);
    }

    public static Specification<Payable> dueTo(LocalDate to) {
        if (to == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("dueDate"), to);
    }

    public static Specification<Payable> onlyOverdue(Boolean overdue) {
        if (overdue == null) {
            return null;
        }
        return (root, query, cb) -> {
            Predicate isOverdue = cb.and(
                    cb.equal(root.get("status"), PayableStatus.PENDING),
                    cb.lessThan(root.get("dueDate"), LocalDate.now()));
            return overdue ? isOverdue : cb.not(isOverdue);
        };
    }

    public static Specification<Payable> textSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("notes")), pattern));
    }

    private static Specification<Payable> byReferenceId(String attribute, UUID id) {
        if (id == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(attribute).get("id"), id);
    }
}
