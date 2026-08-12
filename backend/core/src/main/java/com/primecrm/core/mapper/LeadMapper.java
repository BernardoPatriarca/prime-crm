package com.primecrm.core.mapper;

import com.primecrm.core.dto.commercial.LeadRequest;
import com.primecrm.core.dto.commercial.LeadResponse;
import com.primecrm.core.dto.common.DomainValueSummaryResponse;
import com.primecrm.infra.entity.commercial.Lead;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = CommercialSummaryMapper.class)
public interface LeadMapper {

    @Mapping(target = "tags", source = "tags")
    LeadResponse toResponse(Lead lead, List<DomainValueSummaryResponse> tags);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "origin", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "priority", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "pipeline", ignore = true)
    @Mapping(target = "stage", ignore = true)
    @Mapping(target = "convertedCustomer", ignore = true)
    @Mapping(target = "convertedAt", ignore = true)
    Lead toEntity(LeadRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "origin", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "priority", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "pipeline", ignore = true)
    @Mapping(target = "stage", ignore = true)
    @Mapping(target = "convertedCustomer", ignore = true)
    @Mapping(target = "convertedAt", ignore = true)
    void updateEntity(@MappingTarget Lead lead, LeadRequest request);
}
