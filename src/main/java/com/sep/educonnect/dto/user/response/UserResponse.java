package com.sep.educonnect.dto.user.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    private String userId;
    String username;
    String email;
    String firstName;
    String lastName;
    String phoneNumber;
    String address;
    LocalDate dob;
    String roleName;
    String loginType;
    Boolean emailVerified;
    LocalDateTime emailVerifiedAt;
    String createdBy;
    LocalDateTime createdAt;
    LocalDateTime modifiedAt;
    String modifiedBy;
    Boolean isDeleted;
    Integer point;
    String avatar;
}
