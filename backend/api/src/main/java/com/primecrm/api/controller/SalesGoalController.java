package com.primecrm.api.controller;

import com.primecrm.api.support.SortGuard;
import com.primecrm.core.dto.sales.SalesGoalListFilter;
import com.primecrm.core.dto.sales.SalesGoalRequest;
import com.primecrm.core.dto.sales.SalesGoalResponse;
import com.primecrm.core.service.SalesGoalService;
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
@RequestMapping("/api/v1/sales-goals")
@RequiredArgsConstructor
@Tag(name = "Metas comerciais", description = "Meta de vendas (oportunidades ganhas) por vendedor e mes")
public class SalesGoalController {

    private final SalesGoalService salesGoalService;

    @GetMapping
    @PreAuthorize("hasAuthority('METAS_VIEW')")
    @Operation(summary = "Lista metas comerciais paginadas, com busca textual (observacoes) e filtros por "
            + "vendedor e mes de referencia. Cada meta retorna o valor realizado e o percentual de atingimento, "
            + "calculados a partir das oportunidades ganhas do vendedor no mes")
    public ResponseEntity<PageResponse<SalesGoalResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID ownerUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceMonth,
            @PageableDefault(size = 20, sort = "referenceMonth") Pageable pageable) {
        SalesGoalListFilter filter = new SalesGoalListFilter(search, ownerUserId, referenceMonth);
        return ResponseEntity.ok(
                PageResponse.from(salesGoalService.list(filter, SortGuard.requireSafeSort(pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('METAS_VIEW')")
    @Operation(summary = "Busca uma meta comercial pelo id")
    public ResponseEntity<SalesGoalResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(salesGoalService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('METAS_CREATE')")
    @Operation(summary = "Cria uma meta comercial. O mes de referencia e normalizado para o primeiro dia do mes")
    public ResponseEntity<SalesGoalResponse> create(@Valid @RequestBody SalesGoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salesGoalService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('METAS_EDIT')")
    @Operation(summary = "Atualiza uma meta comercial existente")
    public ResponseEntity<SalesGoalResponse> update(@PathVariable UUID id,
            @Valid @RequestBody SalesGoalRequest request) {
        return ResponseEntity.ok(salesGoalService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('METAS_DELETE')")
    @Operation(summary = "Exclui (soft delete) uma meta comercial")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        salesGoalService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
