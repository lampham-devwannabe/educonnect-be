package com.sep.educonnect.service;

import com.amazonaws.services.cloudfront.CloudFrontUrlSigner;
import com.sep.educonnect.configuration.CloudfrontKeyLoader;
import com.sep.educonnect.dto.file.S3FileResponse;
import com.sep.educonnect.enums.AccessType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private static final int BUFFER_SIZE = 16 * 1024; // 16KB
    private static final String PUBLIC_PREFIX = "public/";
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${s3.bucket-name}")
    private String bucketName;

    @Value("${cdn.domain}")
    private String cdnDomain;

    @Value("${s3.presign-period-minutes:60}")
    private long preSignPeriodMinutes;

    @Value("${s3.region}")
    private String region;

    @Value("${cdn.key}")
    private String keyPairId;

    @Value("${video.s3.base-prefix:videos/lessons}")
    private String videoBasePrefix;

    /** Tạo fileId unique bằng cách thêm timestamp và sanitize tên file */
    public String buildFileId(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        return String.format("%s_%s", System.currentTimeMillis(), sanitizeFileName(filename));
    }

    /** Làm sạch tên file: loại bỏ ký tự đặc biệt, chuẩn hóa Unicode */
    private String sanitizeFileName(String fileName) {
        String normalizedFileName = Normalizer.normalize(fileName, Normalizer.Form.NFKD);
        return normalizedFileName.replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9.\\-_]", "");
    }

    /** Tạo URL public để truy cập file qua CDN */
    public String getPublicLink(String fileName) {
        if (cdnDomain == null || cdnDomain.isEmpty()) {
            log.warn("CDN domain not configured, returning S3 direct URL");
            return String.format("https://%s.s3.amazonaws.com/%s", bucketName, fileName);
        }
        return cdnDomain + "/" + fileName;
    }

    /** Tạo presigned URL cho GET (download) hoặc PUT (upload) */
    public S3FileResponse generatePreSignedUrl(
            String fileId, SdkHttpMethod method, AccessType accessType) throws Exception {
        if (fileId == null || fileId.trim().isEmpty()) {
            throw new IllegalArgumentException("FileId cannot be null or empty");
        }

        if (method == SdkHttpMethod.GET) {
            // GET: sử dụng fileId gốc (không tạo mới)
            return generateGetPreSignedUrl(fileId, accessType);
        } else if (method == SdkHttpMethod.PUT) {
            // PUT: tạo fileId mới để tránh trùng lặp
            String newFileId = buildFileId(fileId);

            log.info("New file id: {}", newFileId);
            return generatePutPresignedUrl(newFileId, accessType);
        } else {
            throw new UnsupportedOperationException("Unsupported HTTP method: " + method);
        }
    }

    /** Tạo presigned URL cho download (GET) */
    private S3FileResponse generateGetPreSignedUrl(String filePath, AccessType accessType)
            throws Exception {

        if (AccessType.PUBLIC.equals(accessType)) {
            return S3FileResponse.builder().fileId(filePath).url(getPublicLink(filePath)).build();
        }

        String signedUrl = cdnSign(filePath);
        log.info("Generated GET presigned URL for file: {}", filePath);
        return S3FileResponse.builder().fileId(filePath).url(signedUrl).build();
    }

    /** Tạo signed URL cho CloudFront */
    private String cdnSign(String objectKey) throws Exception {
        PrivateKey privateKey = CloudfrontKeyLoader.load();
        String resourceUrl = cdnDomain + "/" + objectKey;
        Date expiresOn = Date.from(Instant.now().plusSeconds(60 * 10)); // 10 phút

        return CloudFrontUrlSigner.getSignedURLWithCannedPolicy(
                resourceUrl, keyPairId, privateKey, expiresOn);
    }

    /** Tạo presigned URL cho upload (PUT) */
    private S3FileResponse generatePutPresignedUrl(String filePath, AccessType accessType) {
        filePath = AccessType.PUBLIC.equals(accessType) ? PUBLIC_PREFIX + filePath : filePath;

        PutObjectRequest.Builder putObjectRequestBuilder =
                PutObjectRequest.builder().bucket(bucketName).key(filePath);

        if (AccessType.PRIVATE.equals(accessType)) {
            putObjectRequestBuilder.acl(ObjectCannedACL.PRIVATE);
        }

        if (AccessType.PUBLIC.equals(accessType)) {
            putObjectRequestBuilder.acl(ObjectCannedACL.PUBLIC_READ);
        }

        PutObjectRequest putObjectRequest = putObjectRequestBuilder.build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(preSignPeriodMinutes))
                        .putObjectRequest(putObjectRequest)
                        .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        log.info("Generated PUT presigned URL for file: {}", filePath);
        return S3FileResponse.builder()
                .fileId(filePath)
                .url(presignedRequest.url().toString())
                .build();
    }

    public String uploadFile(MultipartFile file, AccessType accessType) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        String fileId = buildFileId(file.getOriginalFilename());
        log.info("Uploading file: {} with size: {} bytes", fileId, file.getSize());

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest.Builder putObjectRequestBuilder =
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(fileId)
                            .contentType(file.getContentType());

            PutObjectRequest putObjectRequest = putObjectRequestBuilder.build();

            s3Client.putObject(
                    putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));

            log.info("Successfully uploaded file: {}", fileId);
            return fileId;
        } catch (Exception e) {
            log.error("Error uploading file: {}", fileId, e);
            throw new IOException("Failed to upload file to S3", e);
        }
    }

    /** Kiểm tra file có tồn tại trong S3 không */
    public boolean fileExists(String fileId) {
        if (fileId == null || fileId.trim().isEmpty()) {
            return false;
        }

        try {
            HeadObjectRequest headObjectRequest =
                    HeadObjectRequest.builder().bucket(bucketName).key(fileId).build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            log.error("Error checking file existence: {}", fileId, e);
            throw new RuntimeException("Failed to check file existence: " + fileId, e);
        }
    }

    // ===================== VIDEO METHODS =====================

    public String getVideoBasePrefix() {
        return videoBasePrefix;
    }

    public String buildVideoOriginalKey(Long videoLessonId) {
        return String.format("%s/%d/original.mp4", videoBasePrefix, videoLessonId);
    }

    public String buildVideoManifestKey(Long videoLessonId) {
        return String.format("%s/%d/master.m3u8", videoBasePrefix, videoLessonId);
    }

    public String buildVideoVariantKey(Long videoLessonId, String variantFileName) {
        return String.format("%s/%d/%s", videoBasePrefix, videoLessonId, variantFileName);
    }

    /** Presigned URL PUT để upload MP4 gốc */
    public S3FileResponse generateVideoUploadUrl(Long videoLessonId) {
        String key = buildVideoOriginalKey(videoLessonId);
        return generatePutPresignedUrl(key, AccessType.PRIVATE);
    }

    /**
     * CloudFront signed URL cho HLS manifest (master.m3u8) - DEPRECATED: dùng getHlsSignedCookies
     * thay thế
     */
    public String getHlsManifestSignedUrl(Long videoLessonId) {
        String manifestKey = buildVideoManifestKey(videoLessonId);
        return getPublicLink(manifestKey);
    }

    /**
     * Tạo CloudFront signed cookies cho HLS streaming Cookies này sẽ áp dụng cho tất cả files trong
     * thư mục video lesson (master.m3u8, variant.m3u8, *.ts)
     *
     * @param videoLessonId ID của video lesson
     * @return Map chứa 3 cookies: CloudFront-Policy, CloudFront-Signature, CloudFront-Key-Pair-Id
     */
    public Map<String, String> getHlsSignedCookies(Long videoLessonId) throws Exception {
        PrivateKey privateKey = CloudfrontKeyLoader.load();
        String resourcePath = "/" + String.format("%s/%d/*", videoBasePrefix, videoLessonId);

        // Custom policy: cho phép truy cập tất cả files trong thư mục này
        Date expiresOn = Date.from(Instant.now().plusSeconds(60 * 60)); // 1 giờ
        long expiresEpoch = expiresOn.getTime() / 1000;

        // Tạo custom policy JSON với wildcard path
        String resourceUrl = cdnDomain + resourcePath;
        String policyJson =
                String.format(
                        "{\"Statement\":[{\"Resource\":\"%s\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":%d}}}]}",
                        resourceUrl, expiresEpoch);

        log.info("Generating CloudFront signed cookies for resource: {}", resourceUrl);

        // Base64 encode policy
        String policyBase64 =
                Base64.getEncoder()
                        .encodeToString(policyJson.getBytes(StandardCharsets.UTF_8))
                        .replace("+", "-")
                        .replace("=", "_")
                        .replace("/", "~");

        // Sign policy với RSA-SHA1
        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initSign(privateKey);
        signature.update(policyJson.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = signature.sign();
        String signatureBase64 =
                Base64.getEncoder()
                        .encodeToString(signatureBytes)
                        .replace("+", "-")
                        .replace("=", "_")
                        .replace("/", "~");

        // Tạo cookies map
        Map<String, String> cookies = new HashMap<>();
        cookies.put("CloudFront-Policy", policyBase64);
        cookies.put("CloudFront-Signature", signatureBase64);
        cookies.put("CloudFront-Key-Pair-Id", keyPairId);

        return cookies;
    }

    /** Lấy manifest URL không ký (dùng với signed cookies) */
    public String getHlsManifestUrl(Long videoLessonId) {
        String manifestKey = buildVideoManifestKey(videoLessonId);
        return getPublicLink(manifestKey);
    }

    /** Kiểm tra HLS đã sẵn sàng (master.m3u8 tồn tại) */
    public boolean hlsFilesExist(Long videoLessonId) {
        return fileExists(buildVideoManifestKey(videoLessonId));
    }

    /** Xóa toàn bộ files của một video lesson (placeholder) */
    public void deleteVideoLessonFiles(Long videoLessonId) {
        String prefix = String.format("%s/%d/", videoBasePrefix, videoLessonId);
        log.info(
                "Request to delete objects with prefix: {} (implement list+delete if needed)",
                prefix);
    }
}
