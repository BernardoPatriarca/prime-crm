package com.primecrm.core.mapper;

import com.primecrm.core.dto.pipeline.PipelineRequest;
import com.primecrm.core.dto.pipeline.PipelineResponse;
import com.primecrm.core.dto.pipeline.PipelineStageResponse;
import com.primecrm.infra.entity.config.Pipeline;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface PipelineMapper {

    @Mapping(target = "stages", source = "stages")
    PipelineResponse toResponse(Pipeline pipeline, List<PipelineStageResponse> stages);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    Pipeline toEntity(PipelineRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(@MappingTarget Pipeline pipeline, PipelineRequest request);
}
