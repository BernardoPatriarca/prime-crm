package com.primecrm.core.service;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.proposal.ProposalListFilter;
import com.primecrm.core.dto.proposal.ProposalRequest;
import com.primecrm.core.dto.proposal.ProposalResponse;
import com.primecrm.core.mapper.ProposalMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.core.specification.ProposalSpecifications;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.infra.entity.proposal.Proposal;
import com.primecrm.infra.entity.proposal.ProposalItem;
import com.primecrm.infra.entity.proposal.ProposalStatus;
import com.primecrm.infra.repository.ProposalItemRepository;
import com.primecrm.infra.repository.ProposalRepository;
import com.primecrm.shared.exception.BusinessException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
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
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final ProposalItemRepository proposalItemRepository;
    private final ProposalMapper proposalMapper;
    private final CommercialReferenceResolver referenceResolver;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<ProposalResponse> list(ProposalListFilter filter, Pageable pageable) {
        return proposalRepository.findAll(toSpecification(filter), pageable).map(proposalMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProposalResponse findById(UUID id) {
        return proposalMapper.toResponse(getActiveOrThrow(id));
    }

    @Transactional
    public ProposalResponse create(ProposalRequest request) {
        Proposal proposal = proposalMapper.toEntity(request);
        applyReferences(proposal, request);
        proposal.setStatus(ProposalStatus.DRAFT);
        if (proposal.getIssueDate() == null) {
            proposal.setIssueDate(java.time.LocalDate.now());
        }

        proposal = proposalRepository.save(proposal);
        auditService.recordCreate(proposal);
        return proposalMapper.toResponse(proposal);
    }

    @Transactional
    public ProposalResponse update(UUID id, ProposalRequest request) {
        Proposal proposal = getActiveOrThrow(id);
        Map<String, Object> previousState = auditService.snapshot(proposal);

        proposalMapper.updateEntity(proposal, request);
        applyReferences(proposal, request);

        proposal = proposalRepository.save(proposal);
        auditService.recordUpdate(proposal, previousState);
        return proposalMapper.toResponse(proposal);
    }

    @Transactional
    public ProposalResponse changeStatus(UUID id, ProposalStatus status) {
        if (status == null) {
            throw new BusinessException("PROPOSAL_STATUS_REQUIRED", "Informe o novo status da proposta");
        }
        Proposal proposal = getActiveOrThrow(id);
        Map<String, Object> previousState = auditService.snapshot(proposal);

        proposal.setStatus(status);
        proposal.setDecidedAt(status.isClosed() ? Instant.now() : null);

        proposal = proposalRepository.save(proposal);
        auditService.recordUpdate(proposal, previousState);
        return proposalMapper.toResponse(proposal);
    }

    @Transactional
    public void delete(UUID id) {
        Proposal proposal = getActiveOrThrow(id);
        proposal.setDeletedAt(Instant.now());
        proposalRepository.save(proposal);
        auditService.recordDelete(proposal);
    }

    @Transactional
    public void recalculateTotal(UUID proposalId) {
        Proposal proposal = getActiveOrThrow(proposalId);
        BigDecimal total = proposalItemRepository.findByProposal_IdAndDeletedAtIsNullOrderByDisplayOrderAsc(proposalId)
                .stream()
                .map(ProposalItem::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        proposal.setTotalAmount(total);
        proposalRepository.save(proposal);
    }

    private Specification<Proposal> toSpecification(ProposalListFilter filter) {
        return SpecificationUtils.and(
                ProposalSpecifications.notDeleted(),
                ProposalSpecifications.withReferencesFetched(),
                ProposalSpecifications.textSearch(filter.search()),
                ProposalSpecifications.hasStatus(filter.status()),
                ProposalSpecifications.hasCustomer(filter.customerId()),
                ProposalSpecifications.hasOpportunity(filter.opportunityId()),
                ProposalSpecifications.hasOwner(filter.ownerUserId()),
                ProposalSpecifications.onlyExpired(filter.expired()));
    }

    private void applyReferences(Proposal proposal, ProposalRequest request) {
        proposal.setCustomer(referenceResolver.customer(request.customerId()));
        proposal.setContact(referenceResolver.contact(request.contactId()));
        proposal.setOpportunity(referenceResolver.opportunity(request.opportunityId()));
        proposal.setOwner(referenceResolver.user(request.ownerUserId()));
    }

    Proposal getActiveOrThrow(UUID id) {
        return proposalRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proposta", id));
    }
}
