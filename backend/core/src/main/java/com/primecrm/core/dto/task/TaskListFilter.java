package com.primecrm.core.dto.task;

import com.primecrm.infra.entity.task.TaskStatus;
import java.time.Instant;
import java.util.UUID;

public record TaskListFilter(
        String search,
        TaskStatus status,
        UUID typeId,
        UUID priorityId,
        UUID assignedUserId,
        UUID customerId,
        UUID leadId,
        UUID opportunityId,
        Instant dueFrom,
        Instant dueTo,
        Boolean overdue
) {
}
