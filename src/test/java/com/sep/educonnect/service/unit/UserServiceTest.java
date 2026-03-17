package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.sep.educonnect.constant.PredefinedRole;
import com.sep.educonnect.dto.user.request.ChangePasswordRequest;
import com.sep.educonnect.dto.user.request.UserCreationRequest;
import com.sep.educonnect.dto.user.request.UserUpdateRequest;
import com.sep.educonnect.dto.user.response.ChangePasswordResponse;
import com.sep.educonnect.dto.user.response.UserResponse;
import com.sep.educonnect.entity.Role;
import com.sep.educonnect.entity.User;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.UserMapper;
import com.sep.educonnect.repository.RoleRepository;
import com.sep.educonnect.repository.UserRepository;
import com.sep.educonnect.service.UserService;
import com.sep.educonnect.util.MockHelper;
import com.sep.educonnect.util.TestDataBuilder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;

    @Mock private RoleRepository roleRepository;

    @Mock private UserMapper userMapper;

    @Mock private PasswordEncoder passwordEncoder;

    @Mock private com.sep.educonnect.service.EmailVerificationService emailVerificationService;

    @InjectMocks private UserService userService;

    private User testUser;
    private Role testRole;
    private UserResponse testUserResponse;

    @BeforeEach
    void setUp() {
        testRole = TestDataBuilder.defaultRole().name(PredefinedRole.STUDENT_ROLE).build();

        testUser =
                TestDataBuilder.defaultUser()
                        .userId("test-user-id")
                        .username("testuser")
                        .email("test@example.com")
                        .role(testRole)
                        .build();

        testUserResponse =
                UserResponse.builder()
                        .userId("test-user-id")
                        .username("testuser")
                        .email("test@example.com")
                        .build();
    }

    @AfterEach
    void tearDown() {
        MockHelper.clearSecurityContext();
    }

    @Test
    @DisplayName("Should create user successfully with valid request")
    void should_createUserSuccessfully_when_validRequest() {
        // Given
        UserCreationRequest request =
                TestDataBuilder.defaultUserCreationRequest()
                        .username("newuser")
                        .email("newuser@example.com")
                        .password("password123")
                        .roleName(PredefinedRole.STUDENT_ROLE)
                        .build();

        User newUser =
                TestDataBuilder.defaultUser()
                        .username("newuser")
                        .email("newuser@example.com")
                        .role(testRole)
                        .build();

        when(userMapper.toUser(request)).thenReturn(newUser);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(roleRepository.findByName(PredefinedRole.STUDENT_ROLE))
                .thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(userMapper.toUserResponse(newUser)).thenReturn(testUserResponse);

        // When
        UserResponse response = userService.createUser(request);

        // Then
        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        verify(userMapper, times(1)).toUser(request);
        verify(passwordEncoder, times(1)).encode("password123");
        verify(roleRepository, times(1)).findByName(PredefinedRole.STUDENT_ROLE);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when role not found")
    void should_throwException_when_roleNotFound() {
        // Given
        UserCreationRequest request =
                TestDataBuilder.defaultUserCreationRequest()
                        .username("newuser")
                        .roleName("NONEXISTENT_ROLE")
                        .build();

        User newUser = TestDataBuilder.defaultUser().username("newuser").build();

        when(userMapper.toUser(request)).thenReturn(newUser);
        when(roleRepository.findByName("NONEXISTENT_ROLE")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> userService.createUser(request));

        assertEquals(ErrorCode.ROLE_NOT_EXISTED, exception.getErrorCode());
        verify(roleRepository, times(1)).findByName("NONEXISTENT_ROLE");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when user already exists")
    void should_throwException_when_userAlreadyExists() {
        // Given
        UserCreationRequest request =
                TestDataBuilder.defaultUserCreationRequest()
                        .username("existinguser")
                        .roleName(PredefinedRole.STUDENT_ROLE)
                        .build();

        User newUser = TestDataBuilder.defaultUser().username("existinguser").build();

        when(userMapper.toUser(request)).thenReturn(newUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(roleRepository.findByName(PredefinedRole.STUDENT_ROLE))
                .thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("User exists"));

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> userService.createUser(request));

        assertEquals(ErrorCode.USER_EXISTED, exception.getErrorCode());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should get my info successfully")
    void should_getMyInfoSuccessfully() {
        // Given
        MockHelper.mockSecurityContext("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

        // When
        UserResponse response = userService.getMyInfo();

        // Then
        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should throw exception when user not found in getMyInfo")
    void should_throwException_when_userNotFoundInGetMyInfo() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> userService.getMyInfo());

        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(userRepository, times(1)).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("Should upload avatar successfully")
    void should_uploadAvatarSuccessfully() {
        // Given
        MockHelper.mockSecurityContext("testuser");
        String avatarUrl = "https://example.com/avatar.jpg";
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        String result = userService.uploadAvatar(avatarUrl);

        // Then
        assertEquals(avatarUrl, result);
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should update user successfully")
    void should_updateUserSuccessfully() {
        // Given
        UserUpdateRequest request =
                UserUpdateRequest.builder()
                        .firstName("Updated")
                        .lastName("Name")
                        .email("updated@example.com")
                        .build();

        User updatedUser =
                TestDataBuilder.defaultUser()
                        .userId("test-user-id")
                        .firstName("Updated")
                        .lastName("Name")
                        .email("updated@example.com")
                        .build();

        UserResponse updatedResponse =
                UserResponse.builder()
                        .userId("test-user-id")
                        .firstName("Updated")
                        .lastName("Name")
                        .email("updated@example.com")
                        .build();

        when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));
        doNothing().when(userMapper).updateUser(testUser, request);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toUserResponse(updatedUser)).thenReturn(updatedResponse);

        // When
        UserResponse response = userService.updateUser("test-user-id", request);

        // Then
        assertNotNull(response);
        assertEquals("Updated", response.getFirstName());
        verify(userRepository, times(1)).findById("test-user-id");
        verify(userMapper, times(1)).updateUser(testUser, request);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should update password when provided in update request")
    void should_updatePassword_when_providedInUpdateRequest() {
        // Given
        UserUpdateRequest request = UserUpdateRequest.builder().password("newPassword123").build();

        when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));
        doNothing().when(userMapper).updateUser(any(User.class), any(UserUpdateRequest.class));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

        // When
        userService.updateUser("test-user-id", request);

        // Then
        verify(passwordEncoder, times(1)).encode("newPassword123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    // ==================== CHANGE PASSWORD TEST CASES ====================

    @Test
    @DisplayName("CP01 - Should change password successfully")
    void should_changePassword_successfully() {
        // Given
        MockHelper.mockSecurityContext("testuser");
        ChangePasswordRequest request =
                ChangePasswordRequest.builder()
                        .currentPassword("oldPassword123")
                        .newPassword("newPassword123")
                        .confirmPassword("newPassword123")
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword123", testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        ChangePasswordResponse response = userService.changePassword(request);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("success.password.changed", response.getMessage());
        verify(passwordEncoder, times(1)).matches(eq("oldPassword123"), anyString());
        verify(passwordEncoder, times(1)).encode("newPassword123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName(
            "CP02 - Should throw PASSWORD_MISMATCH when new password and confirm password do not match")
    void should_throwPasswordMismatch_whenNewPasswordAndConfirmPasswordDoNotMatch() {
        // Given
        ChangePasswordRequest request =
                ChangePasswordRequest.builder()
                        .currentPassword("oldPassword123")
                        .newPassword("newPassword123")
                        .confirmPassword("differentPassword")
                        .build();

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> userService.changePassword(request));

        assertEquals(ErrorCode.PASSWORD_MISMATCH, exception.getErrorCode());
        verify(userRepository, never()).findByUsername(any());
        verify(passwordEncoder, never()).matches(any(), any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("CP03 - Should throw USER_NOT_EXISTED when user not logged in")
    void should_throwUserNotExisted_whenUserNotLoggedIn() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");
        ChangePasswordRequest request =
                ChangePasswordRequest.builder()
                        .currentPassword("oldPassword123")
                        .newPassword("newPassword123")
                        .confirmPassword("newPassword123")
                        .build();

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> userService.changePassword(request));

        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(userRepository, times(1)).findByUsername("nonexistent");
        verify(passwordEncoder, never()).matches(any(), any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("CP04 - Should throw CURRENT_PASSWORD_INCORRECT when current password is wrong")
    void should_throwCurrentPasswordIncorrect_whenCurrentPasswordIsWrong() {
        // Given
        MockHelper.mockSecurityContext("testuser");
        ChangePasswordRequest request =
                ChangePasswordRequest.builder()
                        .currentPassword("wrongPassword")
                        .newPassword("newPassword123")
                        .confirmPassword("newPassword123")
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", testUser.getPassword())).thenReturn(false);

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> userService.changePassword(request));

        assertEquals(ErrorCode.CURRENT_PASSWORD_INCORRECT, exception.getErrorCode());
        verify(passwordEncoder, times(1)).matches("wrongPassword", testUser.getPassword());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("CP05 - Should change password with strong new password")
    void should_changePassword_withStrongNewPassword() {
        // Given
        MockHelper.mockSecurityContext("testuser");
        String strongPassword = "StrongP@ssw0rd!2023";
        ChangePasswordRequest request =
                ChangePasswordRequest.builder()
                        .currentPassword("oldPassword123")
                        .newPassword(strongPassword)
                        .confirmPassword(strongPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword123", testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(strongPassword)).thenReturn("encodedStrongPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        ChangePasswordResponse response = userService.changePassword(request);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        verify(passwordEncoder, times(1)).encode(strongPassword);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("CP06 - Should throw PASSWORD_MISMATCH when passwords differ by case")
    void should_throwPasswordMismatch_whenPasswordsDifferByCase() {
        // Given
        ChangePasswordRequest request =
                ChangePasswordRequest.builder()
                        .currentPassword("oldPassword123")
                        .newPassword("newPassword123")
                        .confirmPassword("NewPassword123") // Different case
                        .build();

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> userService.changePassword(request));

        assertEquals(ErrorCode.PASSWORD_MISMATCH, exception.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("CP07 - Should throw PASSWORD_MISMATCH when passwords have trailing spaces")
    void should_throwPasswordMismatch_whenPasswordsHaveTrailingSpaces() {
        // Given
        ChangePasswordRequest request =
                ChangePasswordRequest.builder()
                        .currentPassword("oldPassword123")
                        .newPassword("newPassword123")
                        .confirmPassword("newPassword123 ") // Trailing space
                        .build();

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> userService.changePassword(request));

        assertEquals(ErrorCode.PASSWORD_MISMATCH, exception.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("CP08 - Should change password when new password is same as old password")
    void should_changePassword_whenNewPasswordIsSameAsOld() {
        // Given
        MockHelper.mockSecurityContext("testuser");
        String samePassword = "samePassword123";
        ChangePasswordRequest request =
                ChangePasswordRequest.builder()
                        .currentPassword(samePassword)
                        .newPassword(samePassword)
                        .confirmPassword(samePassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(samePassword, testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(samePassword)).thenReturn("encodedSamePassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        ChangePasswordResponse response = userService.changePassword(request);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("CP09 - Should verify password is encoded before saving")
    void should_verifyPasswordIsEncoded_beforeSaving() {
        // Given
        MockHelper.mockSecurityContext("testuser");
        ChangePasswordRequest request =
                ChangePasswordRequest.builder()
                        .currentPassword("oldPassword123")
                        .newPassword("newPassword123")
                        .confirmPassword("newPassword123")
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword123", testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class)))
                .thenAnswer(
                        invocation -> {
                            User savedUser = invocation.getArgument(0);
                            assertEquals("encodedNewPassword", savedUser.getPassword());
                            return savedUser;
                        });

        // When
        ChangePasswordResponse response = userService.changePassword(request);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        verify(passwordEncoder, times(1)).encode("newPassword123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("CP10 - Should change password for different user")
    void should_changePassword_forDifferentUser() {
        // Given
        MockHelper.mockSecurityContext("anotheruser");
        User anotherUser =
                TestDataBuilder.defaultUser()
                        .userId("another-user-id")
                        .username("anotheruser")
                        .password("encodedOldPassword")
                        .role(testRole)
                        .build();

        ChangePasswordRequest request =
                ChangePasswordRequest.builder()
                        .currentPassword("oldPassword456")
                        .newPassword("newPassword456")
                        .confirmPassword("newPassword456")
                        .build();

        when(userRepository.findByUsername("anotheruser")).thenReturn(Optional.of(anotherUser));
        when(passwordEncoder.matches("oldPassword456", anotherUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newPassword456")).thenReturn("encodedNewPassword456");
        when(userRepository.save(any(User.class))).thenReturn(anotherUser);

        // When
        ChangePasswordResponse response = userService.changePassword(request);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        verify(userRepository, times(1)).findByUsername("anotheruser");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName(
            "CP11 - Should throw CURRENT_PASSWORD_INCORRECT when current password is empty string")
    void should_throwCurrentPasswordIncorrect_whenCurrentPasswordIsEmpty() {
        // Given
        MockHelper.mockSecurityContext("testuser");
        ChangePasswordRequest request =
                ChangePasswordRequest.builder()
                        .currentPassword("")
                        .newPassword("newPassword123")
                        .confirmPassword("newPassword123")
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("", testUser.getPassword())).thenReturn(false);

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> userService.changePassword(request));

        assertEquals(ErrorCode.CURRENT_PASSWORD_INCORRECT, exception.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("CP12 - Should change password with special characters")
    void should_changePassword_withSpecialCharacters() {
        // Given
        MockHelper.mockSecurityContext("testuser");
        String specialPassword = "P@ssw0rd!#$%^&*()";
        ChangePasswordRequest request =
                ChangePasswordRequest.builder()
                        .currentPassword("oldPassword123")
                        .newPassword(specialPassword)
                        .confirmPassword(specialPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword123", testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(specialPassword)).thenReturn("encodedSpecialPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        ChangePasswordResponse response = userService.changePassword(request);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        verify(passwordEncoder, times(1)).encode(specialPassword);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("CP13 - Should ensure response contains correct message")
    void should_ensureResponseContainsCorrectMessage() {
        // Given
        MockHelper.mockSecurityContext("testuser");
        ChangePasswordRequest request =
                ChangePasswordRequest.builder()
                        .currentPassword("oldPassword123")
                        .newPassword("newPassword123")
                        .confirmPassword("newPassword123")
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword123", testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        ChangePasswordResponse response = userService.changePassword(request);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("success.password.changed", response.getMessage());
        assertNotNull(response.getMessage());
    }

    @Test
    @DisplayName("CP14 - Should verify all methods called in correct order")
    void should_verifyAllMethodsCalledInCorrectOrder() {
        // Given
        MockHelper.mockSecurityContext("testuser");
        ChangePasswordRequest request =
                ChangePasswordRequest.builder()
                        .currentPassword("oldPassword123")
                        .newPassword("newPassword123")
                        .confirmPassword("newPassword123")
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(eq("oldPassword123"), any())).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        ChangePasswordResponse response = userService.changePassword(request);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());

        // Verify order of operations
        var inOrder = inOrder(userRepository, passwordEncoder);
        inOrder.verify(userRepository).findByUsername("testuser");
        inOrder.verify(passwordEncoder).matches(eq("oldPassword123"), any());
        inOrder.verify(passwordEncoder).encode("newPassword123");
        inOrder.verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("CP15 - Should change password with minimum length password")
    void should_changePassword_withMinimumLengthPassword() {
        // Given
        MockHelper.mockSecurityContext("testuser");
        String minPassword = "Pass1!"; // Minimum viable password
        ChangePasswordRequest request =
                ChangePasswordRequest.builder()
                        .currentPassword("oldPassword123")
                        .newPassword(minPassword)
                        .confirmPassword(minPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword123", testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(minPassword)).thenReturn("encodedMinPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        ChangePasswordResponse response = userService.changePassword(request);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        verify(passwordEncoder, times(1)).encode(minPassword);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("CP16 - Should change password with maximum length password")
    void should_changePassword_withMaximumLengthPassword() {
        // Given
        MockHelper.mockSecurityContext("testuser");
        String maxPassword = "P@ssw0rd!" + "a".repeat(100); // Long password
        ChangePasswordRequest request =
                ChangePasswordRequest.builder()
                        .currentPassword("oldPassword123")
                        .newPassword(maxPassword)
                        .confirmPassword(maxPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword123", testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(maxPassword)).thenReturn("encodedMaxPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        ChangePasswordResponse response = userService.changePassword(request);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        verify(passwordEncoder, times(1)).encode(maxPassword);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName(
            "CP17 - Should throw PASSWORD_MISMATCH when new password is empty but confirm is not")
    void should_throwPasswordMismatch_whenNewPasswordEmptyButConfirmIsNot() {
        // Given
        ChangePasswordRequest request =
                ChangePasswordRequest.builder()
                        .currentPassword("oldPassword123")
                        .newPassword("")
                        .confirmPassword("newPassword123")
                        .build();

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> userService.changePassword(request));

        assertEquals(ErrorCode.PASSWORD_MISMATCH, exception.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("CP18 - Should not call save when password mismatch occurs")
    void should_notCallSave_whenPasswordMismatchOccurs() {
        // Given
        ChangePasswordRequest request =
                ChangePasswordRequest.builder()
                        .currentPassword("oldPassword123")
                        .newPassword("newPassword123")
                        .confirmPassword("differentPassword123")
                        .build();

        // When & Then
        assertThrows(AppException.class, () -> userService.changePassword(request));

        verify(userRepository, never()).findByUsername(any());
        verify(passwordEncoder, never()).matches(any(), any());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should get users with pagination")
    void should_getUsersWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "username"));
        Page<User> userPage = new PageImpl<>(List.of(testUser), pageable, 1);

        when(userRepository.searchUsers(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(userPage);
        when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

        // When
        Page<UserResponse> response = userService.getUsers(0, 10, "username", "asc", null, null, null, null, null);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        verify(userRepository, times(1)).searchUsers(any(), any(), any(), any(), any(), any(Pageable.class));
    }

    // ==================== GET USER TEST CASES ====================

    @Test
    @DisplayName("GU01 - Should get user by id successfully")
    void should_getUserById_successfully() {
        // Given
        when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));
        when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

        // When
        UserResponse response = userService.getUser("test-user-id");

        // Then
        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        verify(userRepository, times(1)).findById("test-user-id");
        verify(userMapper, times(1)).toUserResponse(testUser);
    }

    @Test
    @DisplayName("GU02 - Should throw USER_NOT_EXISTED when user not found")
    void should_throwUserNotExisted_whenUserNotFound() {
        // Given
        when(userRepository.findById("nonexistent-id")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> userService.getUser("nonexistent-id"));

        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(userRepository, times(1)).findById("nonexistent-id");
        verify(userMapper, never()).toUserResponse(any(User.class));
    }

    @Test
    @DisplayName("GU03 - Should get user with all fields populated")
    void should_getUserById_withAllFieldsPopulated() {
        // Given
        User fullUser =
                TestDataBuilder.defaultUser()
                        .userId("full-user-id")
                        .username("fulluser")
                        .email("fulluser@test.com")
                        .firstName("Full")
                        .lastName("User")
                        .phoneNumber("1234567890")
                        .address("123 Test Street")
                        .avatar("avatar.jpg")
                        .role(testRole)
                        .emailVerified(true)
                        .build();

        UserResponse fullUserResponse =
                UserResponse.builder()
                        .userId("full-user-id")
                        .username("fulluser")
                        .email("fulluser@test.com")
                        .firstName("Full")
                        .lastName("User")
                        .roleName(PredefinedRole.STUDENT_ROLE)
                        .build();

        when(userRepository.findById("full-user-id")).thenReturn(Optional.of(fullUser));
        when(userMapper.toUserResponse(fullUser)).thenReturn(fullUserResponse);

        // When
        UserResponse response = userService.getUser("full-user-id");

        // Then
        assertNotNull(response);
        assertEquals("full-user-id", response.getUserId());
        assertEquals("fulluser", response.getUsername());
        assertEquals("fulluser@test.com", response.getEmail());
        assertEquals("Full", response.getFirstName());
        assertEquals("User", response.getLastName());
        assertEquals(PredefinedRole.STUDENT_ROLE, response.getRoleName());
        verify(userRepository, times(1)).findById("full-user-id");
    }

    @Test
    @DisplayName("GU04 - Should get user with different role (TUTOR)")
    void should_getUserById_withTutorRole() {
        // Given
        Role tutorRole = Role.builder().name(PredefinedRole.TUTOR_ROLE).build();

        User tutorUser =
                TestDataBuilder.defaultUser()
                        .userId("tutor-user-id")
                        .username("tutoruser")
                        .email("tutor@test.com")
                        .role(tutorRole)
                        .build();

        UserResponse tutorResponse =
                UserResponse.builder()
                        .userId("tutor-user-id")
                        .username("tutoruser")
                        .email("tutor@test.com")
                        .roleName(PredefinedRole.TUTOR_ROLE)
                        .build();

        when(userRepository.findById("tutor-user-id")).thenReturn(Optional.of(tutorUser));
        when(userMapper.toUserResponse(tutorUser)).thenReturn(tutorResponse);

        // When
        UserResponse response = userService.getUser("tutor-user-id");

        // Then
        assertNotNull(response);
        assertEquals("tutor-user-id", response.getUserId());
        assertEquals("tutoruser", response.getUsername());
        assertEquals(PredefinedRole.TUTOR_ROLE, response.getRoleName());
        verify(userRepository, times(1)).findById("tutor-user-id");
    }

    @Test
    @DisplayName("GU05 - Should get user with different role (ADMIN)")
    void should_getUserById_withAdminRole() {
        // Given
        Role adminRole = Role.builder().name(PredefinedRole.ADMIN_ROLE).build();

        User adminUser =
                TestDataBuilder.defaultUser()
                        .userId("admin-user-id")
                        .username("adminuser")
                        .email("admin@test.com")
                        .role(adminRole)
                        .build();

        UserResponse adminResponse =
                UserResponse.builder()
                        .userId("admin-user-id")
                        .username("adminuser")
                        .email("admin@test.com")
                        .roleName(PredefinedRole.ADMIN_ROLE)
                        .build();

        when(userRepository.findById("admin-user-id")).thenReturn(Optional.of(adminUser));
        when(userMapper.toUserResponse(adminUser)).thenReturn(adminResponse);

        // When
        UserResponse response = userService.getUser("admin-user-id");

        // Then
        assertNotNull(response);
        assertEquals("admin-user-id", response.getUserId());
        assertEquals("adminuser", response.getUsername());
        assertEquals(PredefinedRole.ADMIN_ROLE, response.getRoleName());
        verify(userRepository, times(1)).findById("admin-user-id");
    }

    @Test
    @DisplayName("GU06 - Should get user with verified email")
    void should_getUserById_withVerifiedEmail() {
        // Given
        User verifiedUser =
                TestDataBuilder.defaultUser()
                        .userId("verified-user-id")
                        .username("verifieduser")
                        .email("verified@test.com")
                        .emailVerified(true)
                        .role(testRole)
                        .build();

        UserResponse verifiedResponse =
                UserResponse.builder()
                        .userId("verified-user-id")
                        .username("verifieduser")
                        .email("verified@test.com")
                        .build();

        when(userRepository.findById("verified-user-id")).thenReturn(Optional.of(verifiedUser));
        when(userMapper.toUserResponse(verifiedUser)).thenReturn(verifiedResponse);

        // When
        UserResponse response = userService.getUser("verified-user-id");

        // Then
        assertNotNull(response);
        assertEquals("verified-user-id", response.getUserId());
        assertEquals("verifieduser", response.getUsername());
        verify(userRepository, times(1)).findById("verified-user-id");
    }

    @Test
    @DisplayName("GU07 - Should get user with unverified email")
    void should_getUserById_withUnverifiedEmail() {
        // Given
        User unverifiedUser =
                TestDataBuilder.defaultUser()
                        .userId("unverified-user-id")
                        .username("unverifieduser")
                        .email("unverified@test.com")
                        .emailVerified(false)
                        .role(testRole)
                        .build();

        UserResponse unverifiedResponse =
                UserResponse.builder()
                        .userId("unverified-user-id")
                        .username("unverifieduser")
                        .email("unverified@test.com")
                        .build();

        when(userRepository.findById("unverified-user-id")).thenReturn(Optional.of(unverifiedUser));
        when(userMapper.toUserResponse(unverifiedUser)).thenReturn(unverifiedResponse);

        // When
        UserResponse response = userService.getUser("unverified-user-id");

        // Then
        assertNotNull(response);
        assertEquals("unverified-user-id", response.getUserId());
        assertEquals("unverifieduser", response.getUsername());
        verify(userRepository, times(1)).findById("unverified-user-id");
    }

    @Test
    @DisplayName("GU08 - Should throw USER_NOT_EXISTED with null userId")
    void should_throwUserNotExisted_withNullUserId() {
        // Given
        when(userRepository.findById(null)).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> userService.getUser(null));

        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(userRepository, times(1)).findById(null);
    }

    @Test
    @DisplayName("GU09 - Should throw USER_NOT_EXISTED with empty userId")
    void should_throwUserNotExisted_withEmptyUserId() {
        // Given
        when(userRepository.findById("")).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> userService.getUser(""));

        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(userRepository, times(1)).findById("");
    }

    @Test
    @DisplayName("GU10 - Should get different users with different IDs")
    void should_getUserById_forMultipleDifferentUsers() {
        // Given
        User user1 =
                TestDataBuilder.defaultUser()
                        .userId("user-1")
                        .username("user1")
                        .email("user1@test.com")
                        .role(testRole)
                        .build();

        User user2 =
                TestDataBuilder.defaultUser()
                        .userId("user-2")
                        .username("user2")
                        .email("user2@test.com")
                        .role(testRole)
                        .build();

        UserResponse response1 =
                UserResponse.builder()
                        .userId("user-1")
                        .username("user1")
                        .email("user1@test.com")
                        .build();

        UserResponse response2 =
                UserResponse.builder()
                        .userId("user-2")
                        .username("user2")
                        .email("user2@test.com")
                        .build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user-2")).thenReturn(Optional.of(user2));
        when(userMapper.toUserResponse(user1)).thenReturn(response1);
        when(userMapper.toUserResponse(user2)).thenReturn(response2);

        // When
        UserResponse result1 = userService.getUser("user-1");
        UserResponse result2 = userService.getUser("user-2");

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals("user-1", result1.getUserId());
        assertEquals("user-2", result2.getUserId());
        assertEquals("user1", result1.getUsername());
        assertEquals("user2", result2.getUsername());
        verify(userRepository, times(1)).findById("user-1");
        verify(userRepository, times(1)).findById("user-2");
    }

    @Test
    @DisplayName("GU11 - Should get user with UUID format userId")
    void should_getUserById_withUUIDFormat() {
        // Given
        String uuidUserId = "550e8400-e29b-41d4-a716-446655440000";
        User uuidUser =
                TestDataBuilder.defaultUser()
                        .userId(uuidUserId)
                        .username("uuiduser")
                        .email("uuid@test.com")
                        .role(testRole)
                        .build();

        UserResponse uuidResponse =
                UserResponse.builder()
                        .userId(uuidUserId)
                        .username("uuiduser")
                        .email("uuid@test.com")
                        .build();

        when(userRepository.findById(uuidUserId)).thenReturn(Optional.of(uuidUser));
        when(userMapper.toUserResponse(uuidUser)).thenReturn(uuidResponse);

        // When
        UserResponse response = userService.getUser(uuidUserId);

        // Then
        assertNotNull(response);
        assertEquals(uuidUserId, response.getUserId());
        assertEquals("uuiduser", response.getUsername());
        verify(userRepository, times(1)).findById(uuidUserId);
    }

    @Test
    @DisplayName("GU12 - Should get user and mapper should be called correctly")
    void should_getUserById_andMapperCalledCorrectly() {
        // Given
        when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));
        when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

        // When
        UserResponse response = userService.getUser("test-user-id");

        // Then
        assertNotNull(response);
        verify(userRepository, times(1)).findById("test-user-id");
        verify(userMapper, times(1)).toUserResponse(testUser);
        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(userMapper);
    }

    @Test
    @DisplayName("Should delete user successfully")
    void should_deleteUserSuccessfully() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User adminUser = TestDataBuilder.defaultUser().userId("admin-id").username("admin").build();

        User userToDelete =
                TestDataBuilder.defaultUser()
                        .userId("user-to-delete-id")
                        .username("usertodelete")
                        .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById("user-to-delete-id")).thenReturn(Optional.of(userToDelete));
        when(userRepository.save(any(User.class))).thenReturn(userToDelete);

        // When
        userService.deleteUser("user-to-delete-id");

        // Then
        verify(userRepository, times(1)).findById("user-to-delete-id");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when admin tries to delete themselves")
    void should_throwException_when_adminTriesToDeleteThemselves() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User adminUser = TestDataBuilder.defaultUser().userId("admin-id").username("admin").build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> userService.deleteUser("admin-id"));

        assertEquals(ErrorCode.CANNOT_SELF_DELETE, exception.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should toggle user isDeleted status from false to true")
    void should_deleteUser_toggleIsDeletedFromFalseToTrue() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User adminUser = TestDataBuilder.defaultUser().userId("admin-id").username("admin").build();

        User userToDelete =
                TestDataBuilder.defaultUser().userId("user-123").username("normaluser").build();
        userToDelete.setIsDeleted(false);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById("user-123")).thenReturn(Optional.of(userToDelete));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userService.deleteUser("user-123");

        // Then
        assertTrue(userToDelete.getIsDeleted());
        verify(userRepository, times(1)).findById("user-123");
        verify(userRepository, times(1)).save(userToDelete);
    }

    @Test
    @DisplayName("Should toggle user isDeleted status from true to false (restore user)")
    void should_deleteUser_toggleIsDeletedFromTrueToFalse() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User adminUser = TestDataBuilder.defaultUser().userId("admin-id").username("admin").build();

        User userToRestore =
                TestDataBuilder.defaultUser().userId("user-456").username("deleteduser").build();
        userToRestore.setIsDeleted(true);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById("user-456")).thenReturn(Optional.of(userToRestore));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userService.deleteUser("user-456");

        // Then
        assertFalse(userToRestore.getIsDeleted());
        verify(userRepository, times(1)).findById("user-456");
        verify(userRepository, times(1)).save(userToRestore);
    }

    @Test
    @DisplayName("Should throw USER_NOT_EXISTED when target user not found")
    void should_throwUserNotExisted_whenTargetUserNotFound() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User adminUser = TestDataBuilder.defaultUser().userId("admin-id").username("admin").build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById("non-existent-user-id")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> userService.deleteUser("non-existent-user-id"));

        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(userRepository, times(1)).findById("non-existent-user-id");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw USER_NOT_EXISTED when admin user not found in context")
    void should_throwUserNotExisted_whenAdminUserNotFoundInContext() {
        // Given
        MockHelper.mockSecurityContext("admin");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> userService.deleteUser("user-123"));

        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(userRepository, times(1)).findByUsername("admin");
        verify(userRepository, never()).findById(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should delete user with different userId successfully")
    void should_deleteUser_withDifferentUserId() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User adminUser = TestDataBuilder.defaultUser().userId("admin-id").username("admin").build();

        User userToDelete =
                TestDataBuilder.defaultUser()
                        .userId("different-user-id")
                        .username("differentuser")
                        .build();
        userToDelete.setIsDeleted(false);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById("different-user-id")).thenReturn(Optional.of(userToDelete));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userService.deleteUser("different-user-id");

        // Then
        assertTrue(userToDelete.getIsDeleted());
        verify(userRepository, times(1)).findById("different-user-id");
        verify(userRepository, times(1)).save(userToDelete);
    }

    @Test
    @DisplayName("Should verify soft delete does not actually remove user from database")
    void should_deleteUser_verifySoftDeleteBehavior() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User adminUser = TestDataBuilder.defaultUser().userId("admin-id").username("admin").build();

        User userToDelete =
                TestDataBuilder.defaultUser()
                        .userId("user-789")
                        .username("softdeleteuser")
                        .email("softdelete@example.com")
                        .build();
        userToDelete.setIsDeleted(false);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById("user-789")).thenReturn(Optional.of(userToDelete));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userService.deleteUser("user-789");

        // Then
        assertTrue(userToDelete.getIsDeleted());
        assertEquals("user-789", userToDelete.getUserId());
        assertEquals("softdeleteuser", userToDelete.getUsername());
        assertEquals("softdelete@example.com", userToDelete.getEmail());
        verify(userRepository, times(1)).save(userToDelete);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    @DisplayName("Should verify admin can delete multiple users sequentially")
    void should_deleteUser_allowMultipleDeletions() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User adminUser = TestDataBuilder.defaultUser().userId("admin-id").username("admin").build();

        User user1 = TestDataBuilder.defaultUser().userId("user-1").username("user1").build();
        user1.setIsDeleted(false);

        User user2 = TestDataBuilder.defaultUser().userId("user-2").username("user2").build();
        user2.setIsDeleted(false);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user-2")).thenReturn(Optional.of(user2));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userService.deleteUser("user-1");
        userService.deleteUser("user-2");

        // Then
        assertTrue(user1.getIsDeleted());
        assertTrue(user2.getIsDeleted());
        verify(userRepository, times(2)).save(any(User.class));
    }

    // ==================== COMPREHENSIVE createUser TESTS ====================

    @Test
    @DisplayName("Should create user with username 'abc' successfully")
    void should_createUser_withUsernameAbc() {
        // Given
        UserCreationRequest request =
                UserCreationRequest.builder()
                        .username("abc")
                        .email("hocsinh@gmail.com")
                        .password("12345678")
                        .firstName("Le")
                        .lastName("Tung")
                        .phoneNumber("09123421112")
                        .address("Ho Nai")
                        .dob(java.time.LocalDate.of(2000, 10, 20))
                        .roleName(PredefinedRole.STUDENT_ROLE)
                        .build();

        User newUser =
                TestDataBuilder.defaultUser()
                        .username("abc")
                        .email("hocsinh@gmail.com")
                        .role(testRole)
                        .build();

        when(userMapper.toUser(request)).thenReturn(newUser);
        when(passwordEncoder.encode("12345678")).thenReturn("encodedPassword");
        when(roleRepository.findByName(PredefinedRole.STUDENT_ROLE))
                .thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(userMapper.toUserResponse(newUser)).thenReturn(testUserResponse);

        // When
        UserResponse response = userService.createUser(request);

        // Then
        assertNotNull(response);
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("12345678");
        verify(emailVerificationService, times(1)).scheduleVerificationEmail(any(User.class));
    }

    @Test
    @DisplayName("Should create user with username 'userAbc' successfully")
    void should_createUser_withUsernameUserAbc() {
        // Given
        UserCreationRequest request =
                UserCreationRequest.builder()
                        .username("userAbc")
                        .email("hocsinh@gmail.com")
                        .password("12345678")
                        .firstName("Le")
                        .lastName("Tung")
                        .phoneNumber("09123421112")
                        .address("Ho Nai")
                        .dob(java.time.LocalDate.of(2000, 10, 20))
                        .roleName(PredefinedRole.STUDENT_ROLE)
                        .build();

        User newUser =
                TestDataBuilder.defaultUser()
                        .username("userAbc")
                        .email("hocsinh@gmail.com")
                        .role(testRole)
                        .build();

        when(userMapper.toUser(request)).thenReturn(newUser);
        when(passwordEncoder.encode("12345678")).thenReturn("encodedPassword");
        when(roleRepository.findByName(PredefinedRole.STUDENT_ROLE))
                .thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(userMapper.toUserResponse(newUser)).thenReturn(testUserResponse);

        // When
        UserResponse response = userService.createUser(request);

        // Then
        assertNotNull(response);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw USER_EXISTED when username already exists")
    void should_throwUserExisted_whenUsernameExists() {
        // Given
        UserCreationRequest request =
                UserCreationRequest.builder()
                        .username("abc")
                        .email("hocsinh@gmail.com")
                        .password("12345678")
                        .firstName("Le")
                        .lastName("Tung")
                        .roleName(PredefinedRole.STUDENT_ROLE)
                        .build();

        User newUser = TestDataBuilder.defaultUser().username("abc").build();

        when(userMapper.toUser(request)).thenReturn(newUser);
        when(passwordEncoder.encode("12345678")).thenReturn("encodedPassword");
        when(roleRepository.findByName(PredefinedRole.STUDENT_ROLE))
                .thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate username"));

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> userService.createUser(request));

        assertEquals(ErrorCode.USER_EXISTED, exception.getErrorCode());
        verify(emailVerificationService, never()).scheduleVerificationEmail(any(User.class));
    }

    @Test
    @DisplayName("Should create user with email 'giangvien@gmail.com' successfully")
    void should_createUser_withEmailGiangvien() {
        // Given
        UserCreationRequest request =
                UserCreationRequest.builder()
                        .username("abc")
                        .email("giangvien@gmail.com")
                        .password("12345678")
                        .firstName("Le")
                        .lastName("Tung")
                        .roleName(PredefinedRole.STUDENT_ROLE)
                        .build();

        User newUser =
                TestDataBuilder.defaultUser()
                        .username("abc")
                        .email("giangvien@gmail.com")
                        .role(testRole)
                        .build();

        when(userMapper.toUser(request)).thenReturn(newUser);
        when(passwordEncoder.encode("12345678")).thenReturn("encodedPassword");
        when(roleRepository.findByName(PredefinedRole.STUDENT_ROLE))
                .thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(userMapper.toUserResponse(newUser)).thenReturn(testUserResponse);

        // When
        UserResponse response = userService.createUser(request);

        // Then
        assertNotNull(response);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw USER_EXISTED when email already exists")
    void should_throwUserExisted_whenEmailExists() {
        // Given
        UserCreationRequest request =
                UserCreationRequest.builder()
                        .username("newuser")
                        .email("hocsinh@gmail.com")
                        .password("12345678")
                        .firstName("Le")
                        .lastName("Tung")
                        .roleName(PredefinedRole.STUDENT_ROLE)
                        .build();

        User newUser =
                TestDataBuilder.defaultUser()
                        .username("newuser")
                        .email("hocsinh@gmail.com")
                        .build();

        when(userMapper.toUser(request)).thenReturn(newUser);
        when(passwordEncoder.encode("12345678")).thenReturn("encodedPassword");
        when(roleRepository.findByName(PredefinedRole.STUDENT_ROLE))
                .thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate email"));

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> userService.createUser(request));

        assertEquals(ErrorCode.USER_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should create user with password '512345vsdf' successfully")
    void should_createUser_withPassword512345vsdf() {
        // Given
        UserCreationRequest request =
                UserCreationRequest.builder()
                        .username("abc")
                        .email("hocsinh@gmail.com")
                        .password("512345vsdf")
                        .firstName("Le")
                        .lastName("Tung")
                        .roleName(PredefinedRole.STUDENT_ROLE)
                        .build();

        User newUser =
                TestDataBuilder.defaultUser()
                        .username("abc")
                        .email("hocsinh@gmail.com")
                        .role(testRole)
                        .build();

        when(userMapper.toUser(request)).thenReturn(newUser);
        when(passwordEncoder.encode("512345vsdf")).thenReturn("encodedPassword512345vsdf");
        when(roleRepository.findByName(PredefinedRole.STUDENT_ROLE))
                .thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(userMapper.toUserResponse(newUser)).thenReturn(testUserResponse);

        // When
        UserResponse response = userService.createUser(request);

        // Then
        assertNotNull(response);
        verify(passwordEncoder, times(1)).encode("512345vsdf");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should create user with phoneNumber '03763423242' successfully")
    void should_createUser_withPhoneNumber03763423242() {
        // Given
        UserCreationRequest request =
                UserCreationRequest.builder()
                        .username("abc")
                        .email("hocsinh@gmail.com")
                        .password("12345678")
                        .firstName("Le")
                        .lastName("Tung")
                        .phoneNumber("03763423242")
                        .roleName(PredefinedRole.STUDENT_ROLE)
                        .build();

        User newUser =
                TestDataBuilder.defaultUser()
                        .username("abc")
                        .email("hocsinh@gmail.com")
                        .phoneNumber("03763423242")
                        .role(testRole)
                        .build();

        when(userMapper.toUser(request)).thenReturn(newUser);
        when(passwordEncoder.encode("12345678")).thenReturn("encodedPassword");
        when(roleRepository.findByName(PredefinedRole.STUDENT_ROLE))
                .thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(userMapper.toUserResponse(newUser)).thenReturn(testUserResponse);

        // When
        UserResponse response = userService.createUser(request);

        // Then
        assertNotNull(response);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should create user with role TUTOR successfully")
    void should_createUser_withRoleTutor() {
        // Given
        Role tutorRole = TestDataBuilder.defaultRole().name(PredefinedRole.TUTOR_ROLE).build();

        UserCreationRequest request =
                UserCreationRequest.builder()
                        .username("abc")
                        .email("hocsinh@gmail.com")
                        .password("12345678")
                        .firstName("Le")
                        .lastName("Tung")
                        .roleName(PredefinedRole.TUTOR_ROLE)
                        .build();

        User newUser =
                TestDataBuilder.defaultUser()
                        .username("abc")
                        .email("hocsinh@gmail.com")
                        .role(tutorRole)
                        .build();

        when(userMapper.toUser(request)).thenReturn(newUser);
        when(passwordEncoder.encode("12345678")).thenReturn("encodedPassword");
        when(roleRepository.findByName(PredefinedRole.TUTOR_ROLE))
                .thenReturn(Optional.of(tutorRole));
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(userMapper.toUserResponse(newUser)).thenReturn(testUserResponse);

        // When
        UserResponse response = userService.createUser(request);

        // Then
        assertNotNull(response);
        verify(roleRepository, times(1)).findByName(PredefinedRole.TUTOR_ROLE);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw ROLE_NOT_EXISTED when role SKDH not found")
    void should_throwRoleNotExisted_whenRoleSkdhNotFound() {
        // Given
        UserCreationRequest request =
                UserCreationRequest.builder()
                        .username("abc")
                        .email("hocsinh@gmail.com")
                        .password("12345678")
                        .firstName("Le")
                        .lastName("Tung")
                        .roleName("SKDH")
                        .build();

        User newUser = TestDataBuilder.defaultUser().username("abc").build();

        when(userMapper.toUser(request)).thenReturn(newUser);
        when(roleRepository.findByName("SKDH")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> userService.createUser(request));

        assertEquals(ErrorCode.ROLE_NOT_EXISTED, exception.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
        verify(emailVerificationService, never()).scheduleVerificationEmail(any(User.class));
    }

    @Test
    @DisplayName("Should create user with all fields and verify password encoding")
    void should_createUser_withAllFieldsAndVerifyPasswordEncoding() {
        // Given
        UserCreationRequest request =
                UserCreationRequest.builder()
                        .username("completeuser")
                        .email("complete@gmail.com")
                        .password("securepass123")
                        .firstName("John")
                        .lastName("Doe")
                        .phoneNumber("09123421112")
                        .address("123 Main St")
                        .dob(java.time.LocalDate.of(1995, 5, 15))
                        .roleName(PredefinedRole.STUDENT_ROLE)
                        .build();

        User newUser =
                TestDataBuilder.defaultUser()
                        .username("completeuser")
                        .email("complete@gmail.com")
                        .firstName("John")
                        .lastName("Doe")
                        .phoneNumber("09123421112")
                        .address("123 Main St")
                        .role(testRole)
                        .build();

        when(userMapper.toUser(request)).thenReturn(newUser);
        when(passwordEncoder.encode("securepass123")).thenReturn("encodedSecurePass");
        when(roleRepository.findByName(PredefinedRole.STUDENT_ROLE))
                .thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(userMapper.toUserResponse(newUser)).thenReturn(testUserResponse);

        // When
        UserResponse response = userService.createUser(request);

        // Then
        assertNotNull(response);
        verify(passwordEncoder, times(1)).encode("securepass123");
        verify(roleRepository, times(1)).findByName(PredefinedRole.STUDENT_ROLE);
        verify(userRepository, times(1)).save(any(User.class));
        verify(emailVerificationService, times(1)).scheduleVerificationEmail(newUser);
    }

    @Test
    @DisplayName("Should create user and verify UserResponse is returned correctly")
    void should_createUser_andReturnUserResponse() {
        // Given
        UserCreationRequest request =
                UserCreationRequest.builder()
                        .username("testuser")
                        .email("test@example.com")
                        .password("password123")
                        .firstName("Test")
                        .lastName("User")
                        .roleName(PredefinedRole.STUDENT_ROLE)
                        .build();

        User savedUser =
                TestDataBuilder.defaultUser()
                        .userId("generated-id")
                        .username("testuser")
                        .email("test@example.com")
                        .role(testRole)
                        .build();

        UserResponse expectedResponse =
                UserResponse.builder()
                        .userId("generated-id")
                        .username("testuser")
                        .email("test@example.com")
                        .build();

        when(userMapper.toUser(request)).thenReturn(savedUser);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(roleRepository.findByName(PredefinedRole.STUDENT_ROLE))
                .thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toUserResponse(savedUser)).thenReturn(expectedResponse);

        // When
        UserResponse response = userService.createUser(request);

        // Then
        assertNotNull(response);
        assertEquals("generated-id", response.getUserId());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        verify(userMapper, times(1)).toUserResponse(savedUser);
    }

    // ==================== COMPREHENSIVE getMyInfo TESTS ====================

    @Test
    @DisplayName("Should get my info successfully when user is logged in - Test Case 1")
    void should_getMyInfo_successfully_whenUserLoggedIn_testCase1() {
        // Given
        MockHelper.mockSecurityContext("currentuser");

        User loggedInUser =
                TestDataBuilder.defaultUser()
                        .userId("current-user-id")
                        .username("currentuser")
                        .email("currentuser@example.com")
                        .firstName("Current")
                        .lastName("User")
                        .role(testRole)
                        .build();

        UserResponse expectedResponse =
                UserResponse.builder()
                        .userId("current-user-id")
                        .username("currentuser")
                        .email("currentuser@example.com")
                        .firstName("Current")
                        .lastName("User")
                        .build();

        when(userRepository.findByUsername("currentuser")).thenReturn(Optional.of(loggedInUser));
        when(userMapper.toUserResponse(loggedInUser)).thenReturn(expectedResponse);

        // When
        UserResponse response = userService.getMyInfo();

        // Then
        assertNotNull(response);
        assertEquals("current-user-id", response.getUserId());
        assertEquals("currentuser", response.getUsername());
        assertEquals("currentuser@example.com", response.getEmail());
        assertEquals("Current", response.getFirstName());
        assertEquals("User", response.getLastName());
        verify(userRepository, times(1)).findByUsername("currentuser");
        verify(userMapper, times(1)).toUserResponse(loggedInUser);
    }

    @Test
    @DisplayName("Should throw USER_NOT_EXISTED when user not found in database - Test Case 2")
    void should_throwUserNotExisted_whenUserNotFoundInDatabase() {
        // Given
        MockHelper.mockSecurityContext("nonexistentuser");

        when(userRepository.findByUsername("nonexistentuser")).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> userService.getMyInfo());

        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(userRepository, times(1)).findByUsername("nonexistentuser");
        verify(userMapper, never()).toUserResponse(any(User.class));
    }

    // ==================== COMPREHENSIVE uploadAvatar TESTS ====================

    @Test
    @DisplayName("Should upload avatar successfully with valid file 'abc' - Test Case 1")
    void should_uploadAvatar_successfully_withFileAbc() {
        // Given
        MockHelper.mockSecurityContext("testuser");
        String avatarUrl = "abc";

        User user =
                TestDataBuilder.defaultUser()
                        .userId("test-user-id")
                        .username("testuser")
                        .email("test@example.com")
                        .role(testRole)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        String result = userService.uploadAvatar(avatarUrl);

        // Then
        assertNotNull(result);
        assertEquals("abc", result);
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should upload avatar successfully with valid file name - Test Case 2")
    void should_uploadAvatar_successfully_withValidFileName() {
        // Given
        MockHelper.mockSecurityContext("testuser");
        String avatarUrl = "https://example.com/avatar.jpg";

        User user =
                TestDataBuilder.defaultUser()
                        .userId("test-user-id")
                        .username("testuser")
                        .email("test@example.com")
                        .role(testRole)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        String result = userService.uploadAvatar(avatarUrl);

        // Then
        assertNotNull(result);
        assertEquals("https://example.com/avatar.jpg", result);
        assertEquals(avatarUrl, user.getAvatar());
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw INVALID_FILE when file is null - Test Case 3")
    void should_throwInvalidFile_whenFileIsNull() {
        // Given
        MockHelper.mockSecurityContext("testuser");

        User user =
                TestDataBuilder.defaultUser()
                        .userId("test-user-id")
                        .username("testuser")
                        .email("test@example.com")
                        .role(testRole)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> userService.uploadAvatar(null));

        assertEquals(ErrorCode.INVALID_FILE, exception.getErrorCode());
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw INVALID_FILE when file is empty string")
    void should_throwInvalidFile_whenFileIsEmpty() {
        // Given
        MockHelper.mockSecurityContext("testuser");

        User user =
                TestDataBuilder.defaultUser()
                        .userId("test-user-id")
                        .username("testuser")
                        .email("test@example.com")
                        .role(testRole)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> userService.uploadAvatar(""));

        assertEquals(ErrorCode.INVALID_FILE, exception.getErrorCode());
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw USER_NOT_EXISTED when user not found during upload")
    void should_throwUserNotExisted_whenUserNotFoundDuringUpload() {
        // Given
        MockHelper.mockSecurityContext("nonexistentuser");
        String avatarUrl = "https://example.com/avatar.jpg";

        when(userRepository.findByUsername("nonexistentuser")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> userService.uploadAvatar(avatarUrl));

        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(userRepository, times(1)).findByUsername("nonexistentuser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should update avatar when user already has an avatar")
    void should_updateAvatar_whenUserAlreadyHasAvatar() {
        // Given
        MockHelper.mockSecurityContext("testuser");
        String oldAvatar = "https://example.com/old-avatar.jpg";
        String newAvatar = "https://example.com/new-avatar.jpg";

        User user =
                TestDataBuilder.defaultUser()
                        .userId("test-user-id")
                        .username("testuser")
                        .email("test@example.com")
                        .avatar(oldAvatar)
                        .role(testRole)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        String result = userService.uploadAvatar(newAvatar);

        // Then
        assertNotNull(result);
        assertEquals(newAvatar, result);
        assertEquals(newAvatar, user.getAvatar());
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(userRepository, times(1)).save(any(User.class));
    }

    // ==================== COMPREHENSIVE updateUser TESTS ====================

    @Test
    @DisplayName("Should update user with new email successfully")
    void should_updateUser_withNewEmail() {
        // Given
        String userId = "test-user-id";
        UserUpdateRequest request =
                UserUpdateRequest.builder().email("newemail@example.com").build();

        User existingUser =
                TestDataBuilder.defaultUser()
                        .userId(userId)
                        .username("testuser")
                        .email("old@example.com")
                        .role(testRole)
                        .build();

        UserResponse updatedResponse =
                UserResponse.builder()
                        .userId(userId)
                        .username("testuser")
                        .email("newemail@example.com")
                        .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(userMapper.toUserResponse(any(User.class))).thenReturn(updatedResponse);
        doNothing().when(userMapper).updateUser(any(User.class), any(UserUpdateRequest.class));

        // When
        UserResponse response = userService.updateUser(userId, request);

        // Then
        assertNotNull(response);
        assertEquals("newemail@example.com", response.getEmail());
        verify(userRepository, times(1)).findById(userId);
        verify(userMapper, times(1)).updateUser(existingUser, request);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should update user with new password successfully")
    void should_updateUser_withNewPassword() {
        // Given
        String userId = "test-user-id";
        String newPassword = "newPassword123";
        UserUpdateRequest request = UserUpdateRequest.builder().password(newPassword).build();

        User existingUser =
                TestDataBuilder.defaultUser()
                        .userId(userId)
                        .username("testuser")
                        .password("oldEncodedPassword")
                        .role(testRole)
                        .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(userMapper.toUserResponse(any(User.class))).thenReturn(testUserResponse);
        doNothing().when(userMapper).updateUser(any(User.class), any(UserUpdateRequest.class));

        // When
        UserResponse response = userService.updateUser(userId, request);

        // Then
        assertNotNull(response);
        verify(passwordEncoder, times(1)).encode(newPassword);
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should update user with firstName and lastName successfully")
    void should_updateUser_withNewFirstNameAndLastName() {
        // Given
        String userId = "test-user-id";
        UserUpdateRequest request =
                UserUpdateRequest.builder()
                        .firstName("NewFirstName")
                        .lastName("NewLastName")
                        .build();

        User existingUser =
                TestDataBuilder.defaultUser()
                        .userId(userId)
                        .username("testuser")
                        .firstName("OldFirst")
                        .lastName("OldLast")
                        .role(testRole)
                        .build();

        UserResponse updatedResponse =
                UserResponse.builder()
                        .userId(userId)
                        .username("testuser")
                        .firstName("NewFirstName")
                        .lastName("NewLastName")
                        .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(userMapper.toUserResponse(any(User.class))).thenReturn(updatedResponse);
        doNothing().when(userMapper).updateUser(any(User.class), any(UserUpdateRequest.class));

        // When
        UserResponse response = userService.updateUser(userId, request);

        // Then
        assertNotNull(response);
        assertEquals("NewFirstName", response.getFirstName());
        assertEquals("NewLastName", response.getLastName());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should update user with new phoneNumber successfully")
    void should_updateUser_withNewPhoneNumber() {
        // Given
        String userId = "test-user-id";
        UserUpdateRequest request = UserUpdateRequest.builder().phoneNumber("09876543210").build();

        User existingUser =
                TestDataBuilder.defaultUser()
                        .userId(userId)
                        .username("testuser")
                        .phoneNumber("01234567890")
                        .role(testRole)
                        .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(userMapper.toUserResponse(any(User.class))).thenReturn(testUserResponse);
        doNothing().when(userMapper).updateUser(any(User.class), any(UserUpdateRequest.class));

        // When
        UserResponse response = userService.updateUser(userId, request);

        // Then
        assertNotNull(response);
        verify(userRepository, times(1)).findById(userId);
        verify(userMapper, times(1)).updateUser(existingUser, request);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should update user with new address successfully")
    void should_updateUser_withNewAddress() {
        // Given
        String userId = "test-user-id";
        UserUpdateRequest request =
                UserUpdateRequest.builder().address("123 New Street, New City").build();

        User existingUser =
                TestDataBuilder.defaultUser()
                        .userId(userId)
                        .username("testuser")
                        .address("456 Old Street")
                        .role(testRole)
                        .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(userMapper.toUserResponse(any(User.class))).thenReturn(testUserResponse);
        doNothing().when(userMapper).updateUser(any(User.class), any(UserUpdateRequest.class));

        // When
        UserResponse response = userService.updateUser(userId, request);

        // Then
        assertNotNull(response);
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should update user role to TUTOR successfully")
    void should_updateUser_withNewRoleTutor() {
        // Given
        String userId = "test-user-id";
        Role tutorRole = TestDataBuilder.defaultRole().name(PredefinedRole.TUTOR_ROLE).build();
        UserUpdateRequest request =
                UserUpdateRequest.builder().roleName(PredefinedRole.TUTOR_ROLE).build();

        User existingUser =
                TestDataBuilder.defaultUser()
                        .userId(userId)
                        .username("testuser")
                        .role(testRole)
                        .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(roleRepository.findByName(PredefinedRole.TUTOR_ROLE))
                .thenReturn(Optional.of(tutorRole));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(userMapper.toUserResponse(any(User.class))).thenReturn(testUserResponse);
        doNothing().when(userMapper).updateUser(any(User.class), any(UserUpdateRequest.class));

        // When
        UserResponse response = userService.updateUser(userId, request);

        // Then
        assertNotNull(response);
        verify(roleRepository, times(1)).findByName(PredefinedRole.TUTOR_ROLE);
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw USER_NOT_EXISTED when user not found during update")
    void should_throwUserNotExisted_whenUserNotFoundDuringUpdate() {
        // Given
        String userId = "non-existent-id";
        UserUpdateRequest request = UserUpdateRequest.builder().email("new@example.com").build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> userService.updateUser(userId, request));

        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw ROLE_NOT_EXISTED when role not found during update")
    void should_throwRoleNotExisted_whenRoleNotFoundDuringUpdate() {
        // Given
        String userId = "test-user-id";
        UserUpdateRequest request = UserUpdateRequest.builder().roleName("INVALID_ROLE").build();

        User existingUser =
                TestDataBuilder.defaultUser()
                        .userId(userId)
                        .username("testuser")
                        .role(testRole)
                        .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(roleRepository.findByName("INVALID_ROLE")).thenReturn(Optional.empty());
        doNothing().when(userMapper).updateUser(any(User.class), any(UserUpdateRequest.class));

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> userService.updateUser(userId, request));

        assertEquals(ErrorCode.ROLE_NOT_EXISTED, exception.getErrorCode());
        verify(userRepository, times(1)).findById(userId);
        verify(roleRepository, times(1)).findByName("INVALID_ROLE");
        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== GET USERS TEST CASES ====================

    @Test
    @DisplayName("GU01 - Should get users with default pagination successfully")
    void should_getUsers_withDefaultPagination() {
        // Given
        User user1 =
                TestDataBuilder.defaultUser()
                        .userId("user-1")
                        .username("user1")
                        .email("user1@test.com")
                        .role(testRole)
                        .build();

        User user2 =
                TestDataBuilder.defaultUser()
                        .userId("user-2")
                        .username("user2")
                        .email("user2@test.com")
                        .role(testRole)
                        .build();

        UserResponse response1 =
                UserResponse.builder()
                        .userId("user-1")
                        .username("user1")
                        .email("user1@test.com")
                        .build();

        UserResponse response2 =
                UserResponse.builder()
                        .userId("user-2")
                        .username("user2")
                        .email("user2@test.com")
                        .build();

        List<User> users = Arrays.asList(user1, user2);
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "username"));
        Page<User> userPage = new PageImpl<>(users, pageable, 2);

        when(userRepository.searchUsers(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(userPage);
        when(userMapper.toUserResponse(user1)).thenReturn(response1);
        when(userMapper.toUserResponse(user2)).thenReturn(response2);

        // When
        Page<UserResponse> result = userService.getUsers(0, 10, "username", "asc", null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals("user1", result.getContent().get(0).getUsername());
        assertEquals("user2", result.getContent().get(1).getUsername());
        verify(userRepository).searchUsers(any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("GU02 - Should get users with DESC sorting")
    void should_getUsers_withDescSorting() {
        // Given
        User user1 =
                TestDataBuilder.defaultUser()
                        .userId("user-1")
                        .username("user1")
                        .role(testRole)
                        .build();

        User user2 =
                TestDataBuilder.defaultUser()
                        .userId("user-2")
                        .username("user2")
                        .role(testRole)
                        .build();

        UserResponse response1 = UserResponse.builder().userId("user-1").username("user1").build();

        UserResponse response2 = UserResponse.builder().userId("user-2").username("user2").build();

        List<User> users = Arrays.asList(user2, user1); // Reversed order
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "username"));
        Page<User> userPage = new PageImpl<>(users, pageable, 2);

        when(userRepository.searchUsers(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(userPage);
        when(userMapper.toUserResponse(user1)).thenReturn(response1);
        when(userMapper.toUserResponse(user2)).thenReturn(response2);

        // When
        Page<UserResponse> result = userService.getUsers(0, 10, "username", "desc", null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals("user2", result.getContent().get(0).getUsername());
        assertEquals("user1", result.getContent().get(1).getUsername());
        verify(userRepository).searchUsers(any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("GU03 - Should get users with custom page size")
    void should_getUsers_withCustomPageSize() {
        // Given
        User user1 =
                TestDataBuilder.defaultUser()
                        .userId("user-1")
                        .username("user1")
                        .role(testRole)
                        .build();

        User user2 =
                TestDataBuilder.defaultUser()
                        .userId("user-2")
                        .username("user2")
                        .role(testRole)
                        .build();

        User user3 =
                TestDataBuilder.defaultUser()
                        .userId("user-3")
                        .username("user3")
                        .role(testRole)
                        .build();

        List<User> users = Arrays.asList(user1, user2, user3);
        Pageable pageable = PageRequest.of(0, 3, Sort.by(Sort.Direction.ASC, "username"));
        Page<User> userPage = new PageImpl<>(users, pageable, 3);

        when(userRepository.searchUsers(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(userPage);

        // When
        Page<UserResponse> result = userService.getUsers(0, 3, "username", "asc", null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        assertEquals(3, result.getContent().size());
        verify(userRepository).searchUsers(any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("GU04 - Should get second page of users")
    void should_getUsers_secondPage() {
        // Given
        User user3 =
                TestDataBuilder.defaultUser()
                        .userId("user-3")
                        .username("user3")
                        .role(testRole)
                        .build();

        User user4 =
                TestDataBuilder.defaultUser()
                        .userId("user-4")
                        .username("user4")
                        .role(testRole)
                        .build();

        List<User> users = Arrays.asList(user3, user4);
        Pageable pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.ASC, "username"));
        Page<User> userPage = new PageImpl<>(users, pageable, 4);

        when(userRepository.searchUsers(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(userPage);

        // When
        Page<UserResponse> result = userService.getUsers(1, 2, "username", "asc", null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(4, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals(1, result.getNumber()); // Current page number
        assertEquals(2, result.getTotalPages());
        verify(userRepository).searchUsers(any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("GU05 - Should get empty page when no users exist")
    void should_getUsers_emptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "username"));
        Page<User> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(userRepository.searchUsers(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(emptyPage);

        // When
        Page<UserResponse> result = userService.getUsers(0, 10, "username", "asc", null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(userRepository).searchUsers(any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("GU06 - Should get users sorted by email")
    void should_getUsers_sortedByEmail() {
        // Given
        User user1 =
                TestDataBuilder.defaultUser()
                        .userId("user-1")
                        .username("user1")
                        .email("alice@test.com")
                        .role(testRole)
                        .build();

        User user2 =
                TestDataBuilder.defaultUser()
                        .userId("user-2")
                        .username("user2")
                        .email("bob@test.com")
                        .role(testRole)
                        .build();

        UserResponse response1 =
                UserResponse.builder()
                        .userId("user-1")
                        .username("user1")
                        .email("alice@test.com")
                        .build();

        UserResponse response2 =
                UserResponse.builder()
                        .userId("user-2")
                        .username("user2")
                        .email("bob@test.com")
                        .build();

        List<User> users = Arrays.asList(user1, user2);
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "email"));
        Page<User> userPage = new PageImpl<>(users, pageable, 2);

        when(userRepository.searchUsers(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(userPage);
        when(userMapper.toUserResponse(user1)).thenReturn(response1);
        when(userMapper.toUserResponse(user2)).thenReturn(response2);

        // When
        Page<UserResponse> result = userService.getUsers(0, 10, "email", "asc", null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals("alice@test.com", result.getContent().get(0).getEmail());
        assertEquals("bob@test.com", result.getContent().get(1).getEmail());
        verify(userRepository).searchUsers(any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("GU07 - Should get users sorted by firstName")
    void should_getUsers_sortedByFirstName() {
        // Given
        User user1 =
                TestDataBuilder.defaultUser()
                        .userId("user-1")
                        .username("user1")
                        .firstName("Alice")
                        .role(testRole)
                        .build();

        User user2 =
                TestDataBuilder.defaultUser()
                        .userId("user-2")
                        .username("user2")
                        .firstName("Bob")
                        .role(testRole)
                        .build();

        UserResponse response1 =
                UserResponse.builder()
                        .userId("user-1")
                        .username("user1")
                        .firstName("Alice")
                        .build();

        UserResponse response2 =
                UserResponse.builder().userId("user-2").username("user2").firstName("Bob").build();

        List<User> users = Arrays.asList(user1, user2);
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "firstName"));
        Page<User> userPage = new PageImpl<>(users, pageable, 2);

        when(userRepository.searchUsers(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(userPage);
        when(userMapper.toUserResponse(user1)).thenReturn(response1);
        when(userMapper.toUserResponse(user2)).thenReturn(response2);

        // When
        Page<UserResponse> result = userService.getUsers(0, 10, "firstName", "asc", null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals("Alice", result.getContent().get(0).getFirstName());
        assertEquals("Bob", result.getContent().get(1).getFirstName());
        verify(userRepository).searchUsers(any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("GU08 - Should get users with page size of 1")
    void should_getUsers_withPageSize1() {
        // Given
        User user1 =
                TestDataBuilder.defaultUser()
                        .userId("user-1")
                        .username("user1")
                        .role(testRole)
                        .build();

        List<User> users = Arrays.asList(user1);
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "username"));
        Page<User> userPage = new PageImpl<>(users, pageable, 5);

        when(userRepository.searchUsers(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(userPage);

        // When
        Page<UserResponse> result = userService.getUsers(0, 1, "username", "asc", null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(5, result.getTotalPages()); // 5 total elements / 1 per page = 5 pages
        verify(userRepository).searchUsers(any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("GU09 - Should get users with case insensitive sort direction")
    void should_getUsers_withCaseInsensitiveSortDirection() {
        // Given - Test with "DESC" in uppercase
        User user1 =
                TestDataBuilder.defaultUser()
                        .userId("user-1")
                        .username("user1")
                        .role(testRole)
                        .build();

        List<User> users = Arrays.asList(user1);
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "username"));
        Page<User> userPage = new PageImpl<>(users, pageable, 1);

        when(userRepository.searchUsers(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(userPage);

        // When
        Page<UserResponse> result = userService.getUsers(0, 10, "username", "DESC", null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userRepository).searchUsers(any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("GU10 - Should default to ASC when invalid sort direction provided")
    void should_getUsers_defaultToAscForInvalidDirection() {
        // Given - Test with invalid direction defaults to ASC
        User user1 =
                TestDataBuilder.defaultUser()
                        .userId("user-1")
                        .username("user1")
                        .role(testRole)
                        .build();

        List<User> users = Arrays.asList(user1);
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "username"));
        Page<User> userPage = new PageImpl<>(users, pageable, 1);

        when(userRepository.searchUsers(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(userPage);

        // When
        Page<UserResponse> result = userService.getUsers(0, 10, "username", "invalid", null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userRepository).searchUsers(any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("GU11 - Should get users with all user fields populated")
    void should_getUsers_withAllFieldsPopulated() {
        // Given
        User user1 =
                TestDataBuilder.defaultUser()
                        .userId("user-1")
                        .username("user1")
                        .email("user1@test.com")
                        .firstName("John")
                        .lastName("Doe")
                        .phoneNumber("1234567890")
                        .avatar("avatar.jpg")
                        .role(testRole)
                        .build();

        UserResponse response1 =
                UserResponse.builder()
                        .userId("user-1")
                        .username("user1")
                        .email("user1@test.com")
                        .firstName("John")
                        .lastName("Doe")
                        .build();

        List<User> users = Arrays.asList(user1);
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "username"));
        Page<User> userPage = new PageImpl<>(users, pageable, 1);

        when(userRepository.searchUsers(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(userPage);
        when(userMapper.toUserResponse(user1)).thenReturn(response1);

        // When
        Page<UserResponse> result = userService.getUsers(0, 10, "username", "asc", null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        UserResponse userResponse = result.getContent().get(0);
        assertEquals("user-1", userResponse.getUserId());
        assertEquals("user1", userResponse.getUsername());
        assertEquals("user1@test.com", userResponse.getEmail());
        assertEquals("John", userResponse.getFirstName());
        assertEquals("Doe", userResponse.getLastName());
        verify(userRepository).searchUsers(any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("GU12 - Should get users with large page size")
    void should_getUsers_withLargePageSize() {
        // Given
        User user1 =
                TestDataBuilder.defaultUser()
                        .userId("user-1")
                        .username("user1")
                        .role(testRole)
                        .build();

        List<User> users = Arrays.asList(user1);
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "username"));
        Page<User> userPage = new PageImpl<>(users, pageable, 1);

        when(userRepository.searchUsers(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(userPage);

        // When
        Page<UserResponse> result = userService.getUsers(0, 100, "username", "asc", null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages()); // Only 1 page needed
        verify(userRepository).searchUsers(any(), any(), any(), any(), any(), any(Pageable.class));
    }
}
