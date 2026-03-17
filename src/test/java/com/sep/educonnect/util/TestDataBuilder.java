package com.sep.educonnect.util;

import com.sep.educonnect.constant.PredefinedRole;
import com.sep.educonnect.dto.auth.request.AuthenticationRequest;
import com.sep.educonnect.dto.user.request.UserCreationRequest;
import com.sep.educonnect.entity.Role;
import com.sep.educonnect.entity.User;

import java.time.LocalDate;

/**
 * Utility class để tạo test data cho các test cases
 */
public class TestDataBuilder {

    public static User.UserBuilder defaultUser() {
        return User.builder()
                .userId("test-user-id")
                .username("testuser")
                .email("test@example.com")
                .password("$2a$10$encryptedPasswordHash")
                .firstName("Test")
                .lastName("User")
                .phoneNumber("0123456789")
                .address("Test Address")
                .dob(LocalDate.of(1990, 1, 1))
                .loginType("EMAIL");
    }

    public static User createUserWithRole(String username, String email, Role role) {
        return defaultUser()
                .username(username)
                .email(email)
                .role(role)
                .build();
    }

    public static User createUserWithPassword(String username, String password) {
        return defaultUser()
                .username(username)
                .password(password)
                .build();
    }

    public static Role.RoleBuilder defaultRole() {
        return Role.builder()
                .id(1L)
                .name(PredefinedRole.STUDENT_ROLE);
    }

    public static Role createRole(String roleName) {
        return defaultRole()
                .name(roleName)
                .build();
    }

    public static UserCreationRequest.UserCreationRequestBuilder defaultUserCreationRequest() {
        return UserCreationRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .phoneNumber("0987654321")
                .address("New Address")
                .dob(LocalDate.of(1995, 5, 15))
                .loginType("EMAIL")
                .roleName(PredefinedRole.STUDENT_ROLE);
    }

    public static AuthenticationRequest.AuthenticationRequestBuilder defaultAuthenticationRequest() {
        return AuthenticationRequest.builder()
                .username("testuser")
                .password("password123");
    }

    public static AuthenticationRequest createAuthRequest(String username, String password) {
        return defaultAuthenticationRequest()
                .username(username)
                .password(password)
                .build();
    }
}

