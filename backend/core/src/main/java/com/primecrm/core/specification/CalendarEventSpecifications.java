package com.primecrm.core.specification;

import com.primecrm.infra.entity.agenda.CalendarEvent;
import com.primecrm.infra.entity.agenda.CalendarEventStatus;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class CalendarEventSpecifications {

    private static final String[] TO_ONE_PATHS = {
            "type", "assignee", "customer", "contact", "lead", "opportunity"
    };

    private CalendarEventSpecifications() {
    }

    public static Specification<CalendarEvent> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<CalendarEvent> withReferencesFetched() {
        return SpecificationUtils.fetchToOne(TO_ONE_PATHS);
    }

    public static Specification<CalendarEvent> hasStatus(CalendarEventStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<CalendarEvent> hasType(UUID typeId) {
        return byReferenceId("type", typeId);
    }

    public static Specification<CalendarEvent> hasAssignee(UUID assignedUserId) {
        return byReferenceId("assignee", assignedUserId);
    }

    public static Specification<CalendarEvent> hasCustomer(UUID customerId) {
        return byReferenceId("customer", customerId);
    }

    public static Specification<CalendarEvent> hasLead(UUID leadId) {
        return byReferenceId("lead", leadId);
    }

    public static Specification<CalendarEvent> hasOpportunity(UUID opportunityId) {
        return byReferenceId("opportunity", opportunityId);
    }

    public static Specification<CalendarEvent> startFrom(Instant from) {
        if (from == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startAt"), from);
    }

    public static Specification<CalendarEvent> startTo(Instant to) {
        if (to == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("startAt"), to);
    }

    public static Specification<CalendarEvent> overlapsRange(Instant from, Instant to) {
        if (from == null || to == null) {
            return null;
        }
        return (root, query, cb) -> cb.and(
                cb.lessThanOrEqualTo(root.get("startAt"), to),
                cb.greaterThanOrEqualTo(cb.coalesce(root.get("endAt"), root.get("startAt")), from));
    }

    public static Specification<CalendarEvent> onlyOverdue(Boolean overdue) {
        if (overdue == null) {
            return null;
        }
        return (root, query, cb) -> {
            Predicate isOverdue = cb.and(
                    cb.notEqual(root.get("status"), CalendarEventStatus.DONE),
                    cb.notEqual(root.get("status"), CalendarEventStatus.CANCELED),
                    cb.lessThan(cb.coalesce(root.get("endAt"), root.get("startAt")), Instant.now()));
            return overdue ? isOverdue : cb.not(isOverdue);
        };
    }

    public static Specification<CalendarEvent> textSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("location")), pattern));
    }

    private static Specification<CalendarEvent> byReferenceId(String attribute, UUID id) {
        if (id == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(attribute).get("id"), id);
    }
}
