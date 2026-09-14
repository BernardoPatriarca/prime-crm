package com.primecrm.core.mapper;

import com.primecrm.core.dto.order.OrderItemRequest;
import com.primecrm.core.dto.order.OrderItemResponse;
import com.primecrm.infra.entity.order.OrderItem;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = CommercialSummaryMapper.class)
public interface OrderItemMapper {

    OrderItemResponse toResponse(OrderItem item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(target = "discountPercent", ignore = true)
    @Mapping(target = "displayOrder", ignore = true)
    OrderItem toEntity(OrderItemRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(target = "discountPercent", ignore = true)
    @Mapping(target = "displayOrder", ignore = true)
    void updateEntity(@MappingTarget OrderItem item, OrderItemRequest request);
}
