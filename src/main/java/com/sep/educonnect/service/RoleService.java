package com.sep.educonnect.service;

import com.sep.educonnect.dto.role.request.RoleRequest;
import com.sep.educonnect.dto.role.response.RoleResponse;
import com.sep.educonnect.entity.Role;
import com.sep.educonnect.mapper.RoleMapper;
import com.sep.educonnect.repository.RoleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleService {
    RoleRepository roleRepository;
    RoleMapper roleMapper;

    public RoleResponse create(RoleRequest request) {
        var role = roleMapper.toRole(request);
        role = roleRepository.save(role);
        return roleMapper.toRoleResponse(role);
    }

    public Page<RoleResponse> getAll(int page, int size, String sortBy, String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<Role> rolePage = roleRepository.findAll(pageable);
        return rolePage.map(roleMapper::toRoleResponse);
    }

    public void delete(Long roleId) {
        roleRepository.deleteById(roleId);
    }
}
