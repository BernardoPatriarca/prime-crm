package com.primecrm.core.specification;

import com.primecrm.infra.entity.finance.Receivable;
import com.primecrm.infra.entity.finance.ReceivableStatus;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ReceivableSpecifications {

    private static final String[] TO_ONE_PATHS = {"customer", "order", "contract", "paymentMethod"};

    private ReceivableSpecifications() {
    }

    public static Specification<Receivable> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Receivable> withReferencesFetched() {
        return SpecificationUtils.fetchToOne(TO_ONE_PATHS);
    }

    public static Specification<Receivable> hasStatus(ReceivableStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Receivable> hasCustomer(UUID customerId) {
        return byReferenceId("customer", customerId);
    }

    public static Specification<Receivable> hasOrder(UUID orderId) {
        return byReferenceId("order", orderId);
    }

    public static Specification<Receivable> hasContract(UUID contractId) {
        return byReferenceId("contract", contractId);
    }

    public static Specification<Receivable> dueFrom(LocalDate from) {
        if (from == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dueDate"), from);
    }

    public static Specification<Receivable> dueTo(LocalDate to) {
        if (to == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("dueDate"), to);
    }

    public static Specification<Receivable> onlyOverdue(Boolean overdue) {
        if (overdue == null) {
            return null;
        }
        return (root, query, cb) -> {
            Predicate isOverdue = cb.and(
                    cb.equal(root.get("status"), ReceivableStatus.PENDING),
                    cb.lessThan(root.get("dueDate"), LocalDate.now()));
            return overdue ? isOverdue : cb.not(isOverdue);
        };
    }

    public static Specification<Receivable> textSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("notes")), pattern));
    }

    private static Specification<Receivable> byReferenceId(String attribute, UUID id) {
        if (id == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(attribute).get("id"), id);
    }
}
