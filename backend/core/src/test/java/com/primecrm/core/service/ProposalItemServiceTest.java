package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.proposal.ProposalItemRequest;
import com.primecrm.core.mapper.ProposalItemMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.infra.entity.product.Product;
import com.primecrm.infra.entity.proposal.Proposal;
import com.primecrm.infra.entity.proposal.ProposalItem;
import com.primecrm.infra.repository.ProposalItemRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProposalItemServiceTest {

    @Mock
    private ProposalItemRepository proposalItemRepository;
    @Mock
    private ProposalItemMapper proposalItemMapper;
    @Mock
    private ProposalService proposalService;
    @Mock
    private CommercialReferenceResolver referenceResolver;
    @Mock
    private AuditService auditService;

    private ProposalItemService proposalItemService;

    @BeforeEach
    void setUp() {
        proposalItemService = new ProposalItemService(proposalItemRepository, proposalItemMapper, proposalService,
                referenceResolver, auditService);
    }

    @Test
    void create_withoutUnitPrice_snapshotsTheProductPrice() {
        UUID proposalId = UUID.randomUUID();
        Proposal proposal = new Proposal();
        proposal.setId(proposalId);
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setUnitPrice(new BigDecimal("99.90"));

        ProposalItemRequest request = new ProposalItemRequest(product.getId(), null, new BigDecimal("2"), null, null,
                null);
        ProposalItem item = new ProposalItem();

        when(proposalService.getActiveOrThrow(proposalId)).thenReturn(proposal);
        when(referenceResolver.product(product.getId())).thenReturn(product);
        when(proposalItemMapper.toEntity(request)).thenReturn(item);
        when(proposalItemRepository.save(item)).thenReturn(item);
        when(proposalItemRepository.countByProposal_IdAndDeletedAtIsNull(proposalId)).thenReturn(0L);

        proposalItemService.create(proposalId, request);

        assertThat(item.getUnitPrice()).isEqualByComparingTo("99.90");
        assertThat(item.getDiscountPercent()).isEqualByComparingTo("0");
        verify(auditService).recordCreate(item);
        verify(proposalService).recalculateTotal(proposalId);
    }

    @Test
    void create_withExplicitUnitPrice_keepsTheOverride() {
        UUID proposalId = UUID.randomUUID();
        Proposal proposal = new Proposal();
        proposal.setId(proposalId);
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setUnitPrice(new BigDecimal("99.90"));

        ProposalItemRequest request = new ProposalItemRequest(product.getId(), null, new BigDecimal("2"),
                new BigDecimal("80.00"), new BigDecimal("10"), 3);
        ProposalItem item = new ProposalItem();

        when(proposalService.getActiveOrThrow(proposalId)).thenReturn(proposal);
        when(referenceResolver.product(product.getId())).thenReturn(product);
        when(proposalItemMapper.toEntity(request)).thenReturn(item);
        when(proposalItemRepository.save(item)).thenReturn(item);

        proposalItemService.create(proposalId, request);

        assertThat(item.getUnitPrice()).isEqualByComparingTo("80.00");
        assertThat(item.getDiscountPercent()).isEqualByComparingTo("10");
        assertThat(item.getDisplayOrder()).isEqualTo(3);
    }

    @Test
    void delete_marksTheItemAsDeletedAndRecalculatesTotal() {
        UUID proposalId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        ProposalItem item = new ProposalItem();
        item.setId(itemId);
        Product product = new Product();
        item.setProduct(product);

        when(proposalItemRepository.findByIdAndProposal_IdAndDeletedAtIsNull(itemId, proposalId))
                .thenReturn(Optional.of(item));

        proposalItemService.delete(proposalId, itemId);

        assertThat(item.getDeletedAt()).isNotNull();
        verify(auditService).recordDelete(item);
        verify(proposalService).recalculateTotal(proposalId);
    }
}
