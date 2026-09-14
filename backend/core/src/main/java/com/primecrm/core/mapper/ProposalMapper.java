package com.primecrm.core.mapper;

import com.primecrm.core.dto.proposal.ProposalRequest;
import com.primecrm.core.dto.proposal.ProposalResponse;
import com.primecrm.infra.entity.proposal.Proposal;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = CommercialSummaryMapper.class)
public interface ProposalMapper {

    ProposalResponse toResponse(Proposal proposal);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "contact", ignore = true)
    @Mapping(target = "opportunity", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "decidedAt", ignore = true)
    Proposal toEntity(ProposalRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "contact", ignore = true)
    @Mapping(target = "opportunity", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "decidedAt", ignore = true)
    void updateEntity(@MappingTarget Proposal proposal, ProposalRequest request);
}
