package com.sep.educonnect.mapper;

import com.sep.educonnect.dto.role.request.RoleRequest;
import com.sep.educonnect.dto.role.response.RoleResponse;
import com.sep.educonnect.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "id", ignore = true)
    Role toRole(RoleRequest request);

    @Mapping(target = "description", ignore = true)
    RoleResponse toRoleResponse(Role role);
}
