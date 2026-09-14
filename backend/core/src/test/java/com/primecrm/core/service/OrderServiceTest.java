package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.order.OrderRequest;
import com.primecrm.core.mapper.OrderMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.infra.entity.commercial.Customer;
import com.primecrm.infra.entity.order.Order;
import com.primecrm.infra.entity.order.OrderItem;
import com.primecrm.infra.entity.order.OrderStatus;
import com.primecrm.infra.entity.product.Product;
import com.primecrm.infra.entity.proposal.Proposal;
import com.primecrm.infra.entity.proposal.ProposalItem;
import com.primecrm.infra.repository.OrderItemRepository;
import com.primecrm.infra.repository.OrderRepository;
import com.primecrm.infra.repository.ProposalItemRepository;
import com.primecrm.shared.exception.BusinessException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private CommercialReferenceResolver referenceResolver;
    @Mock
    private AuditService auditService;
    @Mock
    private ProposalService proposalService;
    @Mock
    private ProposalItemRepository proposalItemRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderItemRepository, orderMapper, referenceResolver,
                auditService, proposalService, proposalItemRepository);
    }

    @Test
    void create_startsAsPendingAndIsAudited() {
        OrderRequest request = request();
        Order order = newOrder(OrderStatus.DELIVERED);

        when(orderMapper.toEntity(request)).thenReturn(order);
        when(orderRepository.save(order)).thenReturn(order);

        orderService.create(request);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(auditService).recordCreate(order);
    }

    @Test
    void createFromProposal_copiesCustomerAndItemsFromTheProposal() {
        UUID proposalId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        Proposal proposal = new Proposal();
        proposal.setId(proposalId);
        proposal.setCustomer(customer);

        ProposalItem proposalItem = new ProposalItem();
        proposalItem.setProduct(new Product());
        proposalItem.setQuantity(new BigDecimal("2"));
        proposalItem.setUnitPrice(new BigDecimal("50.00"));
        proposalItem.setDiscountPercent(BigDecimal.ZERO);

        when(proposalService.getActiveOrThrow(proposalId)).thenReturn(proposal);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(UUID.randomUUID());
            }
            return order;
        });
        when(proposalItemRepository.findByProposal_IdAndDeletedAtIsNullOrderByDisplayOrderAsc(proposalId))
                .thenReturn(List.of(proposalItem));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.findByOrder_IdAndDeletedAtIsNullOrderByDisplayOrderAsc(any()))
                .thenReturn(List.of());
        when(orderRepository.findByIdAndDeletedAtIsNull(any())).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            Order order = new Order();
            order.setId(id);
            order.setCustomer(customer);
            return Optional.of(order);
        });

        orderService.createFromProposal(proposalId);

        verify(orderItemRepository).save(any(OrderItem.class));
        verify(auditService).recordCreate(any(Order.class));
    }

    @Test
    void changeStatus_toDelivered_fillsClosedAt() {
        Order order = newOrder(OrderStatus.CONFIRMED);
        when(orderRepository.findByIdAndDeletedAtIsNull(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.changeStatus(order.getId(), OrderStatus.DELIVERED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getClosedAt()).isNotNull();
        verify(auditService).recordUpdate(any(Order.class), any());
    }

    @Test
    void changeStatus_withoutStatus_throwsBusinessException() {
        assertThatThrownBy(() -> orderService.changeStatus(UUID.randomUUID(), null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void recalculateTotal_sumsAllItemTotals() {
        Order order = newOrder(OrderStatus.PENDING);
        when(orderRepository.findByIdAndDeletedAtIsNull(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderItemRepository.findByOrder_IdAndDeletedAtIsNullOrderByDisplayOrderAsc(order.getId()))
                .thenReturn(List.of(itemWithTotal("20.00"), itemWithTotal("5.25")));

        orderService.recalculateTotal(order.getId());

        assertThat(order.getTotalAmount()).isEqualByComparingTo("25.25");
    }

    @Test
    void delete_marksTheOrderAsDeletedAndAudits() {
        Order order = newOrder(OrderStatus.PENDING);
        when(orderRepository.findByIdAndDeletedAtIsNull(order.getId())).thenReturn(Optional.of(order));

        orderService.delete(order.getId());

        assertThat(order.getDeletedAt()).isNotNull();
        verify(auditService).recordDelete(order);
    }

    @Test
    void findById_withUnknownId_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    private OrderItem itemWithTotal(String total) {
        OrderItem item = new OrderItem();
        item.setQuantity(new BigDecimal("1"));
        item.setUnitPrice(new BigDecimal(total));
        return item;
    }

    private OrderRequest request() {
        return new OrderRequest(UUID.randomUUID(), null, null, null, null, null);
    }

    private Order newOrder(OrderStatus status) {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(status);
        return order;
    }
}
