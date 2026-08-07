package com.primecrm.core.mapper;

import com.primecrm.core.dto.domain.DomainValueRequest;
import com.primecrm.core.dto.domain.DomainValueResponse;
import com.primecrm.infra.entity.domain.DomainValue;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface DomainValueMapper {

    @Mapping(target = "domainTypeCode", source = "domainType.code")
    @Mapping(target = "domainTypeLabel", source = "domainType.label")
    DomainValueResponse toResponse(DomainValue domainValue);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "domainType", ignore = true)
    @Mapping(target = "displayOrder", ignore = true)
    @Mapping(target = "active", ignore = true)
    DomainValue toEntity(DomainValueRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "domainType", ignore = true)
    @Mapping(target = "displayOrder", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(@MappingTarget DomainValue domainValue, DomainValueRequest request);
}
