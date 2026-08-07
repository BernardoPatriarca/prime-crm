package com.primecrm.core.mapper;

import com.primecrm.core.dto.holiday.HolidayRequest;
import com.primecrm.core.dto.holiday.HolidayResponse;
import com.primecrm.infra.entity.config.Holiday;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface HolidayMapper {

    HolidayResponse toResponse(Holiday holiday);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "national", ignore = true)
    @Mapping(target = "active", ignore = true)
    Holiday toEntity(HolidayRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "national", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(@MappingTarget Holiday holiday, HolidayRequest request);
}
