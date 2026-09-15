package com.primecrm.api.controller;

import com.primecrm.api.support.SortGuard;
import com.primecrm.core.dto.finance.PayableListFilter;
import com.primecrm.core.dto.finance.PayablePaymentRequest;
import com.primecrm.core.dto.finance.PayableRequest;
import com.primecrm.core.dto.finance.PayableResponse;
import com.primecrm.core.service.PayableService;
import com.primecrm.infra.entity.finance.PayableStatus;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payables")
@RequiredArgsConstructor
@Tag(name = "Contas a Pagar", description = "Despesas e obrigacoes financeiras da empresa junto a fornecedores")
public class PayableController {

    private final PayableService payableService;

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCEIRO_VIEW')")
    @Operation(summary = "Lista contas a pagar paginadas, com busca textual (codigo/descricao/observacoes) "
            + "e filtros por status, fornecedor, categoria, periodo de vencimento e atraso")
    public ResponseEntity<PageResponse<PayableResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) PayableStatus status,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
            @RequestParam(required = false) Boolean overdue,
            @PageableDefault(size = 20, sort = "dueDate") Pageable pageable) {
        PayableListFilter filter = new PayableListFilter(search, status, supplierId, categoryId,
                dueFrom, dueTo, overdue);
        return ResponseEntity.ok(
                PageResponse.from(payableService.list(filter, SortGuard.requireSafeSort(pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCEIRO_VIEW')")
    @Operation(summary = "Busca uma conta a pagar pelo id")
    public ResponseEntity<PayableResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(payableService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCEIRO_CREATE')")
    @Operation(summary = "Cria uma conta a pagar. O codigo (PAG-######) e gerado pelo banco")
    public ResponseEntity<PayableResponse> create(@Valid @RequestBody PayableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payableService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCEIRO_EDIT')")
    @Operation(summary = "Atualiza uma conta a pagar existente")
    public ResponseEntity<PayableResponse> update(@PathVariable UUID id,
            @Valid @RequestBody PayableRequest request) {
        return ResponseEntity.ok(payableService.update(id, request));
    }

    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('FINANCEIRO_EDIT')")
    @Operation(summary = "Registra o pagamento (total ou parcial) de uma conta a pagar. Sem valor informado, "
            + "baixa o saldo restante integralmente")
    public ResponseEntity<PayableResponse> pay(@PathVariable UUID id,
            @Valid @RequestBody PayablePaymentRequest request) {
        return ResponseEntity.ok(payableService.pay(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCEIRO_DELETE')")
    @Operation(summary = "Exclui (soft delete) uma conta a pagar")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        payableService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
