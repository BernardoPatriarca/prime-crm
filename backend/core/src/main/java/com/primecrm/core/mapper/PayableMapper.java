package com.primecrm.core.mapper;

import com.primecrm.core.dto.finance.PayableRequest;
import com.primecrm.core.dto.finance.PayableResponse;
import com.primecrm.infra.entity.finance.Payable;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = CommercialSummaryMapper.class)
public interface PayableMapper {

    PayableResponse toResponse(Payable payable);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "paymentMethod", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "paidAmount", ignore = true)
    @Mapping(target = "paidAt", ignore = true)
    Payable toEntity(PayableRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "paymentMethod", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "paidAmount", ignore = true)
    @Mapping(target = "paidAt", ignore = true)
    void updateEntity(@MappingTarget Payable payable, PayableRequest request);
}
