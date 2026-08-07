package com.primecrm.core.mapper;

import com.primecrm.core.dto.permission.PermissionResponse;
import com.primecrm.infra.entity.auth.Permission;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    PermissionResponse toResponse(Permission permission);

    List<PermissionResponse> toResponseList(List<Permission> permissions);
}
