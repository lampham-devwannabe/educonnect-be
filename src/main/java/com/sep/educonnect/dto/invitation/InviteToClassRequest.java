package com.sep.educonnect.dto.invitation;

import lombok.Data;

import java.util.List;

@Data
public class InviteToClassRequest {
    Long classId;
    List<String> studentIds;
}
