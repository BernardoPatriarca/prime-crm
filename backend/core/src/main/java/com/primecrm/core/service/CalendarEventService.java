package com.primecrm.core.service;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.agenda.CalendarEventListFilter;
import com.primecrm.core.dto.agenda.CalendarEventRequest;
import com.primecrm.core.dto.agenda.CalendarEventResponse;
import com.primecrm.core.mapper.CalendarEventMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.core.specification.CalendarEventSpecifications;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.infra.entity.agenda.CalendarEvent;
import com.primecrm.infra.entity.agenda.CalendarEventStatus;
import com.primecrm.infra.repository.CalendarEventRepository;
import com.primecrm.shared.exception.BusinessException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;
    private final CalendarEventMapper calendarEventMapper;
    private final CommercialReferenceResolver referenceResolver;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<CalendarEventResponse> list(CalendarEventListFilter filter, Pageable pageable) {
        return calendarEventRepository.findAll(toSpecification(filter), pageable)
                .map(calendarEventMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> range(Instant from, Instant to, UUID assignedUserId) {
        if (from == null || to == null) {
            throw new BusinessException("AGENDA_RANGE_REQUIRED", "Informe o periodo (from/to) da agenda");
        }
        Specification<CalendarEvent> specification = SpecificationUtils.and(
                CalendarEventSpecifications.notDeleted(),
                CalendarEventSpecifications.withReferencesFetched(),
                CalendarEventSpecifications.overlapsRange(from, to),
                CalendarEventSpecifications.hasAssignee(assignedUserId));
        return calendarEventRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "startAt")).stream()
                .map(calendarEventMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CalendarEventResponse findById(UUID id) {
        return calendarEventMapper.toResponse(getActiveOrThrow(id));
    }

    @Transactional
    public CalendarEventResponse create(CalendarEventRequest request) {
        validateRange(request);
        CalendarEvent event = calendarEventMapper.toEntity(request);
        applyReferences(event, request);
        event.setStatus(request.status() == null ? CalendarEventStatus.SCHEDULED : request.status());

        event = calendarEventRepository.save(event);
        auditService.recordCreate(event);
        return calendarEventMapper.toResponse(event);
    }

    @Transactional
    public CalendarEventResponse update(UUID id, CalendarEventRequest request) {
        validateRange(request);
        CalendarEvent event = getActiveOrThrow(id);
        Map<String, Object> previousState = auditService.snapshot(event);

        calendarEventMapper.updateEntity(event, request);
        applyReferences(event, request);
        if (request.status() != null) {
            event.setStatus(request.status());
        }

        event = calendarEventRepository.save(event);
        auditService.recordUpdate(event, previousState);
        return calendarEventMapper.toResponse(event);
    }

    @Transactional
    public void delete(UUID id) {
        CalendarEvent event = getActiveOrThrow(id);
        event.setDeletedAt(Instant.now());
        calendarEventRepository.save(event);
        auditService.recordDelete(event);
    }

    private void validateRange(CalendarEventRequest request) {
        if (request.endAt() != null && request.endAt().isBefore(request.startAt())) {
            throw new BusinessException("AGENDA_INVALID_RANGE",
                    "A data/hora de termino nao pode ser anterior ao inicio");
        }
    }

    private Specification<CalendarEvent> toSpecification(CalendarEventListFilter filter) {
        return SpecificationUtils.and(
                CalendarEventSpecifications.notDeleted(),
                CalendarEventSpecifications.withReferencesFetched(),
                CalendarEventSpecifications.textSearch(filter.search()),
                CalendarEventSpecifications.hasStatus(filter.status()),
                CalendarEventSpecifications.hasType(filter.typeId()),
                CalendarEventSpecifications.hasAssignee(filter.assignedUserId()),
                CalendarEventSpecifications.hasCustomer(filter.customerId()),
                CalendarEventSpecifications.hasLead(filter.leadId()),
                CalendarEventSpecifications.hasOpportunity(filter.opportunityId()),
                CalendarEventSpecifications.startFrom(filter.startFrom()),
                CalendarEventSpecifications.startTo(filter.startTo()),
                CalendarEventSpecifications.onlyOverdue(filter.overdue()));
    }

    private void applyReferences(CalendarEvent event, CalendarEventRequest request) {
        event.setType(referenceResolver.domainValue(request.typeId(), "Tipo de compromisso"));
        event.setAssignee(referenceResolver.user(request.assignedUserId()));
        event.setCustomer(referenceResolver.customer(request.customerId()));
        event.setContact(referenceResolver.contact(request.contactId()));
        event.setLead(referenceResolver.lead(request.leadId()));
        event.setOpportunity(referenceResolver.opportunity(request.opportunityId()));
    }

    private CalendarEvent getActiveOrThrow(UUID id) {
        return calendarEventRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compromisso", id));
    }
}
