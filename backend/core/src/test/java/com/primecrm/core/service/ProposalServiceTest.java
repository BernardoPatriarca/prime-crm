package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.proposal.ProposalRequest;
import com.primecrm.core.mapper.ProposalMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.infra.entity.proposal.Proposal;
import com.primecrm.infra.entity.proposal.ProposalStatus;
import com.primecrm.infra.repository.ProposalItemRepository;
import com.primecrm.infra.repository.ProposalRepository;
import com.primecrm.shared.exception.BusinessException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProposalServiceTest {

    @Mock
    private ProposalRepository proposalRepository;
    @Mock
    private ProposalItemRepository proposalItemRepository;
    @Mock
    private ProposalMapper proposalMapper;
    @Mock
    private CommercialReferenceResolver referenceResolver;
    @Mock
    private AuditService auditService;

    private ProposalService proposalService;

    @BeforeEach
    void setUp() {
        proposalService = new ProposalService(proposalRepository, proposalItemRepository, proposalMapper,
                referenceResolver, auditService);
    }

    @Test
    void create_startsAsDraftAndIsAudited() {
        ProposalRequest request = request();
        Proposal proposal = newProposal(ProposalStatus.ACCEPTED);

        when(proposalMapper.toEntity(request)).thenReturn(proposal);
        when(proposalRepository.save(proposal)).thenReturn(proposal);

        proposalService.create(request);

        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.DRAFT);
        verify(auditService).recordCreate(proposal);
    }

    @Test
    void changeStatus_toAccepted_fillsDecidedAt() {
        Proposal proposal = newProposal(ProposalStatus.SENT);
        when(proposalRepository.findByIdAndDeletedAtIsNull(proposal.getId())).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(proposal)).thenReturn(proposal);

        proposalService.changeStatus(proposal.getId(), ProposalStatus.ACCEPTED);

        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.ACCEPTED);
        assertThat(proposal.getDecidedAt()).isNotNull();
        verify(auditService).recordUpdate(any(Proposal.class), any());
    }

    @Test
    void changeStatus_backToDraft_clearsDecidedAt() {
        Proposal proposal = newProposal(ProposalStatus.ACCEPTED);
        proposal.setDecidedAt(java.time.Instant.now());
        when(proposalRepository.findByIdAndDeletedAtIsNull(proposal.getId())).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(proposal)).thenReturn(proposal);

        proposalService.changeStatus(proposal.getId(), ProposalStatus.DRAFT);

        assertThat(proposal.getDecidedAt()).isNull();
    }

    @Test
    void changeStatus_withoutStatus_throwsBusinessException() {
        assertThatThrownBy(() -> proposalService.changeStatus(UUID.randomUUID(), null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void recalculateTotal_sumsAllItemTotals() {
        Proposal proposal = newProposal(ProposalStatus.DRAFT);
        when(proposalRepository.findByIdAndDeletedAtIsNull(proposal.getId())).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(proposal)).thenReturn(proposal);
        when(proposalItemRepository.findByProposal_IdAndDeletedAtIsNullOrderByDisplayOrderAsc(proposal.getId()))
                .thenReturn(List.of(
                        itemWithTotal("10.00"),
                        itemWithTotal("15.50")));

        proposalService.recalculateTotal(proposal.getId());

        assertThat(proposal.getTotalAmount()).isEqualByComparingTo("25.50");
    }

    @Test
    void delete_marksTheProposalAsDeletedAndAudits() {
        Proposal proposal = newProposal(ProposalStatus.DRAFT);
        when(proposalRepository.findByIdAndDeletedAtIsNull(proposal.getId())).thenReturn(Optional.of(proposal));

        proposalService.delete(proposal.getId());

        assertThat(proposal.getDeletedAt()).isNotNull();
        verify(auditService).recordDelete(proposal);
    }

    @Test
    void findById_withUnknownId_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(proposalRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proposalService.findById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    private com.primecrm.infra.entity.proposal.ProposalItem itemWithTotal(String total) {
        com.primecrm.infra.entity.proposal.ProposalItem item = new com.primecrm.infra.entity.proposal.ProposalItem();
        item.setQuantity(new java.math.BigDecimal("1"));
        item.setUnitPrice(new java.math.BigDecimal(total));
        return item;
    }

    private ProposalRequest request() {
        return new ProposalRequest(UUID.randomUUID(), null, null, null, null, null, null);
    }

    private Proposal newProposal(ProposalStatus status) {
        Proposal proposal = new Proposal();
        proposal.setId(UUID.randomUUID());
        proposal.setStatus(status);
        return proposal;
    }
}
