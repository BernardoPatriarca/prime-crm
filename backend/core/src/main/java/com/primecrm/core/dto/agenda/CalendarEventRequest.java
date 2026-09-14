package com.primecrm.core.dto.agenda;

import com.primecrm.infra.entity.agenda.CalendarEventStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record CalendarEventRequest(

        @NotBlank(message = "Titulo e obrigatorio")
        @Size(max = 200)
        String title,

        String description,

        UUID typeId,

        CalendarEventStatus status,

        @NotNull(message = "Data/hora de inicio e obrigatoria")
        Instant startAt,

        Instant endAt,

        boolean allDay,

        @Size(max = 200)
        String location,

        Instant reminderAt,

        UUID assignedUserId,

        UUID customerId,

        UUID contactId,

        UUID leadId,

        UUID opportunityId
) {
}
