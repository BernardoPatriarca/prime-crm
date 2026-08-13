package com.primecrm.core.dto.task;

import com.primecrm.infra.entity.task.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusUpdateRequest(

        @NotNull(message = "Status e obrigatorio")
        TaskStatus status
) {
}
