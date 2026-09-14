package com.primecrm.api.controller;

import com.primecrm.core.dto.order.OrderItemRequest;
import com.primecrm.core.dto.order.OrderItemResponse;
import com.primecrm.core.service.OrderItemService;
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
@RequestMapping("/api/v1/orders/{orderId}/items")
@RequiredArgsConstructor
@Tag(name = "Itens de Pedido", description = "CRUD dos itens de um pedido especifico, sempre aninhado ao orderId")
public class OrderItemController {

    private final OrderItemService orderItemService;

    @GetMapping
    @PreAuthorize("hasAuthority('PEDIDOS_VIEW')")
    @Operation(summary = "Lista os itens de um pedido, ordenados por displayOrder")
    public ResponseEntity<List<OrderItemResponse>> list(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderItemService.list(orderId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PEDIDOS_EDIT')")
    @Operation(summary = "Adiciona um item ao pedido. Se o preco unitario nao for informado, usa o preco "
            + "de venda atual do produto como snapshot")
    public ResponseEntity<OrderItemResponse> create(@PathVariable UUID orderId,
            @Valid @RequestBody OrderItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderItemService.create(orderId, request));
    }

    @PutMapping("/{itemId}")
    @PreAuthorize("hasAuthority('PEDIDOS_EDIT')")
    @Operation(summary = "Atualiza um item existente do pedido")
    public ResponseEntity<OrderItemResponse> update(@PathVariable UUID orderId, @PathVariable UUID itemId,
            @Valid @RequestBody OrderItemRequest request) {
        return ResponseEntity.ok(orderItemService.update(orderId, itemId, request));
    }

    @DeleteMapping("/{itemId}")
    @PreAuthorize("hasAuthority('PEDIDOS_EDIT')")
    @Operation(summary = "Remove (soft delete) um item do pedido")
    public ResponseEntity<Void> delete(@PathVariable UUID orderId, @PathVariable UUID itemId) {
        orderItemService.delete(orderId, itemId);
        return ResponseEntity.noContent().build();
    }
}
