package com.primecrm.core.mapper;

import com.primecrm.core.dto.template.TemplateRequest;
import com.primecrm.core.dto.template.TemplateResponse;
import com.primecrm.infra.entity.config.Template;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface TemplateMapper {

    TemplateResponse toResponse(Template template);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    Template toEntity(TemplateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(@MappingTarget Template template, TemplateRequest request);
}
