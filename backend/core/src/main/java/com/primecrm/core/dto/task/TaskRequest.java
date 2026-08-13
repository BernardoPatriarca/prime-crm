package com.primecrm.core.dto.task;

import com.primecrm.infra.entity.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record TaskRequest(

        @NotBlank(message = "Titulo e obrigatorio")
        @Size(max = 200)
        String title,

        String description,

        UUID typeId,

        UUID priorityId,

        TaskStatus status,

        Instant dueAt,

        Instant reminderAt,

        UUID assignedUserId,

        UUID customerId,

        UUID contactId,

        UUID leadId,

        UUID opportunityId,

        String resultNotes
) {
}
