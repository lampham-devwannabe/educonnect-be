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
public class StreamingUrlResponse {
    private String manifestUrl;
    private Long expiresInSeconds;
    private Map<String, String> cookies; // CloudFront signed cookies: CloudFront-Policy, CloudFront-Signature,
                                         // CloudFront-Key-Pair-Id
}
