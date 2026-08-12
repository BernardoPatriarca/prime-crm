package com.primecrm.core.mapper;

import com.primecrm.core.dto.commercial.OpportunityCardResponse;
import com.primecrm.core.dto.commercial.OpportunityRequest;
import com.primecrm.core.dto.commercial.OpportunityResponse;
import com.primecrm.infra.entity.commercial.Opportunity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = CommercialSummaryMapper.class)
public interface OpportunityMapper {

    OpportunityResponse toResponse(Opportunity opportunity);

    OpportunityCardResponse toCard(Opportunity opportunity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "contact", ignore = true)
    @Mapping(target = "pipeline", ignore = true)
    @Mapping(target = "stage", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "team", ignore = true)
    @Mapping(target = "sourceLead", ignore = true)
    @Mapping(target = "openedAt", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "outcome", ignore = true)
    @Mapping(target = "winReason", ignore = true)
    @Mapping(target = "lossReason", ignore = true)
    @Mapping(target = "probability", ignore = true)
    Opportunity toEntity(OpportunityRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "contact", ignore = true)
    @Mapping(target = "pipeline", ignore = true)
    @Mapping(target = "stage", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "team", ignore = true)
    @Mapping(target = "sourceLead", ignore = true)
    @Mapping(target = "openedAt", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "outcome", ignore = true)
    @Mapping(target = "winReason", ignore = true)
    @Mapping(target = "lossReason", ignore = true)
    @Mapping(target = "probability", ignore = true)
    void updateEntity(@MappingTarget Opportunity opportunity, OpportunityRequest request);
}
