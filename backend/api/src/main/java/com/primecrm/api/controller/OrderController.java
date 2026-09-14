package com.primecrm.api.controller;

import com.primecrm.api.support.SortGuard;
import com.primecrm.core.dto.order.OrderListFilter;
import com.primecrm.core.dto.order.OrderRequest;
import com.primecrm.core.dto.order.OrderResponse;
import com.primecrm.core.dto.order.OrderStatusUpdateRequest;
import com.primecrm.core.service.OrderService;
import com.primecrm.infra.entity.order.OrderStatus;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "Confirmacao formal de uma venda, com itens de produto/servico")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasAuthority('PEDIDOS_VIEW')")
    @Operation(summary = "Lista pedidos paginados, com busca textual (codigo/observacoes) e filtros por "
            + "status, cliente, oportunidade e responsavel")
    public ResponseEntity<PageResponse<OrderResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID opportunityId,
            @RequestParam(required = false) UUID ownerUserId,
            @PageableDefault(size = 20, sort = "orderDate") Pageable pageable) {
        OrderListFilter filter = new OrderListFilter(search, status, customerId, opportunityId, ownerUserId);
        return ResponseEntity.ok(PageResponse.from(orderService.list(filter, SortGuard.requireSafeSort(pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PEDIDOS_VIEW')")
    @Operation(summary = "Busca um pedido pelo id")
    public ResponseEntity<OrderResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PEDIDOS_CREATE')")
    @Operation(summary = "Cria um pedido. O codigo (PED-######) e gerado pelo banco e os itens sao "
            + "adicionados depois, via /orders/{id}/items")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
    }

    @PostMapping("/from-proposal/{proposalId}")
    @PreAuthorize("hasAuthority('PEDIDOS_CREATE')")
    @Operation(summary = "Cria um pedido a partir de uma proposta, copiando cliente, oportunidade, "
            + "responsavel e todos os itens (com o mesmo preco e desconto praticados na proposta)")
    public ResponseEntity<OrderResponse> createFromProposal(@PathVariable UUID proposalId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createFromProposal(proposalId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PEDIDOS_EDIT')")
    @Operation(summary = "Atualiza os dados de cabecalho de um pedido existente")
    public ResponseEntity<OrderResponse> update(@PathVariable UUID id, @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PEDIDOS_EDIT')")
    @Operation(summary = "Altera o status do pedido. Entregar ou cancelar preenche a data de encerramento")
    public ResponseEntity<OrderResponse> changeStatus(@PathVariable UUID id,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(orderService.changeStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PEDIDOS_DELETE')")
    @Operation(summary = "Exclui (soft delete) um pedido")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
