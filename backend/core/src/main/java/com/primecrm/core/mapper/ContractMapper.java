package com.primecrm.core.mapper;

import com.primecrm.core.dto.contract.ContractRequest;
import com.primecrm.core.dto.contract.ContractResponse;
import com.primecrm.infra.entity.contract.Contract;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = CommercialSummaryMapper.class)
public interface ContractMapper {

    ContractResponse toResponse(Contract contract);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "opportunity", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "billingCycle", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "terminatedAt", ignore = true)
    Contract toEntity(ContractRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "opportunity", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "billingCycle", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "terminatedAt", ignore = true)
    void updateEntity(@MappingTarget Contract contract, ContractRequest request);
}
