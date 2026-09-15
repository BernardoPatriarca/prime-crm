package com.primecrm.core.mapper;

import com.primecrm.core.dto.finance.ReceivableRequest;
import com.primecrm.core.dto.finance.ReceivableResponse;
import com.primecrm.infra.entity.finance.Receivable;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = CommercialSummaryMapper.class)
public interface ReceivableMapper {

    ReceivableResponse toResponse(Receivable receivable);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "contract", ignore = true)
    @Mapping(target = "paymentMethod", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "paidAmount", ignore = true)
    @Mapping(target = "paidAt", ignore = true)
    Receivable toEntity(ReceivableRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "contract", ignore = true)
    @Mapping(target = "paymentMethod", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "paidAmount", ignore = true)
    @Mapping(target = "paidAt", ignore = true)
    void updateEntity(@MappingTarget Receivable receivable, ReceivableRequest request);
}
