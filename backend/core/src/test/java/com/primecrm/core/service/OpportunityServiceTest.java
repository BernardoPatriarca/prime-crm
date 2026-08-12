package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.commercial.OpportunityBoardResponse;
import com.primecrm.core.dto.commercial.OpportunityCardResponse;
import com.primecrm.core.dto.commercial.OpportunityResponse;
import com.primecrm.core.dto.commercial.OpportunityStageMoveRequest;
import com.primecrm.core.mapper.OpportunityMapper;
import com.primecrm.core.mapper.OpportunityStageHistoryMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.infra.entity.commercial.Opportunity;
import com.primecrm.infra.entity.commercial.OpportunityOutcome;
import com.primecrm.infra.entity.commercial.OpportunityStageHistory;
import com.primecrm.infra.entity.config.Pipeline;
import com.primecrm.infra.entity.config.PipelineStage;
import com.primecrm.infra.entity.domain.DomainValue;
import com.primecrm.infra.repository.OpportunityRepository;
import com.primecrm.infra.repository.OpportunityStageHistoryRepository;
import com.primecrm.infra.repository.PipelineStageRepository;
import com.primecrm.infra.repository.projection.StageAggregate;
import com.primecrm.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class OpportunityServiceTest {

    @Mock
    private OpportunityRepository opportunityRepository;
    @Mock
    private OpportunityStageHistoryRepository stageHistoryRepository;
    @Mock
    private PipelineStageRepository pipelineStageRepository;
    @Mock
    private OpportunityMapper opportunityMapper;
    @Mock
    private OpportunityStageHistoryMapper stageHistoryMapper;
    @Mock
    private CommercialReferenceResolver referenceResolver;
    @Mock
    private AuditService auditService;

    private OpportunityService opportunityService;

    private Pipeline pipeline;
    private PipelineStage qualification;
    private PipelineStage lostStage;
    private PipelineStage wonStage;

    @BeforeEach
    void setUp() {
        opportunityService = new OpportunityService(opportunityRepository, stageHistoryRepository,
                pipelineStageRepository, opportunityMapper, stageHistoryMapper, referenceResolver, auditService);

        pipeline = new Pipeline();
        pipeline.setId(UUID.randomUUID());
        pipeline.setName("Funil Comercial");

        qualification = newStage("Qualificacao", 1, new BigDecimal("30.00"), false);
        lostStage = newStage("Perdido", 9, new BigDecimal("0.00"), true);
        wonStage = newStage("Ganho", 8, new BigDecimal("100.00"), false);
    }

    @Test
    void moveStage_toStageRequiringLossReason_withoutReason_throwsBusinessException() {
        Opportunity opportunity = newOpportunity(qualification);

        when(opportunityRepository.findById(opportunity.getId())).thenReturn(Optional.of(opportunity));
        when(referenceResolver.stage(lostStage.getId())).thenReturn(lostStage);

        OpportunityStageMoveRequest request =
                new OpportunityStageMoveRequest(lostStage.getId(), null, null, null);

        assertThatThrownBy(() -> opportunityService.moveStage(opportunity.getId(), request, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("motivo de perda");

        assertThat(opportunity.getStage()).isSameAs(qualification);
        assertThat(opportunity.getOutcome()).isEqualTo(OpportunityOutcome.OPEN);
        verify(opportunityRepository, never()).save(any(Opportunity.class));
        verify(stageHistoryRepository, never()).save(any(OpportunityStageHistory.class));
    }

    @Test
    void moveStage_toStageRequiringLossReason_withReason_marksOpportunityAsLost() {
        Opportunity opportunity = newOpportunity(qualification);
        DomainValue lossReason = newDomainValue("Preco");

        when(opportunityRepository.findById(opportunity.getId())).thenReturn(Optional.of(opportunity));
        when(referenceResolver.stage(lostStage.getId())).thenReturn(lostStage);
        when(referenceResolver.domainValue(lossReason.getId(), "Motivo de perda")).thenReturn(lossReason);
        when(opportunityRepository.save(opportunity)).thenReturn(opportunity);
        when(opportunityMapper.toResponse(opportunity)).thenReturn(OpportunityResponse.builder().build());

        opportunityService.moveStage(opportunity.getId(),
                new OpportunityStageMoveRequest(lostStage.getId(), lossReason.getId(), null, "sem orcamento"),
                null);

        assertThat(opportunity.getOutcome()).isEqualTo(OpportunityOutcome.LOST);
        assertThat(opportunity.getStage()).isSameAs(lostStage);
        assertThat(opportunity.getLossReason()).isSameAs(lossReason);
        assertThat(opportunity.getClosedAt()).isNotNull();
        assertThat(opportunity.getProbability()).isEqualByComparingTo("0.00");
        verify(stageHistoryRepository).save(any(OpportunityStageHistory.class));
    }

    @Test
    void moveStage_toWonStage_withoutWinReason_throwsBusinessException() {
        Opportunity opportunity = newOpportunity(qualification);

        when(opportunityRepository.findById(opportunity.getId())).thenReturn(Optional.of(opportunity));
        when(referenceResolver.stage(wonStage.getId())).thenReturn(wonStage);

        assertThatThrownBy(() -> opportunityService.moveStage(opportunity.getId(),
                new OpportunityStageMoveRequest(wonStage.getId(), null, null, null), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("motivo de ganho");

        verify(opportunityRepository, never()).save(any(Opportunity.class));
    }

    @Test
    void moveStage_toWonStage_withWinReason_marksOpportunityAsWon() {
        Opportunity opportunity = newOpportunity(qualification);
        DomainValue winReason = newDomainValue("Melhor proposta");

        when(opportunityRepository.findById(opportunity.getId())).thenReturn(Optional.of(opportunity));
        when(referenceResolver.stage(wonStage.getId())).thenReturn(wonStage);
        when(referenceResolver.domainValue(winReason.getId(), "Motivo de ganho")).thenReturn(winReason);
        when(opportunityRepository.save(opportunity)).thenReturn(opportunity);
        when(opportunityMapper.toResponse(opportunity)).thenReturn(OpportunityResponse.builder().build());

        opportunityService.moveStage(opportunity.getId(),
                new OpportunityStageMoveRequest(wonStage.getId(), null, winReason.getId(), null), null);

        assertThat(opportunity.getOutcome()).isEqualTo(OpportunityOutcome.WON);
        assertThat(opportunity.getWinReason()).isSameAs(winReason);
        assertThat(opportunity.getClosedAt()).isNotNull();
        assertThat(opportunity.getProbability()).isEqualByComparingTo("100.00");
    }

    @Test
    void moveStage_recordsHistoryWithDaysSinceTheLastMove() {
        Opportunity opportunity = newOpportunity(qualification);
        PipelineStage proposal = newStage("Proposta", 2, new BigDecimal("60.00"), false);

        OpportunityStageHistory lastMove = new OpportunityStageHistory();
        lastMove.setMovedAt(Instant.now().minus(7, ChronoUnit.DAYS));

        when(opportunityRepository.findById(opportunity.getId())).thenReturn(Optional.of(opportunity));
        when(referenceResolver.stage(proposal.getId())).thenReturn(proposal);
        when(stageHistoryRepository.findFirstByOpportunity_IdAndDeletedAtIsNullOrderByMovedAtDesc(
                opportunity.getId())).thenReturn(lastMove);
        when(opportunityRepository.save(opportunity)).thenReturn(opportunity);
        when(opportunityMapper.toResponse(opportunity)).thenReturn(OpportunityResponse.builder().build());

        opportunityService.moveStage(opportunity.getId(),
                new OpportunityStageMoveRequest(proposal.getId(), null, null, "avancou"), null);

        ArgumentCaptor<OpportunityStageHistory> captor =
                ArgumentCaptor.forClass(OpportunityStageHistory.class);
        verify(stageHistoryRepository).save(captor.capture());
        OpportunityStageHistory recorded = captor.getValue();

        assertThat(recorded.getFromStage()).isSameAs(qualification);
        assertThat(recorded.getToStage()).isSameAs(proposal);
        assertThat(recorded.getDaysInPreviousStage()).isEqualTo(7);
        assertThat(recorded.getNote()).isEqualTo("avancou");
        assertThat(opportunity.getOutcome()).isEqualTo(OpportunityOutcome.OPEN);
        assertThat(opportunity.getProbability()).isEqualByComparingTo("60.00");
    }

    @Test
    void moveStage_withoutPreviousHistory_countsDaysFromTheOpeningDate() {
        Opportunity opportunity = newOpportunity(qualification);
        opportunity.setOpenedAt(Instant.now().minus(3, ChronoUnit.DAYS));
        PipelineStage proposal = newStage("Proposta", 2, new BigDecimal("60.00"), false);

        when(opportunityRepository.findById(opportunity.getId())).thenReturn(Optional.of(opportunity));
        when(referenceResolver.stage(proposal.getId())).thenReturn(proposal);
        when(opportunityRepository.save(opportunity)).thenReturn(opportunity);
        when(opportunityMapper.toResponse(opportunity)).thenReturn(OpportunityResponse.builder().build());

        opportunityService.moveStage(opportunity.getId(),
                new OpportunityStageMoveRequest(proposal.getId(), null, null, null), null);

        ArgumentCaptor<OpportunityStageHistory> captor =
                ArgumentCaptor.forClass(OpportunityStageHistory.class);
        verify(stageHistoryRepository).save(captor.capture());

        assertThat(captor.getValue().getFromStage()).isSameAs(qualification);
        assertThat(captor.getValue().getDaysInPreviousStage()).isEqualTo(3);
    }

    @Test
    void moveStage_toStageOfAnotherPipeline_throwsBusinessException() {
        Opportunity opportunity = newOpportunity(qualification);

        Pipeline otherPipeline = new Pipeline();
        otherPipeline.setId(UUID.randomUUID());
        PipelineStage foreignStage = new PipelineStage();
        foreignStage.setId(UUID.randomUUID());
        foreignStage.setPipeline(otherPipeline);
        foreignStage.setDefaultProbability(BigDecimal.ZERO);

        when(opportunityRepository.findById(opportunity.getId())).thenReturn(Optional.of(opportunity));
        when(referenceResolver.stage(foreignStage.getId())).thenReturn(foreignStage);

        assertThatThrownBy(() -> opportunityService.moveStage(opportunity.getId(),
                new OpportunityStageMoveRequest(foreignStage.getId(), null, null, null), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nao pertence ao funil");
    }

    @Test
    void moveStage_reopeningIntoAnOrdinaryStage_clearsOutcomeAndClosedAt() {
        Opportunity opportunity = newOpportunity(lostStage);
        opportunity.setOutcome(OpportunityOutcome.LOST);
        opportunity.setClosedAt(Instant.now());
        opportunity.setLossReason(newDomainValue("Preco"));

        when(opportunityRepository.findById(opportunity.getId())).thenReturn(Optional.of(opportunity));
        when(referenceResolver.stage(qualification.getId())).thenReturn(qualification);
        when(opportunityRepository.save(opportunity)).thenReturn(opportunity);
        when(opportunityMapper.toResponse(opportunity)).thenReturn(OpportunityResponse.builder().build());

        opportunityService.moveStage(opportunity.getId(),
                new OpportunityStageMoveRequest(qualification.getId(), null, null, null), null);

        assertThat(opportunity.getOutcome()).isEqualTo(OpportunityOutcome.OPEN);
        assertThat(opportunity.getClosedAt()).isNull();
        assertThat(opportunity.getLossReason()).isNull();
    }

    @Test
    void board_returnsStagesInOrderWithTotalsAndLimitedCards() {
        PipelineStage proposal = newStage("Proposta", 2, new BigDecimal("60.00"), false);
        Opportunity first = newOpportunity(qualification);

        when(referenceResolver.pipeline(pipeline.getId())).thenReturn(pipeline);
        when(pipelineStageRepository.findByPipeline_IdInAndDeletedAtIsNullOrderByDisplayOrderAsc(anyCollection()))
                .thenReturn(List.of(proposal, qualification));
        when(opportunityRepository.summarizeByPipelineAndOutcome(pipeline.getId(), OpportunityOutcome.OPEN))
                .thenReturn(List.of(new SimpleStageAggregate(qualification.getId(), 3L, new BigDecimal("1500.00"))));
        when(opportunityRepository
                .findByStage_IdAndOutcomeAndDeletedAtIsNullOrderByExpectedCloseDateAscOpenedAtAsc(
                        eq(qualification.getId()), eq(OpportunityOutcome.OPEN), any(Pageable.class)))
                .thenReturn(List.of(first));
        when(opportunityMapper.toCard(first)).thenReturn(new OpportunityCardResponse(first.getId(), "OPO-000001",
                "Negocio", new BigDecimal("500.00"), null, null, null, null, null));

        OpportunityBoardResponse board = opportunityService.board(pipeline.getId(), 2);

        assertThat(board.columns()).hasSize(2);
        assertThat(board.columns().get(0).stageName()).isEqualTo("Qualificacao");
        assertThat(board.columns().get(1).stageName()).isEqualTo("Proposta");

        var qualificationColumn = board.columns().get(0);
        assertThat(qualificationColumn.totalCount()).isEqualTo(3L);
        assertThat(qualificationColumn.totalAmount()).isEqualByComparingTo("1500.00");
        assertThat(qualificationColumn.opportunities()).hasSize(1);
        assertThat(qualificationColumn.hasMore()).isTrue();

        var proposalColumn = board.columns().get(1);
        assertThat(proposalColumn.totalCount()).isZero();
        assertThat(proposalColumn.opportunities()).isEmpty();
        assertThat(proposalColumn.hasMore()).isFalse();

        assertThat(board.limitPerStage()).isEqualTo(2);
        assertThat(board.totalCount()).isEqualTo(3L);
        assertThat(board.totalAmount()).isEqualByComparingTo("1500.00");
    }

    @Test
    void board_emptyStagesAreNotQueriedForCards() {
        when(referenceResolver.pipeline(pipeline.getId())).thenReturn(pipeline);
        when(pipelineStageRepository.findByPipeline_IdInAndDeletedAtIsNullOrderByDisplayOrderAsc(anyCollection()))
                .thenReturn(List.of(qualification));
        when(opportunityRepository.summarizeByPipelineAndOutcome(pipeline.getId(), OpportunityOutcome.OPEN))
                .thenReturn(List.of());

        opportunityService.board(pipeline.getId(), null);

        verify(opportunityRepository, never())
                .findByStage_IdAndOutcomeAndDeletedAtIsNullOrderByExpectedCloseDateAscOpenedAtAsc(
                        any(UUID.class), any(OpportunityOutcome.class), any(Pageable.class));
    }

    @Test
    void normalizeBoardLimit_appliesDefaultAndCeiling() {
        assertThat(opportunityService.normalizeBoardLimit(null))
                .isEqualTo(OpportunityService.DEFAULT_BOARD_LIMIT_PER_STAGE);
        assertThat(opportunityService.normalizeBoardLimit(0))
                .isEqualTo(OpportunityService.DEFAULT_BOARD_LIMIT_PER_STAGE);
        assertThat(opportunityService.normalizeBoardLimit(5)).isEqualTo(5);
        assertThat(opportunityService.normalizeBoardLimit(100_000))
                .isEqualTo(OpportunityService.MAX_BOARD_LIMIT_PER_STAGE);
    }

    private PipelineStage newStage(String name, int order, BigDecimal probability, boolean requiresLossReason) {
        PipelineStage stage = new PipelineStage();
        stage.setId(UUID.randomUUID());
        stage.setPipeline(pipeline);
        stage.setName(name);
        stage.setDisplayOrder(order);
        stage.setDefaultProbability(probability);
        stage.setRequiresLossReason(requiresLossReason);
        return stage;
    }

    private Opportunity newOpportunity(PipelineStage stage) {
        Opportunity opportunity = new Opportunity();
        opportunity.setId(UUID.randomUUID());
        opportunity.setTitle("Negocio de teste");
        opportunity.setPipeline(pipeline);
        opportunity.setStage(stage);
        opportunity.setAmount(new BigDecimal("500.00"));
        return opportunity;
    }

    private DomainValue newDomainValue(String name) {
        DomainValue value = new DomainValue();
        value.setId(UUID.randomUUID());
        value.setName(name);
        return value;
    }

    private static final class SimpleStageAggregate implements StageAggregate {

        private final UUID stageId;
        private final long opportunityCount;
        private final BigDecimal totalAmount;

        private SimpleStageAggregate(UUID stageId, long opportunityCount, BigDecimal totalAmount) {
            this.stageId = stageId;
            this.opportunityCount = opportunityCount;
            this.totalAmount = totalAmount;
        }

        @Override
        public UUID getStageId() {
            return stageId;
        }

        @Override
        public long getOpportunityCount() {
            return opportunityCount;
        }

        @Override
        public BigDecimal getTotalAmount() {
            return totalAmount;
        }
    }
}
