package com.primecrm.api.controller;

import com.primecrm.api.support.SortGuard;
import com.primecrm.core.dto.proposal.ProposalListFilter;
import com.primecrm.core.dto.proposal.ProposalRequest;
import com.primecrm.core.dto.proposal.ProposalResponse;
import com.primecrm.core.dto.proposal.ProposalStatusUpdateRequest;
import com.primecrm.core.service.DocumentPdfService;
import com.primecrm.core.service.ProposalService;
import com.primecrm.infra.entity.proposal.ProposalStatus;
import com.primecrm.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/proposals")
@RequiredArgsConstructor
@Tag(name = "Propostas", description = "Documentos comerciais enviados ao cliente, com itens de produto/servico")
public class ProposalController {

    private final ProposalService proposalService;
    private final DocumentPdfService documentPdfService;

    @GetMapping
    @PreAuthorize("hasAuthority('PROPOSTAS_VIEW')")
    @Operation(summary = "Lista propostas paginadas, com busca textual (codigo/observacoes) e filtros por "
            + "status, cliente, oportunidade, responsavel e vencidas")
    public ResponseEntity<PageResponse<ProposalResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ProposalStatus status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID opportunityId,
            @RequestParam(required = false) UUID ownerUserId,
            @RequestParam(required = false) Boolean expired,
            @PageableDefault(size = 20, sort = "issueDate") Pageable pageable) {
        ProposalListFilter filter = new ProposalListFilter(search, status, customerId, opportunityId, ownerUserId,
                expired);
        return ResponseEntity.ok(
                PageResponse.from(proposalService.list(filter, SortGuard.requireSafeSort(pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROPOSTAS_VIEW')")
    @Operation(summary = "Busca uma proposta pelo id")
    public ResponseEntity<ProposalResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(proposalService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PROPOSTAS_CREATE')")
    @Operation(summary = "Cria uma proposta em rascunho. O codigo (PRO-######) e gerado pelo banco e os itens "
            + "sao adicionados depois, via /proposals/{id}/items")
    public ResponseEntity<ProposalResponse> create(@Valid @RequestBody ProposalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proposalService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PROPOSTAS_EDIT')")
    @Operation(summary = "Atualiza os dados de cabecalho de uma proposta existente")
    public ResponseEntity<ProposalResponse> update(@PathVariable UUID id, @Valid @RequestBody ProposalRequest request) {
        return ResponseEntity.ok(proposalService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PROPOSTAS_EDIT')")
    @Operation(summary = "Altera o status da proposta. Aceitar ou rejeitar preenche a data de decisao")
    public ResponseEntity<ProposalResponse> changeStatus(@PathVariable UUID id,
            @Valid @RequestBody ProposalStatusUpdateRequest request) {
        return ResponseEntity.ok(proposalService.changeStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PROPOSTAS_DELETE')")
    @Operation(summary = "Exclui (soft delete) uma proposta")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        proposalService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAuthority('PROPOSTAS_VIEW')")
    @Operation(summary = "Gera o PDF da proposta, com dados do cliente, itens e valor total")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        DocumentPdfService.GeneratedPdf pdf = documentPdfService.proposalPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pdf.fileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf.content());
    }
}
