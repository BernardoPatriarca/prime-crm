package com.primecrm.core.mapper;

import com.primecrm.core.dto.task.TaskRequest;
import com.primecrm.core.dto.task.TaskResponse;
import com.primecrm.infra.entity.task.Task;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = CommercialSummaryMapper.class)
public interface TaskMapper {

    TaskResponse toResponse(Task task);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "priority", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "contact", ignore = true)
    @Mapping(target = "lead", ignore = true)
    @Mapping(target = "opportunity", ignore = true)
    Task toEntity(TaskRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "priority", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "contact", ignore = true)
    @Mapping(target = "lead", ignore = true)
    @Mapping(target = "opportunity", ignore = true)
    void updateEntity(@MappingTarget Task task, TaskRequest request);
}
