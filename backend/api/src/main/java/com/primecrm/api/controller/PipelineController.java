package com.primecrm.api.controller;

import com.primecrm.core.dto.pipeline.PipelineRequest;
import com.primecrm.core.dto.pipeline.PipelineResponse;
import com.primecrm.core.service.PipelineService;
import com.primecrm.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/v1/pipelines")
@RequiredArgsConstructor
@Tag(name = "Pipelines", description = "Gestao de funis de vendas/pos-venda e suas etapas")
public class PipelineController {

    private final PipelineService pipelineService;

    @GetMapping
    @PreAuthorize("hasAuthority('PIPELINES_VIEW')")
    @Operation(summary = "Lista funis paginados, com busca textual e filtro por ativo/inativo")
    public ResponseEntity<PageResponse<PipelineResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(pipelineService.list(search, active, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PIPELINES_VIEW')")
    @Operation(summary = "Busca um funil pelo id, incluindo suas etapas")
    public ResponseEntity<PipelineResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(pipelineService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PIPELINES_CREATE')")
    @Operation(summary = "Cria um novo funil")
    public ResponseEntity<PipelineResponse> create(@Valid @RequestBody PipelineRequest request) {
        PipelineResponse response = pipelineService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PIPELINES_EDIT')")
    @Operation(summary = "Atualiza um funil existente")
    public ResponseEntity<PipelineResponse> update(@PathVariable UUID id, @Valid @RequestBody PipelineRequest request) {
        return ResponseEntity.ok(pipelineService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PIPELINES_DELETE')")
    @Operation(summary = "Exclui (soft delete) um funil")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        pipelineService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
