package com.primecrm.core.dto.agenda;

import com.primecrm.core.dto.common.ContactSummaryResponse;
import com.primecrm.core.dto.common.CustomerSummaryResponse;
import com.primecrm.core.dto.common.DomainValueSummaryResponse;
import com.primecrm.core.dto.common.LeadSummaryResponse;
import com.primecrm.core.dto.common.OpportunitySummaryResponse;
import com.primecrm.core.dto.common.UserSummaryResponse;
import com.primecrm.infra.entity.agenda.CalendarEventStatus;
import java.time.Instant;
import java.util.UUID;

public record CalendarEventResponse(
        UUID id,
        String title,
        String description,
        DomainValueSummaryResponse type,
        CalendarEventStatus status,
        Instant startAt,
        Instant endAt,
        boolean allDay,
        String location,
        Instant reminderAt,
        boolean overdue,
        UserSummaryResponse assignee,
        CustomerSummaryResponse customer,
        ContactSummaryResponse contact,
        LeadSummaryResponse lead,
        OpportunitySummaryResponse opportunity,
        Instant createdAt,
        Instant updatedAt
) {
}
