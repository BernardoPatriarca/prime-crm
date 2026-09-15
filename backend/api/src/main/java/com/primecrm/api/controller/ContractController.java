package com.primecrm.api.controller;

import com.primecrm.api.support.SortGuard;
import com.primecrm.core.dto.contract.ContractListFilter;
import com.primecrm.core.dto.contract.ContractRequest;
import com.primecrm.core.dto.contract.ContractResponse;
import com.primecrm.core.dto.contract.ContractStatusUpdateRequest;
import com.primecrm.core.service.ContractService;
import com.primecrm.core.service.DocumentPdfService;
import com.primecrm.infra.entity.contract.ContractStatus;
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
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
@Tag(name = "Contratos", description = "Vigencia formal de uma venda recorrente ou continuada")
public class ContractController {

    private final ContractService contractService;
    private final DocumentPdfService documentPdfService;

    @GetMapping
    @PreAuthorize("hasAuthority('CONTRATOS_VIEW')")
    @Operation(summary = "Lista contratos paginados, com busca textual (codigo/observacoes) e filtros por "
            + "status, cliente, oportunidade, responsavel e vencidos")
    public ResponseEntity<PageResponse<ContractResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID opportunityId,
            @RequestParam(required = false) UUID ownerUserId,
            @RequestParam(required = false) Boolean expired,
            @PageableDefault(size = 20, sort = "startDate") Pageable pageable) {
        ContractListFilter filter = new ContractListFilter(search, status, customerId, opportunityId, ownerUserId,
                expired);
        return ResponseEntity.ok(
                PageResponse.from(contractService.list(filter, SortGuard.requireSafeSort(pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTRATOS_VIEW')")
    @Operation(summary = "Busca um contrato pelo id")
    public ResponseEntity<ContractResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(contractService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONTRATOS_CREATE')")
    @Operation(summary = "Cria um contrato em rascunho. O codigo (CTR-######) e gerado pelo banco")
    public ResponseEntity<ContractResponse> create(@Valid @RequestBody ContractRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contractService.create(request));
    }

    @PostMapping("/from-order/{orderId}")
    @PreAuthorize("hasAuthority('CONTRATOS_CREATE')")
    @Operation(summary = "Cria um contrato a partir de um pedido, copiando cliente, oportunidade, responsavel "
            + "e usando o valor total do pedido como valor recorrente inicial")
    public ResponseEntity<ContractResponse> createFromOrder(@PathVariable UUID orderId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contractService.createFromOrder(orderId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTRATOS_EDIT')")
    @Operation(summary = "Atualiza os dados de um contrato existente")
    public ResponseEntity<ContractResponse> update(@PathVariable UUID id, @Valid @RequestBody ContractRequest request) {
        return ResponseEntity.ok(contractService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CONTRATOS_EDIT')")
    @Operation(summary = "Altera o status do contrato. Encerrar (TERMINATED) preenche a data de encerramento")
    public ResponseEntity<ContractResponse> changeStatus(@PathVariable UUID id,
            @Valid @RequestBody ContractStatusUpdateRequest request) {
        return ResponseEntity.ok(contractService.changeStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTRATOS_DELETE')")
    @Operation(summary = "Exclui (soft delete) um contrato")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        contractService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAuthority('CONTRATOS_VIEW')")
    @Operation(summary = "Gera o PDF do contrato, com dados do cliente, itens do pedido de origem e valor recorrente")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        DocumentPdfService.GeneratedPdf pdf = documentPdfService.contractPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pdf.fileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf.content());
    }
}
