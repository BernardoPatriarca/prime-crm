package com.primecrm.api.controller;

import com.primecrm.core.dto.common.ReorderRequest;
import com.primecrm.core.dto.pipeline.PipelineStageRequest;
import com.primecrm.core.dto.pipeline.PipelineStageResponse;
import com.primecrm.core.service.PipelineStageService;
import com.primecrm.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pipelines/{pipelineId}/stages")
@RequiredArgsConstructor
@Tag(name = "Etapas de Pipeline", description = "CRUD das etapas de um funil especifico, sempre aninhado ao pipelineId")
public class PipelineStageController {

    private final PipelineStageService pipelineStageService;

    @GetMapping
    @PreAuthorize("hasAuthority('PIPELINES_VIEW')")
    @Operation(summary = "Lista as etapas de um funil, ordenadas por displayOrder")
    public ResponseEntity<PageResponse<PipelineStageResponse>> list(
            @PathVariable UUID pipelineId,
            @PageableDefault(size = 50, sort = "displayOrder") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(pipelineStageService.list(pipelineId, pageable)));
    }

    @GetMapping("/{stageId}")
    @PreAuthorize("hasAuthority('PIPELINES_VIEW')")
    @Operation(summary = "Busca uma etapa de um funil pelo id")
    public ResponseEntity<PipelineStageResponse> getById(@PathVariable UUID pipelineId, @PathVariable UUID stageId) {
        return ResponseEntity.ok(pipelineStageService.findById(pipelineId, stageId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PIPELINES_CREATE')")
    @Operation(summary = "Cria uma nova etapa para o funil")
    public ResponseEntity<PipelineStageResponse> create(@PathVariable UUID pipelineId,
                                                          @Valid @RequestBody PipelineStageRequest request) {
        PipelineStageResponse response = pipelineStageService.create(pipelineId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{stageId}")
    @PreAuthorize("hasAuthority('PIPELINES_EDIT')")
    @Operation(summary = "Atualiza uma etapa existente do funil")
    public ResponseEntity<PipelineStageResponse> update(@PathVariable UUID pipelineId, @PathVariable UUID stageId,
                                                          @Valid @RequestBody PipelineStageRequest request) {
        return ResponseEntity.ok(pipelineStageService.update(pipelineId, stageId, request));
    }

    @DeleteMapping("/{stageId}")
    @PreAuthorize("hasAuthority('PIPELINES_DELETE')")
    @Operation(summary = "Exclui (soft delete) uma etapa do funil. Bloqueado se for a unica etapa restante")
    public ResponseEntity<Void> delete(@PathVariable UUID pipelineId, @PathVariable UUID stageId) {
        pipelineStageService.delete(pipelineId, stageId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasAuthority('PIPELINES_EDIT')")
    @Operation(summary = "Reordena em lote o displayOrder das etapas de um funil")
    public ResponseEntity<List<PipelineStageResponse>> reorder(@PathVariable UUID pipelineId,
                                                                 @Valid @RequestBody ReorderRequest request) {
        return ResponseEntity.ok(pipelineStageService.reorder(pipelineId, request));
    }
}
