package com.primecrm.core.mapper;

import com.primecrm.core.dto.commercial.CustomerRequest;
import com.primecrm.core.dto.commercial.CustomerResponse;
import com.primecrm.core.dto.common.DomainValueSummaryResponse;
import com.primecrm.infra.entity.commercial.Customer;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = CommercialSummaryMapper.class)
public interface CustomerMapper {

    @Mapping(target = "tags", source = "tags")
    CustomerResponse toResponse(Customer customer, List<DomainValueSummaryResponse> tags);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "clientType", ignore = true)
    @Mapping(target = "segment", ignore = true)
    @Mapping(target = "activityBranch", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "origin", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "team", ignore = true)
    @Mapping(target = "parentCustomer", ignore = true)
    Customer toEntity(CustomerRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "clientType", ignore = true)
    @Mapping(target = "segment", ignore = true)
    @Mapping(target = "activityBranch", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "origin", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "team", ignore = true)
    @Mapping(target = "parentCustomer", ignore = true)
    void updateEntity(@MappingTarget Customer customer, CustomerRequest request);
}
