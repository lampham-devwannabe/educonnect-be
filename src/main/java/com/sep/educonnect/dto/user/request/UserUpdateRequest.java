package com.sep.educonnect.dto.user.request;

import com.sep.educonnect.validator.DobConstraint;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {
    String email;
    String password;
    String firstName;
    String lastName;
    String phoneNumber;
    String address;

    @DobConstraint(min = 18, message = "INVALID_DOB")
    LocalDate dob;

    String loginType;
    String roleName;
    String avatar;
}
