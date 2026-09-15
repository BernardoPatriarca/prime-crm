package com.primecrm.api.controller;

import com.primecrm.api.support.SortGuard;
import com.primecrm.core.dto.finance.GenerateInstallmentsRequest;
import com.primecrm.core.dto.finance.ReceivableListFilter;
import com.primecrm.core.dto.finance.ReceivablePaymentRequest;
import com.primecrm.core.dto.finance.ReceivableRequest;
import com.primecrm.core.dto.finance.ReceivableResponse;
import com.primecrm.core.service.ReceivableService;
import com.primecrm.infra.entity.finance.ReceivableStatus;
import com.primecrm.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/receivables")
@RequiredArgsConstructor
@Tag(name = "Contas a Receber", description = "Parcelas de pagamento originadas de Pedidos ou Contratos")
public class ReceivableController {

    private final ReceivableService receivableService;

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCEIRO_VIEW')")
    @Operation(summary = "Lista contas a receber paginadas, com busca textual (codigo/descricao/observacoes) "
            + "e filtros por status, cliente, pedido, contrato, periodo de vencimento e atraso")
    public ResponseEntity<PageResponse<ReceivableResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ReceivableStatus status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) UUID contractId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
            @RequestParam(required = false) Boolean overdue,
            @PageableDefault(size = 20, sort = "dueDate") Pageable pageable) {
        ReceivableListFilter filter = new ReceivableListFilter(search, status, customerId, orderId, contractId,
                dueFrom, dueTo, overdue);
        return ResponseEntity.ok(
                PageResponse.from(receivableService.list(filter, SortGuard.requireSafeSort(pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCEIRO_VIEW')")
    @Operation(summary = "Busca uma conta a receber pelo id")
    public ResponseEntity<ReceivableResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(receivableService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCEIRO_CREATE')")
    @Operation(summary = "Cria uma conta a receber avulsa. O codigo (REC-######) e gerado pelo banco")
    public ResponseEntity<ReceivableResponse> create(@Valid @RequestBody ReceivableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(receivableService.create(request));
    }

    @PostMapping("/from-order/{orderId}")
    @PreAuthorize("hasAuthority('FINANCEIRO_CREATE')")
    @Operation(summary = "Gera as parcelas de um pedido, dividindo o valor total em N parcelas mensais "
            + "iguais (a ultima absorve o arredondamento)")
    public ResponseEntity<List<ReceivableResponse>> generateFromOrder(@PathVariable UUID orderId,
            @Valid @RequestBody GenerateInstallmentsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(receivableService.generateFromOrder(orderId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCEIRO_EDIT')")
    @Operation(summary = "Atualiza uma conta a receber existente")
    public ResponseEntity<ReceivableResponse> update(@PathVariable UUID id,
            @Valid @RequestBody ReceivableRequest request) {
        return ResponseEntity.ok(receivableService.update(id, request));
    }

    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('FINANCEIRO_EDIT')")
    @Operation(summary = "Registra o pagamento (total ou parcial) de uma conta a receber. Sem valor informado, "
            + "baixa o saldo restante integralmente")
    public ResponseEntity<ReceivableResponse> pay(@PathVariable UUID id,
            @Valid @RequestBody ReceivablePaymentRequest request) {
        return ResponseEntity.ok(receivableService.pay(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCEIRO_DELETE')")
    @Operation(summary = "Exclui (soft delete) uma conta a receber")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        receivableService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
