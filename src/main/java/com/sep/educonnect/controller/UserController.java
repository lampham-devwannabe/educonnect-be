package com.sep.educonnect.controller;

import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.user.request.ChangePasswordRequest;
import com.sep.educonnect.dto.user.request.UserCreationRequest;
import com.sep.educonnect.dto.user.request.UserUpdateRequest;
import com.sep.educonnect.dto.user.response.ChangePasswordResponse;
import com.sep.educonnect.dto.user.response.UserResponse;
import com.sep.educonnect.service.I18nService;
import com.sep.educonnect.service.UserService;

import jakarta.validation.Valid;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {
    UserService userService;
    I18nService i18nService;

    @PostMapping
    ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.createUser(request))
                .build();
    }

    @GetMapping
    ApiResponse<Page<UserResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "username") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String email) {
        return ApiResponse.<Page<UserResponse>>builder()
                .result(userService.getUsers(page, size, sortBy, direction, firstName, lastName,
                        phoneNumber, roleName, email))
                .build();
    }

    @GetMapping("/{userId}")
    ApiResponse<UserResponse> getUser(@PathVariable("userId") String userId) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUser(userId))
                .build();
    }

    @GetMapping("/my-info")
    ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    @DeleteMapping("/{userId}")
    ApiResponse<String> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ApiResponse.<String>builder()
                .result(i18nService.msg("msg.user.delete"))
                .build();
    }

    @PutMapping("/{userId}")
    ApiResponse<UserResponse> updateUser(@PathVariable String userId, @RequestBody UserUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateUser(userId, request))
                .build();
    }

    @PostMapping("/upload-avatar")
    public ApiResponse<String> uploadAvatar(
            @RequestParam("file-id") String file) {
        String avatarUrl = userService.uploadAvatar(file);
        return ApiResponse.<String>builder()
                .result(avatarUrl)
                .build();
    }

    @PostMapping("/change-password")
    public ApiResponse<ChangePasswordResponse> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        return ApiResponse.<ChangePasswordResponse>builder()
                .result(userService.changePassword(request))
                .build();
    }
}
