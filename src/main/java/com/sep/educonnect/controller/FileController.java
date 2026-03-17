package com.sep.educonnect.controller;

import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.file.S3FileResponse;
import com.sep.educonnect.enums.AccessType;
import com.sep.educonnect.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.http.SdkHttpMethod;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class FileController {
    S3Service s3Service;

    @PostMapping("/get-upload-url")
    public ApiResponse<S3FileResponse> upload(
            @RequestParam(name = "file-id") String fileId,
            @RequestParam(name = "access-type") String accessType
    ) throws Exception {
        S3FileResponse result = s3Service.generatePreSignedUrl(fileId, SdkHttpMethod.PUT, AccessType.valueOf(accessType));
        return ApiResponse.<S3FileResponse>builder()
                .result(result)
                .build();
    }

    @GetMapping("/get-download-url")
    public ApiResponse<S3FileResponse> getDownloadUrl(
            @RequestParam("file-id") String fileId,
            @RequestParam(name = "access-type") String accessType
    ) throws Exception {
        S3FileResponse result = s3Service.generatePreSignedUrl(fileId, SdkHttpMethod.GET, AccessType.valueOf(accessType));
        return ApiResponse.<S3FileResponse>builder()
                .result(result)
                .build();
    }


}
