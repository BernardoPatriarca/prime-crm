package com.primecrm.core.mapper;

import com.primecrm.core.dto.pipeline.PipelineStageRequest;
import com.primecrm.core.dto.pipeline.PipelineStageResponse;
import com.primecrm.infra.entity.config.PipelineStage;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface PipelineStageMapper {

    @Mapping(target = "pipelineId", source = "pipeline.id")
    PipelineStageResponse toResponse(PipelineStage stage);

    List<PipelineStageResponse> toResponseList(List<PipelineStage> stages);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pipeline", ignore = true)
    @Mapping(target = "displayOrder", ignore = true)
    @Mapping(target = "defaultProbability", ignore = true)
    @Mapping(target = "requiresLossReason", ignore = true)
    PipelineStage toEntity(PipelineStageRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pipeline", ignore = true)
    @Mapping(target = "displayOrder", ignore = true)
    @Mapping(target = "defaultProbability", ignore = true)
    @Mapping(target = "requiresLossReason", ignore = true)
    void updateEntity(@MappingTarget PipelineStage stage, PipelineStageRequest request);
}
