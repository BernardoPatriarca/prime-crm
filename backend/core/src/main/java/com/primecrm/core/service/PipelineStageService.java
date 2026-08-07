package com.primecrm.core.service;

import com.primecrm.core.audit.AuditChanges;
import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.common.ReorderItem;
import com.primecrm.core.dto.common.ReorderRequest;
import com.primecrm.core.dto.pipeline.PipelineStageRequest;
import com.primecrm.core.dto.pipeline.PipelineStageResponse;
import com.primecrm.core.mapper.PipelineStageMapper;
import com.primecrm.core.specification.PipelineStageSpecifications;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.infra.entity.audit.AuditAction;
import com.primecrm.infra.entity.config.Pipeline;
import com.primecrm.infra.entity.config.PipelineStage;
import com.primecrm.infra.repository.PipelineStageRepository;
import com.primecrm.shared.exception.BusinessException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PipelineStageService {

    private static final String AUDIT_ENTITY = "PipelineStage";

    private final PipelineStageRepository pipelineStageRepository;
    private final PipelineStageMapper pipelineStageMapper;
    private final PipelineService pipelineService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<PipelineStageResponse> list(UUID pipelineId, Pageable pageable) {
        pipelineService.getActiveOrThrow(pipelineId);
        var spec = SpecificationUtils.<PipelineStage>and(
                PipelineStageSpecifications.notDeleted(),
                PipelineStageSpecifications.byPipelineId(pipelineId)
        );
        return pipelineStageRepository.findAll(spec, pageable).map(pipelineStageMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PipelineStageResponse findById(UUID pipelineId, UUID stageId) {
        return pipelineStageMapper.toResponse(getActiveOrThrow(pipelineId, stageId));
    }

    @Transactional
    public PipelineStageResponse create(UUID pipelineId, PipelineStageRequest request) {
        Pipeline pipeline = pipelineService.getActiveOrThrow(pipelineId);

        PipelineStage stage = pipelineStageMapper.toEntity(request);
        stage.setPipeline(pipeline);
        stage.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : nextDisplayOrder(pipelineId));
        stage.setDefaultProbability(request.defaultProbability() != null ? request.defaultProbability() : BigDecimal.ZERO);
        stage.setRequiresLossReason(request.requiresLossReason() != null && request.requiresLossReason());

        stage = pipelineStageRepository.save(stage);
        auditService.recordCreate(stage);
        return pipelineStageMapper.toResponse(stage);
    }

    @Transactional
    public PipelineStageResponse update(UUID pipelineId, UUID stageId, PipelineStageRequest request) {
        PipelineStage stage = getActiveOrThrow(pipelineId, stageId);

        Map<String, Object> previousState = auditService.snapshot(stage);
        pipelineStageMapper.updateEntity(stage, request);
        if (request.displayOrder() != null) {
            stage.setDisplayOrder(request.displayOrder());
        }
        if (request.defaultProbability() != null) {
            stage.setDefaultProbability(request.defaultProbability());
        }
        if (request.requiresLossReason() != null) {
            stage.setRequiresLossReason(request.requiresLossReason());
        }

        stage = pipelineStageRepository.save(stage);
        auditService.recordUpdate(stage, previousState);
        return pipelineStageMapper.toResponse(stage);
    }

    @Transactional
    public void delete(UUID pipelineId, UUID stageId) {
        PipelineStage stage = getActiveOrThrow(pipelineId, stageId);

        long remainingStages = pipelineStageRepository.count(SpecificationUtils.<PipelineStage>and(
                PipelineStageSpecifications.notDeleted(),
                PipelineStageSpecifications.byPipelineId(pipelineId)
        ));
        if (remainingStages <= 1) {
            throw new BusinessException("LAST_STAGE_FORBIDDEN",
                    "Nao e possivel excluir a unica etapa restante do funil. Todo funil deve manter pelo menos uma etapa.");
        }

        stage.setDeletedAt(Instant.now());
        pipelineStageRepository.save(stage);
        auditService.recordDelete(stage);
    }

    @Transactional
    public List<PipelineStageResponse> reorder(UUID pipelineId, ReorderRequest request) {
        pipelineService.getActiveOrThrow(pipelineId);

        Map<UUID, Integer> desiredOrder = request.items().stream()
                .collect(Collectors.toMap(ReorderItem::id, ReorderItem::displayOrder));

        List<PipelineStage> stages = pipelineStageRepository.findAll(SpecificationUtils.<PipelineStage>and(
                PipelineStageSpecifications.notDeleted(),
                PipelineStageSpecifications.byPipelineId(pipelineId)
        )).stream().filter(stage -> desiredOrder.containsKey(stage.getId())).toList();

        if (stages.size() != desiredOrder.size()) {
            throw new ResourceNotFoundException("Uma ou mais etapas informadas nao pertencem a este funil");
        }

        Map<UUID, Integer> previousOrder = stages.stream()
                .collect(Collectors.toMap(PipelineStage::getId, PipelineStage::getDisplayOrder));

        stages.forEach(stage -> stage.setDisplayOrder(desiredOrder.get(stage.getId())));
        List<PipelineStage> saved = pipelineStageRepository.saveAll(stages);

        saved.forEach(stage -> auditService.recordChange(AuditAction.UPDATE, AUDIT_ENTITY, stage.getId(),
                Map.of("displayOrder", AuditChanges.of(previousOrder.get(stage.getId()), stage.getDisplayOrder()))));

        return saved.stream()
                .sorted(Comparator.comparingInt(PipelineStage::getDisplayOrder))
                .map(pipelineStageMapper::toResponse)
                .toList();
    }

    private int nextDisplayOrder(UUID pipelineId) {
        return (int) pipelineStageRepository.count(SpecificationUtils.<PipelineStage>and(
                PipelineStageSpecifications.notDeleted(),
                PipelineStageSpecifications.byPipelineId(pipelineId)
        ));
    }

    private PipelineStage getActiveOrThrow(UUID pipelineId, UUID stageId) {
        PipelineStage stage = pipelineStageRepository.findById(stageId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Etapa de funil", stageId));
        if (!stage.getPipeline().getId().equals(pipelineId)) {
            throw new ResourceNotFoundException("Etapa de funil", stageId);
        }
        return stage;
    }
}
