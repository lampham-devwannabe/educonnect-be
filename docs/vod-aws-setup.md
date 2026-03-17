# Hướng dẫn cấu hình S3 Event và CloudFront cho VOD HLS

Tài liệu này mô tả các bước cấu hình trên AWS để hệ thống upload → transcode → stream hoạt động.

## 1) Chuẩn bị

- S3 bucket: `educonnect-bucket` (hoặc tên bucket của bạn)
- Lambda: `educonnect-video-transcoder` (đã tạo bằng `lambda/deploy.sh`)
- CloudFront: tạo mới Distribution để CDN các file HLS trong S3

---

## 2) Cấu hình CloudFront (private content)

Mục tiêu: chỉ phát HLS qua CloudFront bằng signed URL; S3 không public objects.

### 2.1 Tạo Origin Access Control (khuyến nghị) hoặc OAI (legacy)

- Vào CloudFront → Security → Origin access → Create origin access control (OAC)
  - Signing behavior: Sign requests
  - Origin type: S3
- Gắn OAC vào Origin S3 của Distribution khi tạo (hoặc update sau đó)
- Áp dụng policy cho bucket (CloudFront sẽ đề xuất policy); xác nhận bucket chỉ cho phép CloudFront truy cập.

### 2.2 Tạo Distribution

- Origin: S3 bucket `educonnect-bucket`
- Attach OAC ở bước trên
- Behaviors:
  - Path pattern: `videos/lessons/*`
  - Allowed methods: `GET, HEAD`
  - Cache policy: CachingOptimized (hoặc custom)
    - TTL gợi ý: m3u8: `MinTTL 0`, `DefaultTTL 60`; ts: `DefaultTTL 86400`
  - Viewer protocol: Redirect HTTP to HTTPS
- Error responses: cho phép pass-through 403/404 (debug dễ hơn)
- Geo restrictions: None
- Price class: tùy ngân sách

### 2.3 CloudFront Signed URL

- Tạo CloudFront Key Pair (hoặc dùng KeyGroup + public key)
- Lưu `KEY_PAIR_ID` và private key (dạng PEM) cho backend (được `CloudfrontKeyLoader` nạp)
- Đặt biến môi trường backend:
  - `CDN_DOMAIN` = domain CloudFront, ví dụ: `https://dxxxxx.cloudfront.net`
  - `KEY_PAIR_ID` = id của key pair

---

## 3) Cấu hình S3 Event trigger Lambda

Lambda cần được gọi khi file `original.mp4` được upload vào đúng thư mục.

### 3.1 Quy ước đường dẫn

- MP4 gốc: `videos/lessons/{videoLessonId}/original.mp4`
- HLS output: `videos/lessons/{videoLessonId}/...`

### 3.2 Thiết lập Event Notifications (Console)

- Vào S3 bucket → Properties → Event notifications → Create
  - Name: `video-transcode-trigger`
  - Event types: `All object create events`
  - Prefix: `videos/lessons/`
  - Suffix: `original.mp4`
  - Destination: Lambda function → chọn `educonnect-video-transcoder`
- Xác nhận S3 add-permission cho Lambda (Console sẽ tự tạo policy `AWSLambdaS3ExecutionRole`).

### 3.3 Thiết lập bằng CLI (tuỳ chọn)

```bash
# Cho phép S3 invoke Lambda
aws lambda add-permission \
  --function-name educonnect-video-transcoder \
  --statement-id s3invoke \
  --action lambda:InvokeFunction \
  --principal s3.amazonaws.com \
  --source-arn arn:aws:s3:::educonnect-bucket

# Cấu hình notification
aws s3api put-bucket-notification-configuration \
  --bucket educonnect-bucket \
  --notification-configuration '{
    "LambdaFunctionConfigurations": [
      {
        "Id": "video-transcode-trigger",
        "LambdaFunctionArn": "arn:aws:lambda:ap-southeast-1:ACCOUNT_ID:function:educonnect-video-transcoder",
        "Events": ["s3:ObjectCreated:*"] ,
        "Filter": {
          "Key": {
            "FilterRules": [
              {"Name": "prefix", "Value": "videos/lessons/"},
              {"Name": "suffix", "Value": "original.mp4"}
            ]
          }
        }
      }
    ]
  }'
```

Lưu ý: Thay `ACCOUNT_ID` và `bucket` cho phù hợp.

---

## 4) CORS cho S3 (nếu upload trực tiếp từ browser)

Nếu client gọi presigned URL PUT để upload, cần CORS cho bucket:

```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["PUT", "GET", "HEAD"],
    "AllowedOrigins": ["*"],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3000
  }
]
```

Sản xuất nên giới hạn `AllowedOrigins` theo domain frontend của bạn.

---

## 5) IAM cho Lambda

Role `educonnect-lambda-execution-role` cần các quyền:

- `AWSLambdaBasicExecutionRole` (CloudWatch Logs)
- `AmazonS3FullAccess` (hoặc policy tinh gọn chỉ cho bucket và prefix cần thiết)

Ví dụ policy tinh gọn (thay bucket cho phù hợp):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:GetObject", "s3:PutObject", "s3:ListBucket"],
      "Resource": [
        "arn:aws:s3:::educonnect-bucket",
        "arn:aws:s3:::educonnect-bucket/videos/lessons/*"
      ]
    }
  ]
}
```

---

## 6) Kiểm thử

1. Gọi `POST /api/video-lessons` → lấy `videoLessonId`
2. Gọi `POST /api/video-lessons/{id}/upload-url` → nhận presigned URL
3. PUT file MP4 lên presigned URL
4. Theo dõi CloudWatch Logs của Lambda (`/aws/lambda/educonnect-video-transcoder`)
5. Poll `GET /api/video-lessons/{id}/status` đến khi `READY`
6. Gọi `GET /api/video-lessons/{id}/stream` → nhận `manifestUrl`
7. Mở `manifestUrl` trong browser/HLS player

---

## 7) Sự cố thường gặp

- 403 từ CloudFront: signed URL hết hạn, hoặc OAC chưa cấp quyền bucket
- 404 manifest: Lambda chưa chạy xong, hoặc S3 event chưa bật đúng prefix/suffix
- Lambda timeout: video quá dài; tăng timeout/memory, hoặc chuyển sang nhiều bitrate/EC2/ECS
- CORS lỗi PUT: thêm CORS rule cho S3 như mục 4

---

## 8) Gợi ý tối ưu

- Dùng nhiều rendition (360p/480p/720p) để adaptive bitrate
- TTL: m3u8 thấp (60s), ts cao (1 ngày)
- Bật compression (Gzip/Brotli) cho m3u8
- S3 Storage class: Intelligent-Tiering cho video cũ
