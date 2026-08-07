package com.primecrm.core.mapper;

import com.primecrm.core.dto.user.RoleSummaryResponse;
import com.primecrm.core.dto.user.UserCreateRequest;
import com.primecrm.core.dto.user.UserResponse;
import com.primecrm.core.dto.user.UserUpdateRequest;
import com.primecrm.infra.entity.auth.Role;
import com.primecrm.infra.entity.auth.User;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface UserMapper {

    RoleSummaryResponse toRoleSummary(Role role);

    List<RoleSummaryResponse> toRoleSummaryList(List<Role> roles);

    @Mapping(target = "roles", source = "roles")
    UserResponse toResponse(User user, List<RoleSummaryResponse> roles);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "status", ignore = true)
    User toEntity(UserCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntity(@MappingTarget User user, UserUpdateRequest request);
}
