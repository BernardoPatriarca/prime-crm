package com.primecrm.core.service;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.pipeline.PipelineRequest;
import com.primecrm.core.dto.pipeline.PipelineResponse;
import com.primecrm.core.dto.pipeline.PipelineStageResponse;
import com.primecrm.core.mapper.PipelineMapper;
import com.primecrm.core.mapper.PipelineStageMapper;
import com.primecrm.core.specification.PipelineSpecifications;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.infra.entity.config.Pipeline;
import com.primecrm.infra.entity.config.PipelineStage;
import com.primecrm.infra.repository.PipelineRepository;
import com.primecrm.infra.repository.PipelineStageRepository;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
public class PipelineService {

    private final PipelineRepository pipelineRepository;
    private final PipelineStageRepository pipelineStageRepository;
    private final PipelineMapper pipelineMapper;
    private final PipelineStageMapper pipelineStageMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<PipelineResponse> list(String search, Boolean active, Pageable pageable) {
        var spec = SpecificationUtils.<Pipeline>and(
                PipelineSpecifications.notDeleted(),
                PipelineSpecifications.textSearch(search),
                PipelineSpecifications.hasActive(active)
        );
        Page<Pipeline> page = pipelineRepository.findAll(spec, pageable);
        Map<UUID, List<PipelineStageResponse>> stagesByPipeline = loadStagesByPipeline(page.getContent());
        return page.map(pipeline -> pipelineMapper.toResponse(pipeline,
                stagesByPipeline.getOrDefault(pipeline.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public PipelineResponse findById(UUID id) {
        return toResponse(getActiveOrThrow(id));
    }

    @Transactional
    public PipelineResponse create(PipelineRequest request) {
        Pipeline pipeline = pipelineMapper.toEntity(request);
        pipeline.setActive(request.active() == null || request.active());
        pipeline = pipelineRepository.save(pipeline);
        auditService.recordCreate(pipeline);
        return toResponse(pipeline);
    }

    @Transactional
    public PipelineResponse update(UUID id, PipelineRequest request) {
        Pipeline pipeline = getActiveOrThrow(id);
        Map<String, Object> previousState = auditService.snapshot(pipeline);
        pipelineMapper.updateEntity(pipeline, request);
        if (request.active() != null) {
            pipeline.setActive(request.active());
        }
        pipeline = pipelineRepository.save(pipeline);
        auditService.recordUpdate(pipeline, previousState);
        return toResponse(pipeline);
    }

    @Transactional
    public void delete(UUID id) {
        Pipeline pipeline = getActiveOrThrow(id);
        pipeline.setDeletedAt(Instant.now());
        pipelineRepository.save(pipeline);
        auditService.recordDelete(pipeline);
    }

    Pipeline getActiveOrThrow(UUID id) {
        return pipelineRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline", id));
    }

    private PipelineResponse toResponse(Pipeline pipeline) {
        return pipelineMapper.toResponse(pipeline,
                loadStagesByPipeline(List.of(pipeline)).getOrDefault(pipeline.getId(), List.of()));
    }

    private Map<UUID, List<PipelineStageResponse>> loadStagesByPipeline(List<Pipeline> pipelines) {
        if (pipelines.isEmpty()) {
            return Map.of();
        }
        List<UUID> pipelineIds = pipelines.stream().map(Pipeline::getId).toList();
        return pipelineStageRepository.findByPipeline_IdInAndDeletedAtIsNullOrderByDisplayOrderAsc(pipelineIds)
                .stream()
                .sorted(Comparator.comparingInt(PipelineStage::getDisplayOrder))
                .collect(Collectors.groupingBy(stage -> stage.getPipeline().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(pipelineStageMapper::toResponse, Collectors.toList())));
    }
}
