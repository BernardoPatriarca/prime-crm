package com.primecrm.core.mapper;

import com.primecrm.core.dto.agenda.CalendarEventRequest;
import com.primecrm.core.dto.agenda.CalendarEventResponse;
import com.primecrm.infra.entity.agenda.CalendarEvent;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = CommercialSummaryMapper.class)
public interface CalendarEventMapper {

    CalendarEventResponse toResponse(CalendarEvent event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "contact", ignore = true)
    @Mapping(target = "lead", ignore = true)
    @Mapping(target = "opportunity", ignore = true)
    CalendarEvent toEntity(CalendarEventRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "contact", ignore = true)
    @Mapping(target = "lead", ignore = true)
    @Mapping(target = "opportunity", ignore = true)
    void updateEntity(@MappingTarget CalendarEvent event, CalendarEventRequest request);
}
