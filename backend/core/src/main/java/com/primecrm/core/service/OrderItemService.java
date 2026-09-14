package com.primecrm.core.service;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.order.OrderItemRequest;
import com.primecrm.core.dto.order.OrderItemResponse;
import com.primecrm.core.mapper.OrderItemMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.infra.entity.order.Order;
import com.primecrm.infra.entity.order.OrderItem;
import com.primecrm.infra.entity.product.Product;
import com.primecrm.infra.repository.OrderItemRepository;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final OrderItemMapper orderItemMapper;
    private final OrderService orderService;
    private final CommercialReferenceResolver referenceResolver;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<OrderItemResponse> list(UUID orderId) {
        orderService.getActiveOrThrow(orderId);
        return orderItemRepository.findByOrder_IdAndDeletedAtIsNullOrderByDisplayOrderAsc(orderId)
                .stream()
                .map(orderItemMapper::toResponse)
                .toList();
    }

    @Transactional
    public OrderItemResponse create(UUID orderId, OrderItemRequest request) {
        Order order = orderService.getActiveOrThrow(orderId);
        Product product = referenceResolver.product(request.productId());

        OrderItem item = orderItemMapper.toEntity(request);
        item.setOrder(order);
        item.setProduct(product);
        item.setUnitPrice(request.unitPrice() != null ? request.unitPrice() : product.getUnitPrice());
        item.setDiscountPercent(request.discountPercent() != null ? request.discountPercent() : BigDecimal.ZERO);
        item.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : nextDisplayOrder(orderId));

        item = orderItemRepository.save(item);
        auditService.recordCreate(item);
        orderService.recalculateTotal(orderId);
        return orderItemMapper.toResponse(item);
    }

    @Transactional
    public OrderItemResponse update(UUID orderId, UUID itemId, OrderItemRequest request) {
        OrderItem item = getActiveOrThrow(orderId, itemId);
        Map<String, Object> previousState = auditService.snapshot(item);

        orderItemMapper.updateEntity(item, request);
        if (request.productId() != null && !request.productId().equals(item.getProduct().getId())) {
            item.setProduct(referenceResolver.product(request.productId()));
        }
        if (request.unitPrice() != null) {
            item.setUnitPrice(request.unitPrice());
        }
        if (request.discountPercent() != null) {
            item.setDiscountPercent(request.discountPercent());
        }
        if (request.displayOrder() != null) {
            item.setDisplayOrder(request.displayOrder());
        }

        item = orderItemRepository.save(item);
        auditService.recordUpdate(item, previousState);
        orderService.recalculateTotal(orderId);
        return orderItemMapper.toResponse(item);
    }

    @Transactional
    public void delete(UUID orderId, UUID itemId) {
        OrderItem item = getActiveOrThrow(orderId, itemId);
        item.setDeletedAt(Instant.now());
        orderItemRepository.save(item);
        auditService.recordDelete(item);
        orderService.recalculateTotal(orderId);
    }

    private int nextDisplayOrder(UUID orderId) {
        return (int) orderItemRepository.countByOrder_IdAndDeletedAtIsNull(orderId);
    }

    private OrderItem getActiveOrThrow(UUID orderId, UUID itemId) {
        return orderItemRepository.findByIdAndOrder_IdAndDeletedAtIsNull(itemId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Item do pedido", itemId));
    }
}
