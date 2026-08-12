package com.primecrm.core.specification;

import com.primecrm.infra.entity.commercial.Opportunity;
import com.primecrm.infra.entity.commercial.OpportunityOutcome;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class OpportunitySpecifications {

    private static final String[] TO_ONE_PATHS = {
            "customer", "contact", "pipeline", "stage", "owner", "team", "winReason", "lossReason"
    };

    private OpportunitySpecifications() {
    }

    public static Specification<Opportunity> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Opportunity> withReferencesFetched() {
        return SpecificationUtils.fetchToOne(TO_ONE_PATHS);
    }

    public static Specification<Opportunity> hasPipeline(UUID pipelineId) {
        return byReferenceId("pipeline", pipelineId);
    }

    public static Specification<Opportunity> hasStage(UUID stageId) {
        return byReferenceId("stage", stageId);
    }

    public static Specification<Opportunity> hasCustomer(UUID customerId) {
        return byReferenceId("customer", customerId);
    }

    public static Specification<Opportunity> hasOwner(UUID ownerUserId) {
        return byReferenceId("owner", ownerUserId);
    }

    public static Specification<Opportunity> hasOutcome(OpportunityOutcome outcome) {
        if (outcome == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("outcome"), outcome);
    }

    public static Specification<Opportunity> expectedCloseFrom(LocalDate from) {
        if (from == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("expectedCloseDate"), from);
    }

    public static Specification<Opportunity> expectedCloseTo(LocalDate to) {
        if (to == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("expectedCloseDate"), to);
    }

    public static Specification<Opportunity> amountFrom(BigDecimal from) {
        if (from == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("amount"), from);
    }

    public static Specification<Opportunity> amountTo(BigDecimal to) {
        if (to == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("amount"), to);
    }

    public static Specification<Opportunity> textSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            Predicate byTitle = cb.like(cb.lower(root.get("title")), pattern);
            Predicate byCode = cb.like(cb.lower(root.get("code")), pattern);
            return cb.or(byTitle, byCode);
        };
    }

    private static Specification<Opportunity> byReferenceId(String attribute, UUID id) {
        if (id == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(attribute).get("id"), id);
    }
}
