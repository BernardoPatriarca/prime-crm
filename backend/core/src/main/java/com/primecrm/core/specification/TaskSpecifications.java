package com.primecrm.core.specification;

import com.primecrm.infra.entity.task.Task;
import com.primecrm.infra.entity.task.TaskStatus;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class TaskSpecifications {

    private static final String[] TO_ONE_PATHS = {
            "type", "priority", "assignee", "customer", "contact", "lead", "opportunity"
    };

    private static final List<TaskStatus> OPEN_STATUSES = List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS);

    private TaskSpecifications() {
    }

    public static Specification<Task> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Task> withReferencesFetched() {
        return SpecificationUtils.fetchToOne(TO_ONE_PATHS);
    }

    public static Specification<Task> hasStatus(TaskStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Task> hasType(UUID typeId) {
        return byReferenceId("type", typeId);
    }

    public static Specification<Task> hasPriority(UUID priorityId) {
        return byReferenceId("priority", priorityId);
    }

    public static Specification<Task> hasAssignee(UUID assignedUserId) {
        return byReferenceId("assignee", assignedUserId);
    }

    public static Specification<Task> hasCustomer(UUID customerId) {
        return byReferenceId("customer", customerId);
    }

    public static Specification<Task> hasOpportunity(UUID opportunityId) {
        return byReferenceId("opportunity", opportunityId);
    }

    public static Specification<Task> hasLead(UUID leadId) {
        return byReferenceId("lead", leadId);
    }

    public static Specification<Task> dueFrom(Instant from) {
        if (from == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dueAt"), from);
    }

    public static Specification<Task> dueTo(Instant to) {
        if (to == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("dueAt"), to);
    }

    public static Specification<Task> onlyOverdue(Boolean overdue) {
        if (overdue == null) {
            return null;
        }
        return (root, query, cb) -> {
            Predicate isOverdue = cb.and(
                    cb.isNotNull(root.get("dueAt")),
                    cb.lessThan(root.get("dueAt"), Instant.now()),
                    root.get("status").in(OPEN_STATUSES));
            return overdue ? isOverdue : cb.not(isOverdue);
        };
    }

    public static Specification<Task> textSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("description")), pattern));
    }

    private static Specification<Task> byReferenceId(String attribute, UUID id) {
        if (id == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(attribute).get("id"), id);
    }
}
