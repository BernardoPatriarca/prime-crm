package com.primecrm.core.specification;

import com.primecrm.infra.entity.sales.SalesGoal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class SalesGoalSpecifications {

    private static final String[] TO_ONE_PATHS = {"owner"};

    private SalesGoalSpecifications() {
    }

    public static Specification<SalesGoal> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<SalesGoal> withReferencesFetched() {
        return SpecificationUtils.fetchToOne(TO_ONE_PATHS);
    }

    public static Specification<SalesGoal> hasOwner(UUID ownerId) {
        if (ownerId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId);
    }

    public static Specification<SalesGoal> hasReferenceMonth(LocalDate referenceMonth) {
        if (referenceMonth == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("referenceMonth"), referenceMonth);
    }

    public static Specification<SalesGoal> textSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("notes")), pattern);
    }
}
