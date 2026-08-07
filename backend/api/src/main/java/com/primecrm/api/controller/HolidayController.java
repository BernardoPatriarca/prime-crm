package com.primecrm.api.controller;

import com.primecrm.core.dto.holiday.HolidayRequest;
import com.primecrm.core.dto.holiday.HolidayResponse;
import com.primecrm.core.service.HolidayService;
import com.primecrm.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
@RequestMapping("/api/v1/holidays")
@RequiredArgsConstructor
@Tag(name = "Feriados", description = "Feriados nacionais/regionais usados no calculo de SLA e agenda")
public class HolidayController {

    private final HolidayService holidayService;

    @GetMapping
    @PreAuthorize("hasAuthority('FERIADOS_VIEW')")
    @Operation(summary = "Lista feriados paginados, filtraveis por ano, intervalo de datas e ativo/inativo")
    public ResponseEntity<PageResponse<HolidayResponse>> list(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "holidayDate") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(holidayService.list(year, startDate, endDate, active, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FERIADOS_VIEW')")
    @Operation(summary = "Busca um feriado pelo id")
    public ResponseEntity<HolidayResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(holidayService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FERIADOS_CREATE')")
    @Operation(summary = "Cria um novo feriado")
    public ResponseEntity<HolidayResponse> create(@Valid @RequestBody HolidayRequest request) {
        HolidayResponse response = holidayService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FERIADOS_EDIT')")
    @Operation(summary = "Atualiza um feriado existente")
    public ResponseEntity<HolidayResponse> update(@PathVariable UUID id, @Valid @RequestBody HolidayRequest request) {
        return ResponseEntity.ok(holidayService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FERIADOS_DELETE')")
    @Operation(summary = "Exclui (soft delete) um feriado")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        holidayService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
