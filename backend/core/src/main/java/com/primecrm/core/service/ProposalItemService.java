package com.primecrm.core.service;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.proposal.ProposalItemRequest;
import com.primecrm.core.dto.proposal.ProposalItemResponse;
import com.primecrm.core.mapper.ProposalItemMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.infra.entity.product.Product;
import com.primecrm.infra.entity.proposal.Proposal;
import com.primecrm.infra.entity.proposal.ProposalItem;
import com.primecrm.infra.repository.ProposalItemRepository;
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
public class ProposalItemService {

    private final ProposalItemRepository proposalItemRepository;
    private final ProposalItemMapper proposalItemMapper;
    private final ProposalService proposalService;
    private final CommercialReferenceResolver referenceResolver;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ProposalItemResponse> list(UUID proposalId) {
        proposalService.getActiveOrThrow(proposalId);
        return proposalItemRepository.findByProposal_IdAndDeletedAtIsNullOrderByDisplayOrderAsc(proposalId)
                .stream()
                .map(proposalItemMapper::toResponse)
                .toList();
    }

    @Transactional
    public ProposalItemResponse create(UUID proposalId, ProposalItemRequest request) {
        Proposal proposal = proposalService.getActiveOrThrow(proposalId);
        Product product = referenceResolver.product(request.productId());

        ProposalItem item = proposalItemMapper.toEntity(request);
        item.setProposal(proposal);
        item.setProduct(product);
        item.setUnitPrice(request.unitPrice() != null ? request.unitPrice() : product.getUnitPrice());
        item.setDiscountPercent(request.discountPercent() != null ? request.discountPercent() : BigDecimal.ZERO);
        item.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : nextDisplayOrder(proposalId));

        item = proposalItemRepository.save(item);
        auditService.recordCreate(item);
        proposalService.recalculateTotal(proposalId);
        return proposalItemMapper.toResponse(item);
    }

    @Transactional
    public ProposalItemResponse update(UUID proposalId, UUID itemId, ProposalItemRequest request) {
        ProposalItem item = getActiveOrThrow(proposalId, itemId);
        Map<String, Object> previousState = auditService.snapshot(item);

        proposalItemMapper.updateEntity(item, request);
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

        item = proposalItemRepository.save(item);
        auditService.recordUpdate(item, previousState);
        proposalService.recalculateTotal(proposalId);
        return proposalItemMapper.toResponse(item);
    }

    @Transactional
    public void delete(UUID proposalId, UUID itemId) {
        ProposalItem item = getActiveOrThrow(proposalId, itemId);
        item.setDeletedAt(Instant.now());
        proposalItemRepository.save(item);
        auditService.recordDelete(item);
        proposalService.recalculateTotal(proposalId);
    }

    private int nextDisplayOrder(UUID proposalId) {
        return (int) proposalItemRepository.countByProposal_IdAndDeletedAtIsNull(proposalId);
    }

    private ProposalItem getActiveOrThrow(UUID proposalId, UUID itemId) {
        return proposalItemRepository.findByIdAndProposal_IdAndDeletedAtIsNull(itemId, proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Item da proposta", itemId));
    }
}
