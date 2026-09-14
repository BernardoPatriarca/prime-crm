package com.primecrm.api.controller;

import com.primecrm.core.dto.proposal.ProposalItemRequest;
import com.primecrm.core.dto.proposal.ProposalItemResponse;
import com.primecrm.core.service.ProposalItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/proposals/{proposalId}/items")
@RequiredArgsConstructor
@Tag(name = "Itens de Proposta", description = "CRUD dos itens de uma proposta especifica, sempre aninhado ao proposalId")
public class ProposalItemController {

    private final ProposalItemService proposalItemService;

    @GetMapping
    @PreAuthorize("hasAuthority('PROPOSTAS_VIEW')")
    @Operation(summary = "Lista os itens de uma proposta, ordenados por displayOrder")
    public ResponseEntity<List<ProposalItemResponse>> list(@PathVariable UUID proposalId) {
        return ResponseEntity.ok(proposalItemService.list(proposalId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PROPOSTAS_EDIT')")
    @Operation(summary = "Adiciona um item a proposta. Se o preco unitario nao for informado, usa o preco "
            + "de venda atual do produto como snapshot")
    public ResponseEntity<ProposalItemResponse> create(@PathVariable UUID proposalId,
            @Valid @RequestBody ProposalItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proposalItemService.create(proposalId, request));
    }

    @PutMapping("/{itemId}")
    @PreAuthorize("hasAuthority('PROPOSTAS_EDIT')")
    @Operation(summary = "Atualiza um item existente da proposta")
    public ResponseEntity<ProposalItemResponse> update(@PathVariable UUID proposalId, @PathVariable UUID itemId,
            @Valid @RequestBody ProposalItemRequest request) {
        return ResponseEntity.ok(proposalItemService.update(proposalId, itemId, request));
    }

    @DeleteMapping("/{itemId}")
    @PreAuthorize("hasAuthority('PROPOSTAS_EDIT')")
    @Operation(summary = "Remove (soft delete) um item da proposta")
    public ResponseEntity<Void> delete(@PathVariable UUID proposalId, @PathVariable UUID itemId) {
        proposalItemService.delete(proposalId, itemId);
        return ResponseEntity.noContent().build();
    }
}
