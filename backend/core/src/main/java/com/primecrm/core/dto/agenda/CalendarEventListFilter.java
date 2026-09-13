package com.primecrm.core.dto.agenda;

import com.primecrm.infra.entity.agenda.CalendarEventStatus;
import java.time.Instant;
import java.util.UUID;

public record CalendarEventListFilter(
        String search,
        CalendarEventStatus status,
        UUID typeId,
        UUID assignedUserId,
        UUID customerId,
        UUID leadId,
        UUID opportunityId,
        Instant startFrom,
        Instant startTo,
        Boolean overdue
) {
}
