package com.primecrm.core.specification;

import com.primecrm.infra.entity.commercial.Lead;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class LeadSpecifications {

    private static final String[] TO_ONE_PATHS = {
            "origin", "status", "priority", "owner", "pipeline", "stage", "convertedCustomer"
    };

    private LeadSpecifications() {
    }

    public static Specification<Lead> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Lead> withReferencesFetched() {
        return SpecificationUtils.fetchToOne(TO_ONE_PATHS);
    }

    public static Specification<Lead> hasActive(Boolean active) {
        if (active == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    public static Specification<Lead> hasOrigin(UUID originId) {
        return byReferenceId("origin", originId);
    }

    public static Specification<Lead> hasStatus(UUID statusId) {
        return byReferenceId("status", statusId);
    }

    public static Specification<Lead> hasPriority(UUID priorityId) {
        return byReferenceId("priority", priorityId);
    }

    public static Specification<Lead> hasOwner(UUID ownerUserId) {
        return byReferenceId("owner", ownerUserId);
    }

    public static Specification<Lead> hasPipeline(UUID pipelineId) {
        return byReferenceId("pipeline", pipelineId);
    }

    public static Specification<Lead> hasStage(UUID stageId) {
        return byReferenceId("stage", stageId);
    }

    public static Specification<Lead> expectedCloseFrom(LocalDate from) {
        if (from == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("expectedCloseDate"), from);
    }

    public static Specification<Lead> expectedCloseTo(LocalDate to) {
        if (to == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("expectedCloseDate"), to);
    }

    public static Specification<Lead> textSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            Predicate byName = cb.like(cb.lower(root.get("name")), pattern);
            Predicate byCompanyName = cb.like(cb.lower(root.get("companyName")), pattern);
            Predicate byContactName = cb.like(cb.lower(root.get("contactName")), pattern);
            Predicate byEmail = cb.like(cb.lower(root.get("email")), pattern);
            Predicate byCode = cb.like(cb.lower(root.get("code")), pattern);
            return cb.or(byName, byCompanyName, byContactName, byEmail, byCode);
        };
    }

    private static Specification<Lead> byReferenceId(String attribute, UUID id) {
        if (id == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(attribute).get("id"), id);
    }
}
