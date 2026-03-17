package com.sep.educonnect.dto.tutor.response;

import com.sep.educonnect.dto.user.response.UserResponse;
import com.sep.educonnect.entity.Subject;
import com.sep.educonnect.entity.Tag;
import com.sep.educonnect.enums.ProfileStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TutorProfileResponse {
    Long id;
    UserResponse tutor;
    String videoLink;
    String bioEn;
    String bioVi;
    String descVi;
    String descEn;
    ProfileStatus profileStatus;
    Integer reviewCount;
    BigDecimal hourlyRate;
    LocalDateTime reviewedAt;
    UserResponse reviewedBy;
    Integer studentCount;
    String experience;
    Set<Subject> subjects;
    Set<Tag> tags;
    List<ProfileDocumentResponse> documents;
}
