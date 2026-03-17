package com.sep.educonnect.wrapper;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public record S3ObjectInputStreamWrapper(
        ResponseInputStream<GetObjectResponse> inputStream,
        String eTag,
        String contentType
) {
    /**
     * Constructor với 2 tham số (backward compatibility)
     */
    public S3ObjectInputStreamWrapper(ResponseInputStream<GetObjectResponse> inputStream, String eTag) {
        this(inputStream, eTag, null);
    }
}
