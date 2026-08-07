package com.primecrm.core.mapper;

import com.primecrm.core.dto.customfield.CustomFieldRequest;
import com.primecrm.core.dto.customfield.CustomFieldResponse;
import com.primecrm.infra.entity.config.CustomField;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface CustomFieldMapper {

    CustomFieldResponse toResponse(CustomField customField);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "required", ignore = true)
    @Mapping(target = "displayOrder", ignore = true)
    @Mapping(target = "active", ignore = true)
    CustomField toEntity(CustomFieldRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "required", ignore = true)
    @Mapping(target = "displayOrder", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(@MappingTarget CustomField customField, CustomFieldRequest request);
}
