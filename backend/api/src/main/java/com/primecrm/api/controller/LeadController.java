package com.primecrm.api.controller;

import com.primecrm.core.dto.commercial.LeadConvertRequest;
import com.primecrm.core.dto.commercial.LeadConvertResponse;
import com.primecrm.core.dto.commercial.LeadListFilter;
import com.primecrm.core.dto.commercial.LeadRequest;
import com.primecrm.core.dto.commercial.LeadResponse;
import com.primecrm.core.service.LeadService;
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
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
@Tag(name = "Leads", description = "Leads (pre-qualificacao) e conversao em cliente")
public class LeadController {

    private final LeadService leadService;

    @GetMapping
    @PreAuthorize("hasAuthority('LEADS_VIEW')")
    @Operation(summary = "Lista leads paginados, com busca textual e filtros por origem, status, prioridade, "
            + "responsavel, funil, etapa, ativo e faixa de previsao de fechamento")
    public ResponseEntity<PageResponse<LeadResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID originId,
            @RequestParam(required = false) UUID statusId,
            @RequestParam(required = false) UUID priorityId,
            @RequestParam(required = false) UUID ownerUserId,
            @RequestParam(required = false) UUID pipelineId,
            @RequestParam(required = false) UUID stageId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedCloseFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedCloseTo,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        LeadListFilter filter = new LeadListFilter(search, originId, statusId, priorityId, ownerUserId,
                pipelineId, stageId, active, expectedCloseFrom, expectedCloseTo);
        return ResponseEntity.ok(PageResponse.from(leadService.list(filter, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LEADS_VIEW')")
    @Operation(summary = "Busca um lead pelo id")
    public ResponseEntity<LeadResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(leadService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('LEADS_CREATE')")
    @Operation(summary = "Cria um lead. O codigo (LEAD-######) e gerado pelo banco e nao aceito no request")
    public ResponseEntity<LeadResponse> create(@Valid @RequestBody LeadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leadService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LEADS_EDIT')")
    @Operation(summary = "Atualiza um lead existente")
    public ResponseEntity<LeadResponse> update(@PathVariable UUID id, @Valid @RequestBody LeadRequest request) {
        return ResponseEntity.ok(leadService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LEADS_DELETE')")
    @Operation(summary = "Exclui (soft delete) um lead")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        leadService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAuthority('LEADS_EDIT') and hasAuthority('CLIENTES_CREATE')")
    @Operation(summary = "Converte o lead em cliente, opcionalmente criando ja a primeira oportunidade. "
            + "Um lead so pode ser convertido uma vez")
    public ResponseEntity<LeadConvertResponse> convert(@PathVariable UUID id,
            @Valid @RequestBody LeadConvertRequest request) {
        return ResponseEntity.ok(leadService.convert(id, request));
    }
}
