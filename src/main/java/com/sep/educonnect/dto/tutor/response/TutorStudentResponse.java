package com.sep.educonnect.dto.tutor.response;

import com.sep.educonnect.dto.subject.response.SubjectResponse;
import com.sep.educonnect.entity.Tag;
import com.sep.educonnect.enums.CurrencyCode;
import com.sep.educonnect.enums.ProfileStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TutorStudentResponse {
    Long id;
    String bioEn;
    String bioVi;
    String experience;
    Set<SubjectResponse> subjects;
    BigDecimal hourlyRate;
    CurrencyCode currencyCode;
    ProfileStatus submissionStatus;
    String descEn;
    String descVi;
    String rejectionReason;
    String videoLink;
    Integer studentCount;
    Integer reviewCount;
    Double rating;
    LocalDateTime reviewedAt;
    Set<Tag> tags;

    // Like status
    Boolean isLikedByCurrentStudent;

    // User fields (thông tin cơ bản)
    String userId;
    String email;
    String firstName;
    String lastName;
    String phoneNumber;
    String address;
    LocalDate dob;
    String avatar;

    // Metadata
    String createdBy;
    LocalDateTime createdAt;
    String modifiedBy;
    LocalDateTime modifiedAt;
    Boolean isDeleted;
}
