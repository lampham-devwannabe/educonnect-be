package com.sep.educonnect.dto.booking;

import com.sep.educonnect.enums.BookingMemberStatus;
import com.sep.educonnect.enums.BookingStatus;
import com.sep.educonnect.enums.CourseStatus;
import com.sep.educonnect.enums.CourseType;
import com.sep.educonnect.enums.GroupType;
import com.sep.educonnect.enums.RegistrationType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingResponse {
    Long id;
    BookingStatus bookingStatus;
    RegistrationType registrationType;
    GroupType groupType;
    BigDecimal totalAmount;
    String scheduleDescription;
    CourseInfo course;
    List<MemberInfo> members;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CourseInfo {
        Long id;
        String name;
        CourseType type;
        CourseStatus status;
        BigDecimal price;
        Boolean combo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MemberInfo {
        String userId;
        String name;
        String role;
        BookingMemberStatus status;
    }
}

