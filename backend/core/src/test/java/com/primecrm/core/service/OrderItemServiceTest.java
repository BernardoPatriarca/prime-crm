package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.order.OrderItemRequest;
import com.primecrm.core.mapper.OrderItemMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.infra.entity.order.Order;
import com.primecrm.infra.entity.order.OrderItem;
import com.primecrm.infra.entity.product.Product;
import com.primecrm.infra.repository.OrderItemRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private OrderService orderService;
    @Mock
    private CommercialReferenceResolver referenceResolver;
    @Mock
    private AuditService auditService;

    private OrderItemService orderItemService;

    @BeforeEach
    void setUp() {
        orderItemService = new OrderItemService(orderItemRepository, orderItemMapper, orderService,
                referenceResolver, auditService);
    }

    @Test
    void create_withoutUnitPrice_snapshotsTheProductPrice() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setUnitPrice(new BigDecimal("120.00"));

        OrderItemRequest request = new OrderItemRequest(product.getId(), null, new BigDecimal("3"), null, null, null);
        OrderItem item = new OrderItem();

        when(orderService.getActiveOrThrow(orderId)).thenReturn(order);
        when(referenceResolver.product(product.getId())).thenReturn(product);
        when(orderItemMapper.toEntity(request)).thenReturn(item);
        when(orderItemRepository.save(item)).thenReturn(item);
        when(orderItemRepository.countByOrder_IdAndDeletedAtIsNull(orderId)).thenReturn(0L);

        orderItemService.create(orderId, request);

        assertThat(item.getUnitPrice()).isEqualByComparingTo("120.00");
        assertThat(item.getDiscountPercent()).isEqualByComparingTo("0");
        verify(auditService).recordCreate(item);
        verify(orderService).recalculateTotal(orderId);
    }

    @Test
    void delete_marksTheItemAsDeletedAndRecalculatesTotal() {
        UUID orderId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        OrderItem item = new OrderItem();
        item.setId(itemId);
        item.setProduct(new Product());

        when(orderItemRepository.findByIdAndOrder_IdAndDeletedAtIsNull(itemId, orderId))
                .thenReturn(Optional.of(item));

        orderItemService.delete(orderId, itemId);

        assertThat(item.getDeletedAt()).isNotNull();
        verify(auditService).recordDelete(item);
        verify(orderService).recalculateTotal(orderId);
    }
}
