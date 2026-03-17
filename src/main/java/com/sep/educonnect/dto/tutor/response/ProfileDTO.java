package com.sep.educonnect.dto.tutor.response;

import com.sep.educonnect.entity.ReviewComment;
import com.sep.educonnect.entity.TutorProfile;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileDTO {
    TutorProfile tutorProfile;
    List<ReviewComment> comment;
}
