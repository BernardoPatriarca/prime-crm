package com.primecrm.core.report;

import com.primecrm.infra.entity.task.Task;
import com.primecrm.infra.entity.task.TaskStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TaskReportGroupBy implements ReportDimension<Task> {

    STATUS(ReportDimension.attribute("status")),
    TYPE(ReportDimension.referenceName("type")),
    PRIORITY(ReportDimension.referenceName("priority")),
    ASSIGNEE(ReportDimension.referenceName("assignee")),
    CUSTOMER(ReportDimension.referenceName("customer")),
    OPPORTUNITY(ReportDimension.referenceName("opportunity")),
    DUE_MONTH(ReportDimension.month("dueAt")),
    CREATED_MONTH(ReportDimension.month("createdAt")),
    COMPLETED_MONTH(ReportDimension.month("completedAt")),
    OVERDUE(TaskReportGroupBy::overdueExpression);

    private static final List<TaskStatus> OPEN_STATUSES = List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS);

    private final ReportDimension<Task> dimension;

    @Override
    public Expression<?> expression(Root<Task> root, CriteriaBuilder cb) {
        return dimension.expression(root, cb);
    }

    private static Expression<?> overdueExpression(Root<Task> root, CriteriaBuilder cb) {
        return cb.selectCase()
                .when(cb.and(
                        cb.isNotNull(root.get("dueAt")),
                        cb.lessThan(root.get("dueAt"), Instant.now()),
                        root.get("status").in(OPEN_STATUSES)), Boolean.TRUE)
                .otherwise(Boolean.FALSE);
    }
}
