package com.sep.educonnect.dto.video;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamingInfo {
    private String manifestUrl;
    private Map<String, String> cookies; // CloudFront signed cookies
    private Long expiresInSeconds;
}
