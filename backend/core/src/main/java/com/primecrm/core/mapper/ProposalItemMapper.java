package com.primecrm.core.mapper;

import com.primecrm.core.dto.proposal.ProposalItemRequest;
import com.primecrm.core.dto.proposal.ProposalItemResponse;
import com.primecrm.infra.entity.proposal.ProposalItem;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = CommercialSummaryMapper.class)
public interface ProposalItemMapper {

    ProposalItemResponse toResponse(ProposalItem item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "proposal", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(target = "discountPercent", ignore = true)
    @Mapping(target = "displayOrder", ignore = true)
    ProposalItem toEntity(ProposalItemRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "proposal", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(target = "discountPercent", ignore = true)
    @Mapping(target = "displayOrder", ignore = true)
    void updateEntity(@MappingTarget ProposalItem item, ProposalItemRequest request);
}
