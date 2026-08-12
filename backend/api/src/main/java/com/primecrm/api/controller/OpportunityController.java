package com.primecrm.api.controller;

import com.primecrm.core.dto.commercial.OpportunityBoardResponse;
import com.primecrm.core.dto.commercial.OpportunityListFilter;
import com.primecrm.core.dto.commercial.OpportunityRequest;
import com.primecrm.core.dto.commercial.OpportunityResponse;
import com.primecrm.core.dto.commercial.OpportunityStageHistoryResponse;
import com.primecrm.core.dto.commercial.OpportunityStageMoveRequest;
import com.primecrm.core.security.AuthenticatedUser;
import com.primecrm.core.service.OpportunityService;
import com.primecrm.infra.entity.commercial.OpportunityOutcome;
import com.primecrm.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/opportunities")
@RequiredArgsConstructor
@Tag(name = "Oportunidades", description = "Negocios em funil, quadro kanban e historico de movimentacao")
public class OpportunityController {

    private final OpportunityService opportunityService;

    @GetMapping
    @PreAuthorize("hasAuthority('OPORTUNIDADES_VIEW')")
    @Operation(summary = "Lista oportunidades paginadas, com busca textual (titulo/codigo) e filtros por funil, "
            + "etapa, cliente, responsavel, desfecho, faixa de previsao de fechamento e faixa de valor")
    public ResponseEntity<PageResponse<OpportunityResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID pipelineId,
            @RequestParam(required = false) UUID stageId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID ownerUserId,
            @RequestParam(required = false) OpportunityOutcome outcome,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedCloseFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedCloseTo,
            @RequestParam(required = false) BigDecimal amountFrom,
            @RequestParam(required = false) BigDecimal amountTo,
            @PageableDefault(size = 20, sort = "title") Pageable pageable) {
        OpportunityListFilter filter = new OpportunityListFilter(search, pipelineId, stageId, customerId,
                ownerUserId, outcome, expectedCloseFrom, expectedCloseTo, amountFrom, amountTo);
        return ResponseEntity.ok(PageResponse.from(opportunityService.list(filter, pageable)));
    }

    @GetMapping("/board")
    @PreAuthorize("hasAuthority('OPORTUNIDADES_VIEW')")
    @Operation(summary = "Quadro kanban do funil: etapas na ordem, oportunidades abertas de cada coluna e "
            + "totalizadores (quantidade e soma de valores). limitPerStage limita os cards por coluna")
    public ResponseEntity<OpportunityBoardResponse> board(
            @RequestParam UUID pipelineId,
            @RequestParam(required = false) Integer limitPerStage) {
        return ResponseEntity.ok(opportunityService.board(pipelineId, limitPerStage));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('OPORTUNIDADES_VIEW')")
    @Operation(summary = "Busca uma oportunidade pelo id")
    public ResponseEntity<OpportunityResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(opportunityService.findById(id));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('OPORTUNIDADES_VIEW')")
    @Operation(summary = "Historico de movimentacao entre etapas, do mais antigo para o mais recente")
    public ResponseEntity<List<OpportunityStageHistoryResponse>> history(@PathVariable UUID id) {
        return ResponseEntity.ok(opportunityService.findHistory(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('OPORTUNIDADES_CREATE')")
    @Operation(summary = "Cria uma oportunidade. O codigo (OPO-######) e gerado pelo banco e nao aceito no request")
    public ResponseEntity<OpportunityResponse> create(@Valid @RequestBody OpportunityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(opportunityService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('OPORTUNIDADES_EDIT')")
    @Operation(summary = "Atualiza uma oportunidade existente")
    public ResponseEntity<OpportunityResponse> update(@PathVariable UUID id,
            @Valid @RequestBody OpportunityRequest request) {
        return ResponseEntity.ok(opportunityService.update(id, request));
    }

    @PatchMapping("/{id}/stage")
    @PreAuthorize("hasAuthority('OPORTUNIDADES_EDIT')")
    @Operation(summary = "Move a oportunidade de etapa, registrando o historico e atualizando a probabilidade. "
            + "Etapas de perda exigem lossReasonId e etapas de ganho (100%) exigem winReasonId")
    public ResponseEntity<OpportunityResponse> moveStage(@PathVariable UUID id,
            @Valid @RequestBody OpportunityStageMoveRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(
                opportunityService.moveStage(id, request, currentUser == null ? null : currentUser.id()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('OPORTUNIDADES_DELETE')")
    @Operation(summary = "Exclui (soft delete) uma oportunidade")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        opportunityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
