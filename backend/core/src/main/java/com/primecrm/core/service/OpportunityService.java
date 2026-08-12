package com.primecrm.core.service;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.commercial.OpportunityBoardColumnResponse;
import com.primecrm.core.dto.commercial.OpportunityBoardResponse;
import com.primecrm.core.dto.commercial.OpportunityCardResponse;
import com.primecrm.core.dto.commercial.OpportunityListFilter;
import com.primecrm.core.dto.commercial.OpportunityRequest;
import com.primecrm.core.dto.commercial.OpportunityResponse;
import com.primecrm.core.dto.commercial.OpportunityStageHistoryResponse;
import com.primecrm.core.dto.commercial.OpportunityStageMoveRequest;
import com.primecrm.core.mapper.OpportunityMapper;
import com.primecrm.core.mapper.OpportunityStageHistoryMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.core.specification.OpportunitySpecifications;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.infra.entity.commercial.Opportunity;
import com.primecrm.infra.entity.commercial.OpportunityOutcome;
import com.primecrm.infra.entity.commercial.OpportunityStageHistory;
import com.primecrm.infra.entity.config.Pipeline;
import com.primecrm.infra.entity.config.PipelineStage;
import com.primecrm.infra.repository.OpportunityRepository;
import com.primecrm.infra.repository.OpportunityStageHistoryRepository;
import com.primecrm.infra.repository.PipelineStageRepository;
import com.primecrm.infra.repository.projection.StageAggregate;
import com.primecrm.shared.exception.BusinessException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OpportunityService {

    public static final int DEFAULT_BOARD_LIMIT_PER_STAGE = 25;
    public static final int MAX_BOARD_LIMIT_PER_STAGE = 100;

    private static final BigDecimal WON_PROBABILITY = new BigDecimal("100.00");

    private final OpportunityRepository opportunityRepository;
    private final OpportunityStageHistoryRepository stageHistoryRepository;
    private final PipelineStageRepository pipelineStageRepository;
    private final OpportunityMapper opportunityMapper;
    private final OpportunityStageHistoryMapper stageHistoryMapper;
    private final CommercialReferenceResolver referenceResolver;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<OpportunityResponse> list(OpportunityListFilter filter, Pageable pageable) {
        var spec = SpecificationUtils.<Opportunity>and(
                OpportunitySpecifications.notDeleted(),
                OpportunitySpecifications.withReferencesFetched(),
                OpportunitySpecifications.textSearch(filter.search()),
                OpportunitySpecifications.hasPipeline(filter.pipelineId()),
                OpportunitySpecifications.hasStage(filter.stageId()),
                OpportunitySpecifications.hasCustomer(filter.customerId()),
                OpportunitySpecifications.hasOwner(filter.ownerUserId()),
                OpportunitySpecifications.hasOutcome(filter.outcome()),
                OpportunitySpecifications.expectedCloseFrom(filter.expectedCloseFrom()),
                OpportunitySpecifications.expectedCloseTo(filter.expectedCloseTo()),
                OpportunitySpecifications.amountFrom(filter.amountFrom()),
                OpportunitySpecifications.amountTo(filter.amountTo())
        );
        return opportunityRepository.findAll(spec, pageable).map(opportunityMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public OpportunityResponse findById(UUID id) {
        return opportunityMapper.toResponse(getActiveOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<OpportunityStageHistoryResponse> findHistory(UUID id) {
        getActiveOrThrow(id);
        return stageHistoryRepository.findByOpportunity_IdAndDeletedAtIsNullOrderByMovedAtAsc(id).stream()
                .map(stageHistoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OpportunityBoardResponse board(UUID pipelineId, Integer limitPerStage) {
        Pipeline pipeline = referenceResolver.pipeline(pipelineId);
        if (pipeline == null) {
            throw new ResourceNotFoundException("Funil", pipelineId);
        }
        int limit = normalizeBoardLimit(limitPerStage);

        List<PipelineStage> stages = pipelineStageRepository
                .findByPipeline_IdInAndDeletedAtIsNullOrderByDisplayOrderAsc(List.of(pipelineId)).stream()
                .sorted(Comparator.comparingInt(PipelineStage::getDisplayOrder))
                .toList();

        Map<UUID, StageAggregate> aggregates = opportunityRepository
                .summarizeByPipelineAndOutcome(pipelineId, OpportunityOutcome.OPEN).stream()
                .collect(Collectors.toMap(StageAggregate::getStageId, Function.identity(), (a, b) -> a,
                        HashMap::new));

        List<OpportunityBoardColumnResponse> columns = new ArrayList<>(stages.size());
        long boardCount = 0;
        BigDecimal boardAmount = BigDecimal.ZERO;

        for (PipelineStage stage : stages) {
            StageAggregate aggregate = aggregates.get(stage.getId());
            long totalCount = aggregate == null ? 0L : aggregate.getOpportunityCount();
            BigDecimal totalAmount = aggregate == null || aggregate.getTotalAmount() == null
                    ? BigDecimal.ZERO
                    : aggregate.getTotalAmount();

            List<OpportunityCardResponse> cards = totalCount == 0
                    ? List.of()
                    : opportunityRepository
                            .findByStage_IdAndOutcomeAndDeletedAtIsNullOrderByExpectedCloseDateAscOpenedAtAsc(
                                    stage.getId(), OpportunityOutcome.OPEN, PageRequest.of(0, limit))
                            .stream()
                            .map(opportunityMapper::toCard)
                            .toList();

            columns.add(new OpportunityBoardColumnResponse(
                    stage.getId(),
                    stage.getName(),
                    stage.getDisplayOrder(),
                    stage.getDefaultProbability(),
                    stage.getColor(),
                    stage.isRequiresLossReason(),
                    totalCount,
                    totalAmount,
                    totalCount > cards.size(),
                    cards));

            boardCount += totalCount;
            boardAmount = boardAmount.add(totalAmount);
        }

        return new OpportunityBoardResponse(pipeline.getId(), pipeline.getName(), limit, boardCount, boardAmount,
                columns);
    }

    @Transactional
    public OpportunityResponse create(OpportunityRequest request) {
        Opportunity opportunity = opportunityMapper.toEntity(request);
        Pipeline pipeline = referenceResolver.pipeline(request.pipelineId());
        PipelineStage stage = request.stageId() == null
                ? referenceResolver.firstStageOf(request.pipelineId())
                : referenceResolver.stage(request.stageId());
        ensureStageBelongsToPipeline(stage, pipeline);

        opportunity.setCustomer(referenceResolver.customer(request.customerId()));
        opportunity.setContact(referenceResolver.contact(request.contactId()));
        opportunity.setPipeline(pipeline);
        opportunity.setStage(stage);
        opportunity.setOwner(referenceResolver.user(request.ownerUserId()));
        opportunity.setTeam(referenceResolver.domainValue(request.teamId(), "Equipe"));
        opportunity.setSourceLead(referenceResolver.lead(request.sourceLeadId()));
        opportunity.setProbability(
                request.probability() == null ? stage.getDefaultProbability() : request.probability());
        opportunity.setOpenedAt(Instant.now());
        opportunity.setOutcome(OpportunityOutcome.OPEN);

        opportunity = opportunityRepository.save(opportunity);
        recordStageHistory(opportunity, null, stage, null, Instant.now(), null, "Criacao da oportunidade");
        auditService.recordCreate(opportunity);
        return opportunityMapper.toResponse(opportunity);
    }

    @Transactional
    public OpportunityResponse update(UUID id, OpportunityRequest request) {
        Opportunity opportunity = getActiveOrThrow(id);
        Pipeline pipeline = referenceResolver.pipeline(request.pipelineId());
        PipelineStage stage = request.stageId() == null
                ? opportunity.getStage()
                : referenceResolver.stage(request.stageId());
        ensureStageBelongsToPipeline(stage, pipeline);

        Map<String, Object> previousState = auditService.snapshot(opportunity);
        opportunityMapper.updateEntity(opportunity, request);
        opportunity.setCustomer(referenceResolver.customer(request.customerId()));
        opportunity.setContact(referenceResolver.contact(request.contactId()));
        opportunity.setPipeline(pipeline);
        opportunity.setStage(stage);
        opportunity.setOwner(referenceResolver.user(request.ownerUserId()));
        opportunity.setTeam(referenceResolver.domainValue(request.teamId(), "Equipe"));
        opportunity.setSourceLead(referenceResolver.lead(request.sourceLeadId()));
        if (request.probability() != null) {
            opportunity.setProbability(request.probability());
        }

        opportunity = opportunityRepository.save(opportunity);
        auditService.recordUpdate(opportunity, previousState);
        return opportunityMapper.toResponse(opportunity);
    }

    @Transactional
    public void delete(UUID id) {
        Opportunity opportunity = getActiveOrThrow(id);
        opportunity.setDeletedAt(Instant.now());
        opportunityRepository.save(opportunity);
        auditService.recordDelete(opportunity);
    }

    @Transactional
    public OpportunityResponse moveStage(UUID id, OpportunityStageMoveRequest request, UUID currentUserId) {
        Opportunity opportunity = getActiveOrThrow(id);
        PipelineStage target = referenceResolver.stage(request.stageId());
        ensureStageBelongsToPipeline(target, opportunity.getPipeline());

        PipelineStage previousStage = opportunity.getStage();
        Instant movedAt = Instant.now();
        Integer daysInPreviousStage = daysSinceLastMove(opportunity, movedAt);

        Map<String, Object> previousState = auditService.snapshot(opportunity);

        if (target.isRequiresLossReason()) {
            if (request.lossReasonId() == null) {
                throw new BusinessException("LOSS_REASON_REQUIRED",
                        "A etapa de destino exige o motivo de perda.");
            }
            opportunity.setLossReason(referenceResolver.domainValue(request.lossReasonId(), "Motivo de perda"));
            opportunity.setWinReason(null);
            opportunity.setOutcome(OpportunityOutcome.LOST);
            opportunity.setClosedAt(movedAt);
        } else if (isWonStage(target)) {
            if (request.winReasonId() == null) {
                throw new BusinessException("WIN_REASON_REQUIRED",
                        "A etapa de destino exige o motivo de ganho.");
            }
            opportunity.setWinReason(referenceResolver.domainValue(request.winReasonId(), "Motivo de ganho"));
            opportunity.setLossReason(null);
            opportunity.setOutcome(OpportunityOutcome.WON);
            opportunity.setClosedAt(movedAt);
        } else {
            opportunity.setWinReason(null);
            opportunity.setLossReason(null);
            opportunity.setOutcome(OpportunityOutcome.OPEN);
            opportunity.setClosedAt(null);
        }

        opportunity.setStage(target);
        opportunity.setProbability(target.getDefaultProbability());
        opportunity = opportunityRepository.save(opportunity);

        recordStageHistory(opportunity, previousStage, target, currentUserId, movedAt, daysInPreviousStage,
                request.note());

        auditService.recordUpdate(opportunity, previousState);

        return opportunityMapper.toResponse(opportunity);
    }

    public int normalizeBoardLimit(Integer limitPerStage) {
        if (limitPerStage == null || limitPerStage <= 0) {
            return DEFAULT_BOARD_LIMIT_PER_STAGE;
        }
        return Math.min(limitPerStage, MAX_BOARD_LIMIT_PER_STAGE);
    }

    Integer daysSinceLastMove(Opportunity opportunity, Instant movedAt) {
        OpportunityStageHistory last = stageHistoryRepository
                .findFirstByOpportunity_IdAndDeletedAtIsNullOrderByMovedAtDesc(opportunity.getId());
        Instant reference = last == null ? opportunity.getOpenedAt() : last.getMovedAt();
        if (reference == null) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(reference, movedAt);
    }

    private boolean isWonStage(PipelineStage stage) {
        return stage.getDefaultProbability() != null
                && stage.getDefaultProbability().compareTo(WON_PROBABILITY) >= 0;
    }

    private void recordStageHistory(Opportunity opportunity, PipelineStage from, PipelineStage to,
            UUID movedByUserId, Instant movedAt, Integer daysInPreviousStage, String note) {
        OpportunityStageHistory history = new OpportunityStageHistory();
        history.setOpportunity(opportunity);
        history.setFromStage(from);
        history.setToStage(to);
        history.setMovedByUser(referenceResolver.user(movedByUserId));
        history.setMovedAt(movedAt);
        history.setDaysInPreviousStage(daysInPreviousStage);
        history.setNote(note);
        stageHistoryRepository.save(history);
    }

    private void ensureStageBelongsToPipeline(PipelineStage stage, Pipeline pipeline) {
        if (stage == null || pipeline == null) {
            return;
        }
        if (!stage.getPipeline().getId().equals(pipeline.getId())) {
            throw new BusinessException("STAGE_PIPELINE_MISMATCH",
                    "A etapa informada nao pertence ao funil da oportunidade.");
        }
    }

    private Opportunity getActiveOrThrow(UUID id) {
        return opportunityRepository.findById(id)
                .filter(opportunity -> !opportunity.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Oportunidade", id));
    }
}
