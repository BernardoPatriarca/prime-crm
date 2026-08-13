package com.primecrm.core.dto.task;

import com.primecrm.core.dto.common.ContactSummaryResponse;
import com.primecrm.core.dto.common.CustomerSummaryResponse;
import com.primecrm.core.dto.common.DomainValueSummaryResponse;
import com.primecrm.core.dto.common.LeadSummaryResponse;
import com.primecrm.core.dto.common.OpportunitySummaryResponse;
import com.primecrm.core.dto.common.UserSummaryResponse;
import com.primecrm.infra.entity.task.TaskStatus;
import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String code,
        String title,
        String description,
        DomainValueSummaryResponse type,
        DomainValueSummaryResponse priority,
        TaskStatus status,
        Instant dueAt,
        Instant reminderAt,
        Instant completedAt,
        boolean overdue,
        UserSummaryResponse assignee,
        CustomerSummaryResponse customer,
        ContactSummaryResponse contact,
        LeadSummaryResponse lead,
        OpportunitySummaryResponse opportunity,
        String resultNotes,
        Instant createdAt,
        Instant updatedAt
) {
}
