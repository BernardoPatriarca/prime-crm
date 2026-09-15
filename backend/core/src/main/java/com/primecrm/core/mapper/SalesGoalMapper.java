package com.primecrm.core.mapper;

import com.primecrm.core.dto.sales.SalesGoalRequest;
import com.primecrm.core.dto.sales.SalesGoalResponse;
import com.primecrm.infra.entity.sales.SalesGoal;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = CommercialSummaryMapper.class)
public interface SalesGoalMapper {

    @Mapping(target = "realizedAmount", ignore = true)
    @Mapping(target = "achievementPercent", ignore = true)
    SalesGoalResponse toResponse(SalesGoal goal);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    SalesGoal toEntity(SalesGoalRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    void updateEntity(@MappingTarget SalesGoal goal, SalesGoalRequest request);
}
