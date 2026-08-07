package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.pipeline.PipelineRequest;
import com.primecrm.core.dto.pipeline.PipelineResponse;
import com.primecrm.core.dto.pipeline.PipelineStageResponse;
import com.primecrm.core.mapper.PipelineMapper;
import com.primecrm.core.mapper.PipelineStageMapper;
import com.primecrm.infra.entity.config.Pipeline;
import com.primecrm.infra.entity.config.PipelineStage;
import com.primecrm.infra.repository.PipelineRepository;
import com.primecrm.infra.repository.PipelineStageRepository;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class PipelineServiceTest {

    @Mock
    private PipelineRepository pipelineRepository;
    @Mock
    private PipelineStageRepository pipelineStageRepository;
    @Mock
    private PipelineMapper pipelineMapper;
    @Mock
    private PipelineStageMapper pipelineStageMapper;
    @Mock
    private AuditService auditService;

    private PipelineService pipelineService;

    @BeforeEach
    void setUp() {
        pipelineService = new PipelineService(pipelineRepository, pipelineStageRepository, pipelineMapper,
                pipelineStageMapper, auditService);
    }

    @Test
    void create_withoutActiveFlag_defaultsToActiveAndAudits() {
        PipelineRequest request = new PipelineRequest("Funil Comercial", "SERVICES", null);

        Pipeline mapped = new Pipeline();
        Pipeline saved = new Pipeline();
        saved.setId(UUID.randomUUID());
        PipelineResponse expected = new PipelineResponse(saved.getId(), "Funil Comercial", "SERVICES", true, List.of());

        when(pipelineMapper.toEntity(request)).thenReturn(mapped);
        when(pipelineRepository.save(mapped)).thenReturn(saved);
        when(pipelineStageRepository.findByPipeline_IdInAndDeletedAtIsNullOrderByDisplayOrderAsc(anyCollection())).thenReturn(List.of());
        when(pipelineMapper.toResponse(eq(saved), anyList())).thenReturn(expected);

        assertThat(pipelineService.create(request)).isEqualTo(expected);
        assertThat(mapped.isActive()).isTrue();
        verify(auditService).recordCreate(saved);
    }

    @Test
    void create_withActiveFalse_keepsPipelineInactive() {
        PipelineRequest request = new PipelineRequest("Funil Inativo", "TRADE", false);

        Pipeline mapped = new Pipeline();
        Pipeline saved = new Pipeline();
        saved.setId(UUID.randomUUID());

        when(pipelineMapper.toEntity(request)).thenReturn(mapped);
        when(pipelineRepository.save(mapped)).thenReturn(saved);
        when(pipelineStageRepository.findByPipeline_IdInAndDeletedAtIsNullOrderByDisplayOrderAsc(anyCollection())).thenReturn(List.of());

        pipelineService.create(request);

        assertThat(mapped.isActive()).isFalse();
    }

    @Test
    void update_existingPipeline_recordsAuditWithPreviousState() {
        UUID id = UUID.randomUUID();
        PipelineRequest request = new PipelineRequest("Funil Renomeado", "INDUSTRY", false);

        Pipeline pipeline = new Pipeline();
        pipeline.setId(id);
        pipeline.setName("Funil Antigo");
        Map<String, Object> previousState = Map.of("name", "Funil Antigo");

        when(pipelineRepository.findById(id)).thenReturn(Optional.of(pipeline));
        when(auditService.snapshot(pipeline)).thenReturn(previousState);
        when(pipelineRepository.save(pipeline)).thenReturn(pipeline);
        when(pipelineStageRepository.findByPipeline_IdInAndDeletedAtIsNullOrderByDisplayOrderAsc(anyCollection())).thenReturn(List.of());

        pipelineService.update(id, request);

        assertThat(pipeline.isActive()).isFalse();
        verify(auditService).recordUpdate(pipeline, previousState);
    }

    @Test
    void update_unknownPipeline_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(pipelineRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pipelineService.update(id, new PipelineRequest("X", "TRADE", null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(pipelineRepository, never()).save(any(Pipeline.class));
    }

    @Test
    void delete_existingPipeline_softDeletesAndAudits() {
        UUID id = UUID.randomUUID();
        Pipeline pipeline = new Pipeline();
        pipeline.setId(id);

        when(pipelineRepository.findById(id)).thenReturn(Optional.of(pipeline));
        when(pipelineRepository.save(pipeline)).thenReturn(pipeline);

        pipelineService.delete(id);

        assertThat(pipeline.getDeletedAt()).isNotNull();
        verify(auditService).recordDelete(pipeline);
    }

    @Test
    void getActiveOrThrow_softDeletedPipeline_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        Pipeline deleted = new Pipeline();
        deleted.setId(id);
        deleted.setDeletedAt(Instant.now());

        when(pipelineRepository.findById(id)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> pipelineService.getActiveOrThrow(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findById_returnsStagesSortedByDisplayOrder() {
        UUID id = UUID.randomUUID();
        Pipeline pipeline = new Pipeline();
        pipeline.setId(id);

        PipelineStage second = new PipelineStage();
        second.setId(UUID.randomUUID());
        second.setDisplayOrder(2);
        second.setPipeline(pipeline);
        PipelineStage first = new PipelineStage();
        first.setId(UUID.randomUUID());
        first.setDisplayOrder(1);
        first.setPipeline(pipeline);

        PipelineStageResponse secondResponse = new PipelineStageResponse(second.getId(), id, "Proposta", 2,
                BigDecimal.ZERO, null, null, false);
        PipelineStageResponse firstResponse = new PipelineStageResponse(first.getId(), id, "Prospeccao", 1,
                BigDecimal.ZERO, null, null, false);

        when(pipelineRepository.findById(id)).thenReturn(Optional.of(pipeline));
        when(pipelineStageRepository.findByPipeline_IdInAndDeletedAtIsNullOrderByDisplayOrderAsc(anyCollection())).thenReturn(List.of(second, first));
        when(pipelineStageMapper.toResponse(second)).thenReturn(secondResponse);
        when(pipelineStageMapper.toResponse(first)).thenReturn(firstResponse);
        when(pipelineMapper.toResponse(eq(pipeline), anyList()))
                .thenAnswer(invocation -> new PipelineResponse(id, "Funil", "TRADE", true, invocation.getArgument(1)));

        PipelineResponse response = pipelineService.findById(id);

        assertThat(response.stages()).containsExactly(firstResponse, secondResponse);
    }
}
