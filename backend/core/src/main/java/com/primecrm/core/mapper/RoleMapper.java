package com.primecrm.core.mapper;

import com.primecrm.core.dto.permission.PermissionResponse;
import com.primecrm.core.dto.role.RoleRequest;
import com.primecrm.core.dto.role.RoleResponse;
import com.primecrm.infra.entity.auth.Role;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "permissions", source = "permissions")
    RoleResponse toResponse(Role role, List<PermissionResponse> permissions);

    @Mapping(target = "id", ignore = true)
    Role toEntity(RoleRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void updateEntity(@MappingTarget Role role, RoleRequest request);
}
