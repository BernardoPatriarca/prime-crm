package com.primecrm.api.controller;

import com.primecrm.api.support.SortGuard;
import com.primecrm.core.dto.agenda.CalendarEventListFilter;
import com.primecrm.core.dto.agenda.CalendarEventRequest;
import com.primecrm.core.dto.agenda.CalendarEventResponse;
import com.primecrm.core.service.CalendarEventService;
import com.primecrm.infra.entity.agenda.CalendarEventStatus;
import com.primecrm.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agenda")
@RequiredArgsConstructor
@Tag(name = "Agenda", description = "Compromissos do usuario, ligados opcionalmente a cliente, lead ou oportunidade")
public class CalendarEventController {

    private final CalendarEventService calendarEventService;

    @GetMapping
    @PreAuthorize("hasAuthority('AGENDA_VIEW')")
    @Operation(summary = "Lista compromissos paginados, com busca textual e filtros por status, tipo, "
            + "responsavel, cliente, lead, oportunidade, periodo de inicio e atraso")
    public ResponseEntity<PageResponse<CalendarEventResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CalendarEventStatus status,
            @RequestParam(required = false) UUID typeId,
            @RequestParam(required = false) UUID assignedUserId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID leadId,
            @RequestParam(required = false) UUID opportunityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTo,
            @RequestParam(required = false) Boolean overdue,
            @PageableDefault(size = 20, sort = "startAt") Pageable pageable) {
        CalendarEventListFilter filter = new CalendarEventListFilter(search, status, typeId, assignedUserId,
                customerId, leadId, opportunityId, startFrom, startTo, overdue);
        return ResponseEntity.ok(
                PageResponse.from(calendarEventService.list(filter, SortGuard.requireSafeSort(pageable))));
    }

    @GetMapping("/range")
    @PreAuthorize("hasAuthority('AGENDA_VIEW')")
    @Operation(summary = "Lista, sem paginacao, todos os compromissos que se sobrepoem ao periodo informado "
            + "(usado pela visao de calendario mes/semana/dia)")
    public ResponseEntity<List<CalendarEventResponse>> range(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID assignedUserId) {
        return ResponseEntity.ok(calendarEventService.range(from, to, assignedUserId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('AGENDA_VIEW')")
    @Operation(summary = "Busca um compromisso pelo id")
    public ResponseEntity<CalendarEventResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(calendarEventService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('AGENDA_CREATE')")
    @Operation(summary = "Cria um compromisso na agenda")
    public ResponseEntity<CalendarEventResponse> create(@Valid @RequestBody CalendarEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(calendarEventService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('AGENDA_EDIT')")
    @Operation(summary = "Atualiza um compromisso existente")
    public ResponseEntity<CalendarEventResponse> update(@PathVariable UUID id,
            @Valid @RequestBody CalendarEventRequest request) {
        return ResponseEntity.ok(calendarEventService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('AGENDA_DELETE')")
    @Operation(summary = "Exclui (soft delete) um compromisso")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        calendarEventService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
