package com.primecrm.core.mapper;

import com.primecrm.core.dto.order.OrderRequest;
import com.primecrm.core.dto.order.OrderResponse;
import com.primecrm.infra.entity.order.Order;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = CommercialSummaryMapper.class)
public interface OrderMapper {

    OrderResponse toResponse(Order order);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "proposal", ignore = true)
    @Mapping(target = "opportunity", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    Order toEntity(OrderRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "proposal", ignore = true)
    @Mapping(target = "opportunity", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    void updateEntity(@MappingTarget Order order, OrderRequest request);
}
