package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.pipeline.PipelineStageRequest;
import com.primecrm.core.mapper.PipelineStageMapper;
import com.primecrm.infra.entity.config.Pipeline;
import com.primecrm.infra.entity.config.PipelineStage;
import com.primecrm.infra.repository.PipelineStageRepository;
import com.primecrm.shared.exception.BusinessException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class PipelineStageServiceTest {

    @Mock
    private PipelineStageRepository pipelineStageRepository;
    @Mock
    private PipelineStageMapper pipelineStageMapper;
    @Mock
    private PipelineService pipelineService;
    @Mock
    private AuditService auditService;

    private PipelineStageService pipelineStageService;

    @BeforeEach
    void setUp() {
        pipelineStageService = new PipelineStageService(pipelineStageRepository, pipelineStageMapper, pipelineService,
                auditService);
    }

    @Test
    void delete_lastStageInPipeline_throwsBusinessException() {
        UUID pipelineId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();

        Pipeline pipeline = new Pipeline();
        pipeline.setId(pipelineId);

        PipelineStage stage = new PipelineStage();
        stage.setId(stageId);
        stage.setPipeline(pipeline);

        when(pipelineStageRepository.findById(stageId)).thenReturn(Optional.of(stage));
        when(pipelineStageRepository.count(any(Specification.class))).thenReturn(1L);

        assertThatThrownBy(() -> pipelineStageService.delete(pipelineId, stageId))
                .isInstanceOf(BusinessException.class);

        verify(pipelineStageRepository, never()).save(any(PipelineStage.class));
    }

    @Test
    void delete_notLastStage_softDeletesStage() {
        UUID pipelineId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();

        Pipeline pipeline = new Pipeline();
        pipeline.setId(pipelineId);

        PipelineStage stage = new PipelineStage();
        stage.setId(stageId);
        stage.setPipeline(pipeline);

        when(pipelineStageRepository.findById(stageId)).thenReturn(Optional.of(stage));
        when(pipelineStageRepository.count(any(Specification.class))).thenReturn(2L);
        when(pipelineStageRepository.save(stage)).thenReturn(stage);

        pipelineStageService.delete(pipelineId, stageId);

        assertThat(stage.getDeletedAt()).isNotNull();
        verify(pipelineStageRepository).save(stage);
    }

    @Test
    void delete_stageFromDifferentPipeline_throwsResourceNotFound() {
        UUID pipelineId = UUID.randomUUID();
        UUID otherPipelineId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();

        Pipeline otherPipeline = new Pipeline();
        otherPipeline.setId(otherPipelineId);

        PipelineStage stage = new PipelineStage();
        stage.setId(stageId);
        stage.setPipeline(otherPipeline);

        when(pipelineStageRepository.findById(stageId)).thenReturn(Optional.of(stage));

        assertThatThrownBy(() -> pipelineStageService.delete(pipelineId, stageId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(pipelineStageRepository, never()).save(any(PipelineStage.class));
    }

    @Test
    void create_withUnknownPipeline_throwsResourceNotFound() {
        UUID pipelineId = UUID.randomUUID();
        PipelineStageRequest request = new PipelineStageRequest("Prospeccao", null, null, null, null, null);

        when(pipelineService.getActiveOrThrow(pipelineId))
                .thenThrow(new ResourceNotFoundException("Pipeline", pipelineId));

        assertThatThrownBy(() -> pipelineStageService.create(pipelineId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(pipelineStageRepository, never()).save(any(PipelineStage.class));
    }
}
