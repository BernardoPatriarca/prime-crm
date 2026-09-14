package com.primecrm.core.service;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.order.OrderListFilter;
import com.primecrm.core.dto.order.OrderRequest;
import com.primecrm.core.dto.order.OrderResponse;
import com.primecrm.core.mapper.OrderMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.core.specification.OrderSpecifications;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.infra.entity.order.Order;
import com.primecrm.infra.entity.order.OrderItem;
import com.primecrm.infra.entity.order.OrderStatus;
import com.primecrm.infra.entity.proposal.Proposal;
import com.primecrm.infra.entity.proposal.ProposalItem;
import com.primecrm.infra.repository.OrderItemRepository;
import com.primecrm.infra.repository.OrderRepository;
import com.primecrm.infra.repository.ProposalItemRepository;
import com.primecrm.shared.exception.BusinessException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper orderMapper;
    private final CommercialReferenceResolver referenceResolver;
    private final AuditService auditService;
    private final ProposalService proposalService;
    private final ProposalItemRepository proposalItemRepository;

    @Transactional(readOnly = true)
    public Page<OrderResponse> list(OrderListFilter filter, Pageable pageable) {
        return orderRepository.findAll(toSpecification(filter), pageable).map(orderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(UUID id) {
        return orderMapper.toResponse(getActiveOrThrow(id));
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        Order order = orderMapper.toEntity(request);
        applyReferences(order, request);
        order.setStatus(OrderStatus.PENDING);
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDate.now());
        }

        order = orderRepository.save(order);
        auditService.recordCreate(order);
        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse createFromProposal(UUID proposalId) {
        Proposal proposal = proposalService.getActiveOrThrow(proposalId);

        Order order = new Order();
        order.setCustomer(proposal.getCustomer());
        order.setProposal(proposal);
        order.setOpportunity(proposal.getOpportunity());
        order.setOwner(proposal.getOwner());
        order.setStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDate.now());
        order = orderRepository.save(order);
        auditService.recordCreate(order);

        var proposalItems = proposalItemRepository
                .findByProposal_IdAndDeletedAtIsNullOrderByDisplayOrderAsc(proposalId);
        for (ProposalItem proposalItem : proposalItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(proposalItem.getProduct());
            orderItem.setDescription(proposalItem.getDescription());
            orderItem.setQuantity(proposalItem.getQuantity());
            orderItem.setUnitPrice(proposalItem.getUnitPrice());
            orderItem.setDiscountPercent(proposalItem.getDiscountPercent());
            orderItem.setDisplayOrder(proposalItem.getDisplayOrder());
            orderItemRepository.save(orderItem);
        }

        recalculateTotal(order.getId());
        return orderMapper.toResponse(getActiveOrThrow(order.getId()));
    }

    @Transactional
    public OrderResponse update(UUID id, OrderRequest request) {
        Order order = getActiveOrThrow(id);
        Map<String, Object> previousState = auditService.snapshot(order);

        orderMapper.updateEntity(order, request);
        applyReferences(order, request);

        order = orderRepository.save(order);
        auditService.recordUpdate(order, previousState);
        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse changeStatus(UUID id, OrderStatus status) {
        if (status == null) {
            throw new BusinessException("ORDER_STATUS_REQUIRED", "Informe o novo status do pedido");
        }
        Order order = getActiveOrThrow(id);
        Map<String, Object> previousState = auditService.snapshot(order);

        order.setStatus(status);
        order.setClosedAt(status.isClosed() ? Instant.now() : null);

        order = orderRepository.save(order);
        auditService.recordUpdate(order, previousState);
        return orderMapper.toResponse(order);
    }

    @Transactional
    public void delete(UUID id) {
        Order order = getActiveOrThrow(id);
        order.setDeletedAt(Instant.now());
        orderRepository.save(order);
        auditService.recordDelete(order);
    }

    @Transactional
    public void recalculateTotal(UUID orderId) {
        Order order = getActiveOrThrow(orderId);
        BigDecimal total = orderItemRepository.findByOrder_IdAndDeletedAtIsNullOrderByDisplayOrderAsc(orderId)
                .stream()
                .map(OrderItem::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);
        orderRepository.save(order);
    }

    private Specification<Order> toSpecification(OrderListFilter filter) {
        return SpecificationUtils.and(
                OrderSpecifications.notDeleted(),
                OrderSpecifications.withReferencesFetched(),
                OrderSpecifications.textSearch(filter.search()),
                OrderSpecifications.hasStatus(filter.status()),
                OrderSpecifications.hasCustomer(filter.customerId()),
                OrderSpecifications.hasOpportunity(filter.opportunityId()),
                OrderSpecifications.hasOwner(filter.ownerUserId()));
    }

    private void applyReferences(Order order, OrderRequest request) {
        order.setCustomer(referenceResolver.customer(request.customerId()));
        order.setOpportunity(referenceResolver.opportunity(request.opportunityId()));
        order.setOwner(referenceResolver.user(request.ownerUserId()));
    }

    Order getActiveOrThrow(UUID id) {
        return orderRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));
    }
}
