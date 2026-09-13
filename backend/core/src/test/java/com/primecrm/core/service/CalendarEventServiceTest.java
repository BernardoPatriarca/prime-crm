package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.agenda.CalendarEventRequest;
import com.primecrm.core.mapper.CalendarEventMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.infra.entity.agenda.CalendarEvent;
import com.primecrm.infra.entity.agenda.CalendarEventStatus;
import com.primecrm.infra.repository.CalendarEventRepository;
import com.primecrm.shared.exception.BusinessException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CalendarEventServiceTest {

    @Mock
    private CalendarEventRepository calendarEventRepository;
    @Mock
    private CalendarEventMapper calendarEventMapper;
    @Mock
    private CommercialReferenceResolver referenceResolver;
    @Mock
    private AuditService auditService;

    private CalendarEventService calendarEventService;

    private CalendarEventService service() {
        return new CalendarEventService(calendarEventRepository, calendarEventMapper, referenceResolver,
                auditService);
    }

    @Test
    void create_withoutStatus_startsAsScheduledAndIsAudited() {
        calendarEventService = service();
        Instant startAt = Instant.parse("2026-02-10T13:00:00Z");
        CalendarEventRequest request = requestWithStatus(startAt, null, null);
        CalendarEvent event = newEvent(startAt, CalendarEventStatus.DONE);

        when(calendarEventMapper.toEntity(request)).thenReturn(event);
        when(calendarEventRepository.save(event)).thenReturn(event);

        calendarEventService.create(request);

        assertThat(event.getStatus()).isEqualTo(CalendarEventStatus.SCHEDULED);
        verify(auditService).recordCreate(event);
    }

    @Test
    void create_withEndBeforeStart_throwsBusinessException() {
        calendarEventService = service();
        Instant startAt = Instant.parse("2026-02-10T13:00:00Z");
        Instant endAt = startAt.minusSeconds(3600);
        CalendarEventRequest request = requestWithStatus(startAt, endAt, null);

        assertThatThrownBy(() -> calendarEventService.create(request)).isInstanceOf(BusinessException.class);
    }

    @Test
    void range_withoutFromOrTo_throwsBusinessException() {
        calendarEventService = service();
        assertThatThrownBy(() -> calendarEventService.range(null, Instant.now(), null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void update_changesPreviousStatusAndAudits() {
        calendarEventService = service();
        Instant startAt = Instant.parse("2026-02-10T13:00:00Z");
        CalendarEvent event = newEvent(startAt, CalendarEventStatus.SCHEDULED);
        CalendarEventRequest request = requestWithStatus(startAt, null, CalendarEventStatus.CANCELED);

        when(calendarEventRepository.findByIdAndDeletedAtIsNull(event.getId())).thenReturn(Optional.of(event));
        when(calendarEventRepository.save(event)).thenReturn(event);

        calendarEventService.update(event.getId(), request);

        assertThat(event.getStatus()).isEqualTo(CalendarEventStatus.CANCELED);
        verify(auditService).recordUpdate(any(CalendarEvent.class), any());
    }

    @Test
    void delete_marksTheEventAsDeletedAndAudits() {
        calendarEventService = service();
        CalendarEvent event = newEvent(Instant.now(), CalendarEventStatus.SCHEDULED);
        when(calendarEventRepository.findByIdAndDeletedAtIsNull(event.getId())).thenReturn(Optional.of(event));

        calendarEventService.delete(event.getId());

        assertThat(event.getDeletedAt()).isNotNull();
        verify(auditService).recordDelete(event);
    }

    @Test
    void findById_withUnknownId_throwsResourceNotFound() {
        calendarEventService = service();
        UUID id = UUID.randomUUID();
        when(calendarEventRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarEventService.findById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    private CalendarEventRequest requestWithStatus(Instant startAt, Instant endAt, CalendarEventStatus status) {
        return new CalendarEventRequest("Reuniao com cliente", null, null, status, startAt, endAt, false, null,
                null, null, null, null, null, null);
    }

    private CalendarEvent newEvent(Instant startAt, CalendarEventStatus status) {
        CalendarEvent event = new CalendarEvent();
        event.setId(UUID.randomUUID());
        event.setTitle("Reuniao com cliente");
        event.setStartAt(startAt);
        event.setStatus(status);
        return event;
    }
}
