package com.sep.educonnect.service.unit;

import com.sep.educonnect.dto.role.request.RoleRequest;
import com.sep.educonnect.dto.role.response.RoleResponse;
import com.sep.educonnect.entity.Role;
import com.sep.educonnect.mapper.RoleMapper;
import com.sep.educonnect.repository.RoleRepository;
import com.sep.educonnect.service.RoleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService Unit Tests")
class RoleServiceTest {

        @Mock
        private RoleRepository roleRepository;

        @Mock
        private RoleMapper roleMapper;

        @InjectMocks
        private RoleService roleService;

        @Test
        @DisplayName("Should create role successfully")
        void should_createRole() {
                // Given
                RoleRequest request = RoleRequest.builder()
                                .name("TUTOR")
                                .description("Tutor role")
                                .build();

                Role roleEntity = Role.builder()
                                .id(1L)
                                .name("TUTOR")
                                .build();

                RoleResponse expectedResponse = RoleResponse.builder()
                                .name("TUTOR")
                                .description("Tutor role")
                                .build();

                when(roleMapper.toRole(request)).thenReturn(roleEntity);
                when(roleRepository.save(roleEntity)).thenReturn(roleEntity);
                when(roleMapper.toRoleResponse(roleEntity)).thenReturn(expectedResponse);

                // When
                RoleResponse response = roleService.create(request);

                // Then
                assertEquals(expectedResponse, response);
                verify(roleRepository).save(roleEntity);
        }

        @Test
        @DisplayName("Should return paged roles")
        void should_getAllRolesWithPagination() {
                // Given
                Role role = Role.builder()
                                .id(2L)
                                .name("ADMIN")
                                .build();
                Page<Role> rolePage = new PageImpl<>(List.of(role));
                RoleResponse mappedResponse = RoleResponse.builder()
                                .name("ADMIN")
                                .build();

                when(roleRepository.findAll(any(Pageable.class))).thenReturn(rolePage);
                when(roleMapper.toRoleResponse(role)).thenReturn(mappedResponse);

                // When
                Page<RoleResponse> result = roleService.getAll(0, 5, "name", "asc");

                // Then
                assertEquals(1, result.getTotalElements());
                assertEquals(mappedResponse, result.getContent().get(0));
                verify(roleRepository).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("Should delete role by id")
        void should_deleteRole() {
                // Given
                Long roleId = 3L;

                // When
                roleService.delete(roleId);

                // Then
                verify(roleRepository).deleteById(roleId);
        }

        @Test
        @DisplayName("Should create role with all fields populated")
        void should_createRoleWithAllFields() {
                // Given
                RoleRequest request = RoleRequest.builder()
                                .name("STUDENT")
                                .description("Student role with full access to courses")
                                .build();

                Role roleEntity = Role.builder()
                                .id(4L)
                                .name("STUDENT")
                                .build();

                RoleResponse expectedResponse = RoleResponse.builder()
                                .name("STUDENT")
                                .description("Student role with full access to courses")
                                .build();

                when(roleMapper.toRole(request)).thenReturn(roleEntity);
                when(roleRepository.save(roleEntity)).thenReturn(roleEntity);
                when(roleMapper.toRoleResponse(roleEntity)).thenReturn(expectedResponse);

                // When
                RoleResponse response = roleService.create(request);

                // Then
                assertNotNull(response);
                assertEquals("STUDENT", response.getName());
                assertEquals("Student role with full access to courses", response.getDescription());
                verify(roleMapper).toRole(request);
                verify(roleRepository).save(roleEntity);
                verify(roleMapper).toRoleResponse(roleEntity);
        }

        @Test
        @DisplayName("Should return empty page when no roles exist")
        void should_returnEmptyPageWhenNoRoles() {
                // Given
                Page<Role> emptyPage = new PageImpl<>(List.of());

                when(roleRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

                // When
                Page<RoleResponse> result = roleService.getAll(0, 10, "name", "asc");

                // Then
                assertTrue(result.isEmpty());
                assertEquals(0, result.getTotalElements());
                verify(roleRepository).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("Should apply descending sort order correctly")
        void should_getAllRolesWithDescendingSort() {
                // Given
                Role role1 = Role.builder().id(1L).name("ADMIN").build();
                Role role2 = Role.builder().id(2L).name("TUTOR").build();
                Page<Role> rolePage = new PageImpl<>(List.of(role1, role2));

                RoleResponse response1 = RoleResponse.builder().name("ADMIN").build();
                RoleResponse response2 = RoleResponse.builder().name("TUTOR").build();

                when(roleRepository.findAll(any(Pageable.class))).thenReturn(rolePage);
                when(roleMapper.toRoleResponse(role1)).thenReturn(response1);
                when(roleMapper.toRoleResponse(role2)).thenReturn(response2);

                // When
                Page<RoleResponse> result = roleService.getAll(0, 10, "name", "desc");

                // Then
                assertEquals(2, result.getTotalElements());
                assertEquals(List.of(response1, response2), result.getContent());
                verify(roleRepository).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("Should delete role by specific id")
        void should_deleteRoleBySpecificId() {
                // Given
                Long roleId = 5L;

                // When
                roleService.delete(roleId);

                // Then
                verify(roleRepository, times(1)).deleteById(5L);
        }
}
