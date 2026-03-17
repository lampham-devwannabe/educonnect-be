# Hướng dẫn hoàn chỉnh: Upload và Xử lý VOD HLS

Tài liệu này hướng dẫn toàn bộ quy trình từ setup AWS, Backend, Frontend đến test upload và phát VOD HLS streaming.

## 📋 Mục lục

1. [Tổng quan luồng xử lý](#1-tổng-quan-luồng-xử-lý)
2. [Setup AWS Infrastructure](#2-setup-aws-infrastructure)
3. [Setup Backend](#3-setup-backend)
4. [Setup Frontend](#4-setup-frontend)
5. [Quy trình Test](#5-quy-trình-test)
6. [Troubleshooting](#6-troubleshooting)

---

## 1. Tổng quan luồng xử lý

### 1.1 Kiến trúc hệ thống

```
┌─────────────┐
│  Frontend   │
└──────┬──────┘
       │
       ├─► 1. POST /api/video-lessons (tạo metadata)
       │
       ├─► 2. POST /api/video-lessons/{id}/upload-url (lấy presigned URL)
       │
       ├─► 3. PUT file MP4 lên S3 (direct upload)
       │
       ├─► 4. Poll GET /api/video-lessons/{id}/status (chờ READY)
       │
       └─► 5. GET /api/video-lessons/{id}/stream (lấy streaming URL)
            │
            └─► Phát video bằng hls.js

┌─────────────┐
│   S3 Bucket │
└──────┬──────┘
       │
       ├─► videos/lessons/{id}/original.mp4 (file upload)
       │
       └─► videos/lessons/{id}/master.m3u8 + *.ts (HLS output)
            │
            └─► S3 Event Trigger

┌─────────────┐
│   Lambda    │ ◄──── S3 Event (khi upload original.mp4)
└──────┬──────┘
       │
       ├─► Download original.mp4
       ├─► FFmpeg transcode → HLS (720p.m3u8 + *.ts)
       ├─► Upload HLS files lên S3
       └─► Callback API → Update status = READY

┌─────────────┐
│ CloudFront  │ ◄──── Phục vụ HLS files (signed URL)
└─────────────┘
```

### 1.2 Cấu trúc thư mục trong S3

```
educonnect-bucket/
├── videos/lessons/              ← Folder riêng cho VOD
│   ├── 1/                      ← VideoLesson ID = 1
│   │   ├── original.mp4        ← File MP4 gốc upload
│   │   ├── master.m3u8         ← Master playlist HLS
│   │   ├── 720p.m3u8           ← Variant playlist 720p
│   │   ├── 720p_000.ts         ← HLS segment files
│   │   ├── 720p_001.ts
│   │   └── ...
│   ├── 2/
│   │   └── ...
│   └── ...
├── public/                      ← Files public khác
└── [các file khác]
```

### 1.3 Luồng xử lý chi tiết

1. **Tạo VideoLesson**: Backend tạo record với status = `UPLOADING`
2. **Lấy Upload URL**: Backend tạo S3 presigned URL PUT (hết hạn 60 phút)
3. **Upload MP4**: Frontend upload trực tiếp lên S3 qua presigned URL
4. **Lambda Trigger**: S3 Event tự động trigger Lambda khi detect `videos/lessons/*/original.mp4`
5. **Transcode**: Lambda download MP4, dùng FFmpeg transcode sang HLS
6. **Upload HLS**: Lambda upload HLS files (`master.m3u8`, `720p.m3u8`, `*.ts`) lên S3
7. **Callback**: Lambda gọi API update status = `READY`
8. **Streaming**: Backend tạo CloudFront signed URL cho `master.m3u8`
9. **Playback**: Frontend dùng hls.js phát video qua CloudFront URL

---

## 2. Setup AWS Infrastructure

### 2.1 S3 Bucket

#### Bước 1: Kiểm tra bucket hiện có

Bucket `educonnect-bucket` đã có ở region `ap-southeast-1`.

#### Bước 2: Cấu hình CORS

Cho phép upload từ browser. Đây là CORS config tối ưu cho VOD:

```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["GET", "PUT", "HEAD"],
    "AllowedOrigins": [
      "http://localhost:3000",
      "http://localhost:5173",
      "https://your-production-domain.com"
    ],
    "ExposeHeaders": [
      "ETag",
      "x-amz-server-side-encryption",
      "x-amz-request-id",
      "x-amz-id-2"
    ],
    "MaxAgeSeconds": 21600
  }
]
```

**Giải thích:**

- **AllowedMethods**:

  - `PUT`: Bắt buộc cho upload MP4 lên S3 qua presigned URL
  - `GET`: Cần cho việc download/streaming files (nếu có direct access)
  - `HEAD`: Cần cho việc check file existence
  - **Không cần `POST`, `DELETE`** cho VOD flow (chỉ dùng PUT để upload)

- **AllowedOrigins**:

  - Development: `localhost:3000`, `localhost:5173` (React/Vite)
  - Production: Thay `https://your-production-domain.com` bằng domain thực tế
  - **Không dùng `"*"` trong production** vì lý do bảo mật

- **AllowedHeaders**: `["*"]` cho phép tất cả headers (cần cho presigned URL với custom headers)

- **ExposeHeaders**: Các headers cần expose cho browser để xử lý response

- **MaxAgeSeconds**: `21600` (6 giờ) là hợp lý, giảm số lần preflight requests

**Áp dụng qua CLI:**

```bash
aws s3api put-bucket-cors --bucket educonnect-bucket --cors-configuration file://cors-config.json
```

**Hoặc qua Console:**

- Vào S3 → Bucket `educonnect-bucket` → Permissions → CORS
- Paste JSON config trên
- **Lưu ý:** Bỏ dấu `[ ]` nếu paste trực tiếp trong Console editor

**CORS config cho production (khuyến nghị):**

```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["GET", "PUT", "HEAD"],
    "AllowedOrigins": [
      "https://educonnect.com",
      "https://www.educonnect.com",
      "https://app.educonnect.com"
    ],
    "ExposeHeaders": [
      "ETag",
      "x-amz-server-side-encryption",
      "x-amz-request-id",
      "x-amz-id-2"
    ],
    "MaxAgeSeconds": 21600
  }
]
```

**Nếu bạn có IP cố định cho server/backend và cần upload từ server:**

```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["GET", "PUT", "HEAD"],
    "AllowedOrigins": [
      "http://localhost:3000",
      "http://localhost:5173",
      "https://your-production-domain.com"
    ],
    "ExposeHeaders": [
      "ETag",
      "x-amz-server-side-encryption",
      "x-amz-request-id",
      "x-amz-id-2"
    ],
    "MaxAgeSeconds": 21600
  },
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
    "AllowedOrigins": ["http://139.59.97.252", "https://api.your-domain.com"],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3600
  }
]
```

Lưu ý: IP `139.59.97.252` nên thay bằng domain nếu có, hoặc dùng cho internal/development chỉ.

---

### 2.2 Lambda Function

#### Bước 1: Tạo IAM Role

##### Cách 1: Qua AWS Console (Giao diện Web) - Khuyến nghị

1. **Vào IAM Console:**

   - Đăng nhập AWS Console
   - Tìm "IAM" trong search bar hoặc vào Services → IAM
   - Vào **Roles** ở menu bên trái

2. **Create Role:**

   - Click nút **Create role** (màu xanh, góc trên bên phải)

3. **Chọn Trusted entity type:**

   - Chọn **AWS service**
   - Ở phần **Use case**, chọn **Lambda**
   - Click **Next**

4. **Attach permissions policies:**

   - Tìm và chọn:
     - ✅ **AWSLambdaBasicExecutionRole** (cho CloudWatch Logs)
     - ✅ **AmazonS3FullAccess** (hoặc tạo custom policy hẹp hơn)
   - Click **Next**

5. **Name, review, and create:**

   - **Role name**: `educonnect-lambda-execution-role`
   - **Description**: `Role for Lambda function to process video transcoding`
   - Review lại các policies đã chọn
   - Click **Create role**

6. **Kiểm tra Role đã tạo:**
   - Tìm role `educonnect-lambda-execution-role` trong danh sách
   - Click vào role để xem details

**Lưu ý:** Nếu muốn tạo custom policy hẹp hơn (khuyến nghị cho production):

- Trong phần **Permissions** của Role, click **Add permissions** → **Create inline policy**
- Chọn **JSON** tab và paste:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:ap-southeast-1:*:*"
    },
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

- **Policy name**: `educonnect-lambda-s3-policy`
- Click **Create policy**

##### Cách 2: Qua AWS CLI

```bash
# Tạo role
aws iam create-role \
  --role-name educonnect-lambda-execution-role \
  --assume-role-policy-document '{
    "Version":"2012-10-17",
    "Statement":[{
      "Effect":"Allow",
      "Principal":{"Service":"lambda.amazonaws.com"},
      "Action":"sts:AssumeRole"
    }]
  }' || echo "Role already exists"

# Attach CloudWatch Logs policy
aws iam attach-role-policy \
  --role-name educonnect-lambda-execution-role \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

# Attach S3 Full Access (hoặc tạo custom policy hẹp hơn)
aws iam attach-role-policy \
  --role-name educonnect-lambda-execution-role \
  --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess
```

**Lưu Role ARN để dùng khi deploy Lambda:**

- ARN sẽ có dạng: `arn:aws:iam::ACCOUNT_ID:role/educonnect-lambda-execution-role`
- Hoặc copy từ Role details page trong Console

#### Bước 2: Tạo FFmpeg Lambda Layer

Lambda cần FFmpeg để transcode. Có 2 cách:

**Cách 1: Dùng Lambda Layer có sẵn**

Tìm trên GitHub: "ffmpeg-lambda-layer" hoặc dùng:

```bash
# Download và upload layer (tham khảo hướng dẫn trên GitHub)
aws lambda publish-layer-version \
  --layer-name ffmpeg \
  --zip-file fileb://ffmpeg-layer.zip \
  --compatible-runtimes python3.12
```

**Cách 2: Dùng Container Image (khuyến nghị)**

Sửa `lambda/Dockerfile` và deploy bằng container image.

#### Bước 3: Deploy Lambda Function

```bash
cd lambda

# Set environment variables
export S3_BUCKET_NAME=educonnect-bucket
export CALLBACK_API_URL=https://your-api-domain.com  # URL của backend API

# Deploy
./deploy.sh
```

Script `deploy.sh` sẽ:

- Zip code và upload lên S3
- Tạo/update Lambda function
- Set timeout 900s (15 phút), memory 3008 MB

**Kiểm tra deployment:**

```bash
aws lambda get-function --function-name educonnect-video-transcoder --region ap-southeast-1
```

#### Bước 4: Thêm FFmpeg Layer (nếu dùng Layer)

```bash
# Lấy ARN của layer version
LAYER_ARN="arn:aws:lambda:ap-southeast-1:ACCOUNT_ID:layer:ffmpeg:VERSION"

# Update Lambda function với layer
aws lambda update-function-configuration \
  --function-name educonnect-video-transcoder \
  --layers $LAYER_ARN \
  --region ap-southeast-1
```

---

### 2.3 S3 Event Trigger

Cấu hình S3 tự động trigger Lambda khi upload `original.mp4`:

#### Qua Console:

1. Vào S3 → Bucket `educonnect-bucket` → Properties
2. Scroll xuống **Event notifications** → **Create event notification**
3. Cấu hình:
   - **Name**: `video-transcode-trigger`
   - **Prefix**: `videos/lessons/`
   - **Suffix**: `original.mp4`
   - **Event types**: Chọn `All object create events`
   - **Destination**: Chọn Lambda function → `educonnect-video-transcoder`
4. Xác nhận cho phép S3 invoke Lambda

#### Qua CLI:

```bash
# Cho phép S3 invoke Lambda
aws lambda add-permission \
  --function-name educonnect-video-transcoder \
  --statement-id s3invoke \
  --action lambda:InvokeFunction \
  --principal s3.amazonaws.com \
  --source-arn arn:aws:s3:::educonnect-bucket \
  --region ap-southeast-1

# Cấu hình notification
aws s3api put-bucket-notification-configuration \
  --bucket educonnect-bucket \
  --notification-configuration '{
    "LambdaFunctionConfigurations": [{
      "Id": "video-transcode-trigger",
      "LambdaFunctionArn": "arn:aws:lambda:ap-southeast-1:ACCOUNT_ID:function:educonnect-video-transcoder",
      "Events": ["s3:ObjectCreated:*"],
      "Filter": {
        "Key": {
          "FilterRules": [
            {"Name": "prefix", "Value": "videos/lessons/"},
            {"Name": "suffix", "Value": "original.mp4"}
          ]
        }
      }
    }]
  }'
```

**Thay `ACCOUNT_ID` bằng AWS Account ID của bạn.**

---

### 2.4 CloudFront Distribution

#### Bước 1: Tạo Origin Access Control (OAC)

1. Vào CloudFront Console → Security → **Origin access**
2. Click **Create origin access control**
3. Cấu hình:
   - **Name**: `educonnect-s3-oac`
   - **Signing behavior**: Sign requests (recommended)
   - **Origin type**: S3 origin
4. Click **Create**

#### Bước 2: Tạo CloudFront Distribution

1. Vào CloudFront → **Create distribution**
2. **Origin settings:**
   - **Origin domain**: Chọn `educonnect-bucket.s3.ap-southeast-1.amazonaws.com`
   - **Origin access**: Chọn OAC vừa tạo (`educonnect-s3-oac`)
   - **Name**: `educonnect-bucket`
3. **Default cache behavior:**
   - **Path pattern**: `videos/lessons/*`
   - **Allowed HTTP methods**: `GET, HEAD`
   - **Cache policy**: CachingOptimized (hoặc custom)
     - TTL: `m3u8` (MinTTL: 0, DefaultTTL: 60s)
     - TTL: `ts` files (DefaultTTL: 86400s = 1 ngày)
   - **Viewer protocol**: Redirect HTTP to HTTPS
4. **Distribution settings:**
   - **Price class**: Chọn theo ngân sách
5. Click **Create distribution**

**Lưu lại Domain name:** `https://dxxxxx.cloudfront.net`

#### Bước 3: Update S3 Bucket Policy

Khi attach OAC, CloudFront sẽ đề xuất bucket policy. Copy và áp dụng:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowCloudFrontServicePrincipal",
      "Effect": "Allow",
      "Principal": {
        "Service": "cloudfront.amazonaws.com"
      },
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::educonnect-bucket/videos/lessons/*",
      "Condition": {
        "StringEquals": {
          "AWS:SourceArn": "arn:aws:cloudfront::ACCOUNT_ID:distribution/DISTRIBUTION_ID"
        }
      }
    }
  ]
}
```

**Hoặc qua Console:**

1. Vào S3 → Bucket → Permissions
2. CloudFront sẽ tự động suggest policy khi attach OAC
3. Click **Copy policy** và **Save**

---

### 2.5 CloudFront Key Pair (Signed URL)

#### Bước 1: Tạo Key Pair

1. Vào CloudFront → **Public keys** → **Create public key**
2. Upload public key (hoặc tạo mới)
3. Vào **Key groups** → **Create key group**
4. Chọn public key vừa tạo
5. Lưu **Key group ID** (ví dụ: `K2JCJMDEHXQW5F`)

#### Bước 2: Lưu Private Key

- Download private key (file `.pem`)
- Lưu vào backend: `src/main/resources/cloudfront-private-key.pem`
- **Hoặc** encode base64 và lưu vào environment variable

#### Bước 3: Update CloudFront Distribution

1. Vào Distribution → **Behaviors** → Edit
2. **Restrict viewer access**: Chọn **Yes**
3. **Viewer protocol policy**: **Redirect HTTP to HTTPS**
4. **Restrictions**: Chọn Key group vừa tạo
5. Save changes

---

## 3. Setup Backend

### 3.1 Environment Variables

Thêm vào `.env` hoặc environment configuration:

```bash
# S3 Configuration
S3_ACCESS_KEY=your-access-key
S3_SECRET_KEY=your-secret-key
S3_BUCKET_NAME=educonnect-bucket
S3_REGION=ap-southeast-1

# CloudFront Configuration
CDN_DOMAIN=https://dxxxxx.cloudfront.net  # Domain từ CloudFront distribution
KEY_PAIR_ID=K2JCJMDEHXQW5F  # Key group ID từ CloudFront

# Lambda (optional - cho monitoring)
VIDEO_LAMBDA_FUNCTION_NAME=educonnect-video-transcoder
VIDEO_LAMBDA_REGION=ap-southeast-1
CALLBACK_API_URL=https://your-api-domain.com  # URL để Lambda callback
```

### 3.2 CloudFront Private Key

**Cách 1: File trong resources (khuyến nghị)**

1. Lưu file private key: `src/main/resources/cloudfront-private-key.pem`
2. Code sẽ tự động load từ classpath

**Cách 2: Environment Variable (cho production)**

1. Encode private key base64:

```bash
cat cloudfront-private-key.pem | base64 -w 0
```

2. Set environment variable:

```bash
CLOUDFRONT_PRIVATE_KEY_BASE64="LS0tLS1CRUdJTi..."
```

3. Update `CloudfrontKeyLoader` để decode từ env var.

### 3.3 Database Migration

Đảm bảo các bảng đã được tạo:

```sql
-- Bảng video_lesson
CREATE TABLE IF NOT EXISTS video_lesson (
  video_lesson_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  lesson_id BIGINT,
  session_id BIGINT,
  title VARCHAR(255),
  description TEXT,
  video_type VARCHAR(50),
  duration_seconds INT,
  thumbnail_s3_key VARCHAR(255),
  original_video_s3_key VARCHAR(255),
  hls_master_playlist_s3_key VARCHAR(255),
  status VARCHAR(50) DEFAULT 'UPLOADING',
  processing_progress INT DEFAULT 0,
  processing_error_message TEXT,
  processed_at DATETIME,
  access_type VARCHAR(50) DEFAULT 'ENROLLED_ONLY',
  is_preview BOOLEAN DEFAULT false,
  view_count BIGINT DEFAULT 0,
  like_count BIGINT DEFAULT 0,
  uploaded_at DATETIME,
  published_at DATETIME,
  created_by BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  modified_by BIGINT,
  modified_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted BOOLEAN DEFAULT false
);

-- Bảng video_processing_job (optional - cho tracking)
CREATE TABLE IF NOT EXISTS video_processing_job (
  job_id VARCHAR(255) PRIMARY KEY,
  video_lesson_id BIGINT,
  status VARCHAR(50),
  progress_percentage INT,
  started_at DATETIME,
  completed_at DATETIME,
  input_s3_key VARCHAR(255),
  output_s3_prefix VARCHAR(255)
);
```

**Lưu ý:** Nếu dùng JPA với `ddl-auto: update`, các bảng sẽ tự động tạo.

### 3.4 Verify Configuration

Kiểm tra `application.yaml`:

```yaml
s3:
  bucket-name: educonnect-bucket
  region: ap-southeast-1

cdn:
  domain: https://dxxxxx.cloudfront.net

video:
  s3:
    base-prefix: videos/lessons
  cloudfront:
    manifest-expiry-seconds: 600
```

### 3.5 API Endpoints

Backend đã có sẵn các endpoints:

| Method | Endpoint                                | Mô tả                             |
| ------ | --------------------------------------- | --------------------------------- |
| POST   | `/api/video-lessons`                    | Tạo VideoLesson (metadata)        |
| POST   | `/api/video-lessons/{id}/upload-url`    | Lấy presigned upload URL          |
| GET    | `/api/video-lessons/{id}/status`        | Kiểm tra trạng thái xử lý         |
| GET    | `/api/video-lessons/{id}/stream`        | Lấy CloudFront signed URL để phát |
| GET    | `/api/video-lessons/lessons/{lessonId}` | List videos theo lesson           |
| POST   | `/api/video-lessons/update-status`      | Callback từ Lambda (internal)     |

---

## 4. Setup Frontend

### 4.1 Install Dependencies

```bash
npm install hls.js
# hoặc
yarn add hls.js
```

### 4.2 API Client Functions

Tạo file `services/videoService.ts`:

```typescript
import Hls from "hls.js";

const API_BASE = process.env.REACT_APP_API_URL || "http://localhost:8080";

interface VideoLessonRequest {
  lessonId?: number;
  sessionId?: number;
  title: string;
  description?: string;
  videoType?: "THEORY" | "PRACTICE" | "DEMO" | "REVIEW" | "RECORDING";
  accessType?: "PUBLIC" | "ENROLLED_ONLY" | "PAID_ONLY";
  isPreview?: boolean;
}

interface VideoLessonResponse {
  videoLessonId: number;
  status: "UPLOADING" | "PROCESSING" | "READY" | "FAILED";
  processingProgress?: number;
  processingErrorMessage?: string;
  // ... other fields
}

interface UploadUrlResponse {
  uploadUrl: string;
  expiresInSeconds: number;
}

interface StreamingUrlResponse {
  manifestUrl: string;
  expiresInSeconds: number;
}

interface VideoStatusResponse {
  videoLessonId: number;
  status: string;
  processingProgress?: number;
  errorMessage?: string;
  completedAt?: string;
}

// Tạo VideoLesson
export async function createVideoLesson(
  token: string,
  payload: VideoLessonRequest
): Promise<VideoLessonResponse> {
  const res = await fetch(`${API_BASE}/api/video-lessons`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(payload),
  });
  if (!res.ok) throw new Error("Cannot create video lesson");
  const data = await res.json();
  return data.result;
}

// Lấy Upload URL
export async function getUploadUrl(
  videoLessonId: number,
  token: string
): Promise<UploadUrlResponse> {
  const res = await fetch(
    `${API_BASE}/api/video-lessons/${videoLessonId}/upload-url`,
    {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
    }
  );
  if (!res.ok) throw new Error("Cannot get upload url");
  const data = await res.json();
  return data.result;
}

// Upload file lên S3
export async function uploadToS3(
  uploadUrl: string,
  file: File,
  onProgress?: (percent: number) => void
): Promise<void> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();

    xhr.upload.addEventListener("progress", (e) => {
      if (e.lengthComputable && onProgress) {
        const percent = (e.loaded / e.total) * 100;
        onProgress(percent);
      }
    });

    xhr.addEventListener("load", () => {
      if (xhr.status === 200) {
        resolve();
      } else {
        reject(new Error("Upload failed"));
      }
    });

    xhr.addEventListener("error", () => {
      reject(new Error("Upload error"));
    });

    xhr.open("PUT", uploadUrl);
    xhr.setRequestHeader("Content-Type", file.type || "video/mp4");
    xhr.send(file);
  });
}

// Poll status cho đến khi READY
export async function waitUntilReady(
  videoLessonId: number,
  token: string,
  maxAttempts = 60,
  intervalMs = 10000
): Promise<VideoStatusResponse> {
  for (let i = 0; i < maxAttempts; i++) {
    const res = await fetch(
      `${API_BASE}/api/video-lessons/${videoLessonId}/status`,
      {
        headers: { Authorization: `Bearer ${token}` },
      }
    );
    if (!res.ok) throw new Error("Cannot get status");
    const data = await res.json();
    const status = data.result;

    if (status.status === "READY") {
      return status;
    }
    if (status.status === "FAILED") {
      throw new Error(
        `Processing failed: ${status.errorMessage || "Unknown error"}`
      );
    }

    // Wait before next poll
    await new Promise((r) => setTimeout(r, intervalMs));
  }
  throw new Error("Processing timeout");
}

// Lấy Streaming URL
export async function getStreamingUrl(
  videoLessonId: number,
  token: string
): Promise<StreamingUrlResponse> {
  const res = await fetch(
    `${API_BASE}/api/video-lessons/${videoLessonId}/stream`,
    {
      headers: { Authorization: `Bearer ${token}` },
    }
  );
  if (!res.ok) throw new Error("Cannot get streaming url");
  const data = await res.json();
  return data.result;
}

// Phát video bằng hls.js
export function playVideo(
  videoElement: HTMLVideoElement,
  manifestUrl: string
): void {
  if (Hls.isSupported()) {
    const hls = new Hls({
      enableWorker: true,
      lowLatencyMode: false, // Set true nếu cần low latency
    });

    hls.loadSource(manifestUrl);
    hls.attachMedia(videoElement);

    hls.on(Hls.Events.ERROR, (event, data) => {
      if (data.fatal) {
        switch (data.type) {
          case Hls.ErrorTypes.NETWORK_ERROR:
            console.error("Network error, trying to recover...");
            hls.startLoad();
            break;
          case Hls.ErrorTypes.MEDIA_ERROR:
            console.error("Media error, trying to recover...");
            hls.recoverMediaError();
            break;
          default:
            console.error("Fatal error, cannot recover");
            hls.destroy();
            break;
        }
      }
    });
  } else if (videoElement.canPlayType("application/vnd.apple.mpegurl")) {
    // Safari native HLS support
    videoElement.src = manifestUrl;
  } else {
    throw new Error("HLS not supported in this browser");
  }
}
```

### 4.3 Upload Component Example

```typescript
import React, { useState } from "react";
import {
  createVideoLesson,
  getUploadUrl,
  uploadToS3,
  waitUntilReady,
  getStreamingUrl,
  playVideo,
} from "../services/videoService";

export function VideoUploadForm({
  token,
  lessonId,
}: {
  token: string;
  lessonId?: number;
}) {
  const [file, setFile] = useState<File | null>(null);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [processingProgress, setProcessingProgress] = useState(0);
  const [videoLessonId, setVideoLessonId] = useState<number | null>(null);
  const [status, setStatus] = useState<string>("");
  const [streamingUrl, setStreamingUrl] = useState<string | null>(null);

  const handleUpload = async () => {
    if (!file) return;

    try {
      // Step 1: Tạo VideoLesson
      setStatus("Creating video lesson...");
      const videoLesson = await createVideoLesson(token, {
        lessonId,
        title: file.name,
        videoType: "THEORY",
        accessType: "ENROLLED_ONLY",
      });
      setVideoLessonId(videoLesson.videoLessonId);

      // Step 2: Lấy Upload URL
      setStatus("Getting upload URL...");
      const { uploadUrl } = await getUploadUrl(
        videoLesson.videoLessonId,
        token
      );

      // Step 3: Upload file
      setStatus("Uploading video...");
      await uploadToS3(uploadUrl, file, (percent) => {
        setUploadProgress(percent);
      });
      setStatus("Upload complete. Processing...");

      // Step 4: Poll status
      const statusResponse = await waitUntilReady(
        videoLesson.videoLessonId,
        token,
        60, // max 10 minutes
        10000 // poll every 10 seconds
      );
      setProcessingProgress(100);
      setStatus("Video ready!");

      // Step 5: Lấy streaming URL
      const { manifestUrl } = await getStreamingUrl(
        videoLesson.videoLessonId,
        token
      );
      setStreamingUrl(manifestUrl);
    } catch (error) {
      console.error("Upload error:", error);
      setStatus(`Error: ${error.message}`);
    }
  };

  return (
    <div>
      <input
        type="file"
        accept="video/mp4"
        onChange={(e) => setFile(e.target.files?.[0] || null)}
      />
      <button onClick={handleUpload} disabled={!file || status.includes("...")}>
        Upload
      </button>

      {uploadProgress > 0 && (
        <div>
          <p>Upload: {uploadProgress.toFixed(1)}%</p>
          <progress value={uploadProgress} max={100} />
        </div>
      )}

      {processingProgress > 0 && (
        <div>
          <p>Processing: {processingProgress}%</p>
          <progress value={processingProgress} max={100} />
        </div>
      )}

      {status && <p>{status}</p>}

      {streamingUrl && (
        <div>
          <video
            ref={(video) => {
              if (video) playVideo(video, streamingUrl);
            }}
            controls
            style={{ width: "100%", maxWidth: "800px" }}
          />
        </div>
      )}
    </div>
  );
}
```

---

## 5. Quy trình Test

### 5.1 Test Backend API

#### Step 1: Tạo VideoLesson

```bash
curl -X POST http://localhost:8080/api/video-lessons \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "title": "Test Video Upload",
    "description": "Testing VOD upload flow",
    "videoType": "THEORY",
    "accessType": "ENROLLED_ONLY"
  }'
```

Response:

```json
{
  "result": {
    "videoLessonId": 1,
    "status": "UPLOADING",
    "title": "Test Video Upload",
    ...
  }
}
```

#### Step 2: Lấy Upload URL

```bash
curl -X POST http://localhost:8080/api/video-lessons/1/upload-url \
  -H "Authorization: Bearer YOUR_TOKEN"
```

Response:

```json
{
  "result": {
    "uploadUrl": "https://educonnect-bucket.s3.ap-southeast-1.amazonaws.com/videos/lessons/1/original.mp4?X-Amz-Algorithm=...",
    "expiresInSeconds": 3600
  }
}
```

#### Step 3: Upload MP4 file

```bash
curl -X PUT "{uploadUrl}" \
  -H "Content-Type: video/mp4" \
  --upload-file test-video.mp4 \
  -v
```

**Kiểm tra trong S3 Console:**

- Vào bucket → prefix `videos/lessons/1/`
- Sẽ thấy file `original.mp4`

#### Step 4: Kiểm tra Lambda logs

```bash
aws logs tail /aws/lambda/educonnect-video-transcoder --follow --region ap-southeast-1
```

Hoặc qua CloudWatch Console:

- CloudWatch → Log groups → `/aws/lambda/educonnect-video-transcoder`

#### Step 5: Poll status

```bash
# Poll cho đến khi status = READY
curl http://localhost:8080/api/video-lessons/1/status \
  -H "Authorization: Bearer YOUR_TOKEN"
```

Response khi READY:

```json
{
  "result": {
    "videoLessonId": 1,
    "status": "READY",
    "processingProgress": 100,
    "completedAt": "2025-01-22T10:30:00"
  }
}
```

**Kiểm tra trong S3 Console:**

- Vào `videos/lessons/1/`
- Sẽ thấy: `master.m3u8`, `720p.m3u8`, `720p_000.ts`, `720p_001.ts`, ...

#### Step 6: Lấy Streaming URL

```bash
curl http://localhost:8080/api/video-lessons/1/stream \
  -H "Authorization: Bearer YOUR_TOKEN"
```

Response:

```json
{
  "result": {
    "manifestUrl": "https://dxxxxx.cloudfront.net/videos/lessons/1/master.m3u8?Expires=1737540000&Signature=...",
    "expiresInSeconds": 600
  }
}
```

#### Step 7: Test playback

- Copy `manifestUrl` và paste vào browser
- Hoặc dùng VLC: Media → Open Network Stream → Paste URL
- Hoặc dùng hls.js trong frontend (xem phần 4.3)

### 5.2 Test Frontend Flow

1. Mở frontend application
2. Chọn file video MP4
3. Click "Upload"
4. Theo dõi progress bar:
   - Upload progress (0-100%)
   - Processing progress (sau khi upload xong)
5. Khi status = "READY", video sẽ tự động phát

### 5.3 Verify trong S3 Console

1. Mở S3 Console → `educonnect-bucket`
2. Tìm prefix: `videos/lessons/`
3. Click vào folder `1/` (hoặc videoLessonId tương ứng)
4. Kiểm tra files:
   - ✅ `original.mp4` (file upload)
   - ✅ `master.m3u8` (HLS master playlist)
   - ✅ `720p.m3u8` (HLS variant playlist)
   - ✅ `720p_000.ts`, `720p_001.ts`, ... (HLS segments)

---

## 6. Troubleshooting

### 6.1 Lambda không trigger

**Triệu chứng:** Upload MP4 xong nhưng Lambda không chạy.

**Kiểm tra:**

1. S3 Event notification đã cấu hình đúng prefix/suffix?

   - Prefix: `videos/lessons/`
   - Suffix: `original.mp4`

2. Lambda permissions:

```bash
aws lambda get-policy --function-name educonnect-video-transcoder
```

3. CloudWatch Logs:

```bash
aws logs tail /aws/lambda/educonnect-video-transcoder --follow
```

**Fix:**

- Verify S3 event notification config
- Re-add Lambda permission nếu cần

### 6.2 Lambda timeout

**Triệu chứng:** Lambda chạy > 15 phút và timeout.

**Fix:**

- Tăng Lambda timeout (max 15 phút)
- Tăng memory (3008 MB)
- Hoặc chuyển sang AWS MediaConvert cho video lớn

### 6.3 FFmpeg không tìm thấy

**Triệu chứng:** Lambda logs: `ffmpeg: command not found`

**Fix:**

- Thêm FFmpeg Lambda Layer
- Hoặc chuyển sang container image có FFmpeg

### 6.4 403 từ CloudFront

**Triệu chứng:** Khi mở manifest URL bị 403 Forbidden.

**Kiểm tra:**

1. CloudFront signed URL chưa hết hạn?
2. OAC đã được attach vào Origin?
3. Bucket policy đã cho phép CloudFront?
4. Key pair ID đúng?

**Fix:**

- Verify CloudFront distribution settings
- Check bucket policy
- Regenerate signed URL

### 6.5 Video không phát được

**Triệu chứng:** Có streaming URL nhưng video không phát.

**Kiểm tra:**

1. HLS files đã có trong S3?
2. Manifest URL có thể truy cập được?
3. CORS đã cấu hình đúng?
4. Browser console có lỗi gì?

**Fix:**

- Verify HLS files trong S3
- Test manifest URL trực tiếp trong browser
- Check CORS config
- Xem browser console errors

### 6.6 Callback API không hoạt động

**Triệu chứng:** Lambda xử lý xong nhưng status không update thành READY.

**Kiểm tra:**

1. `CALLBACK_API_URL` trong Lambda env vars đúng?
2. Backend API endpoint `/api/video-lessons/update-status` accessible?
3. Lambda có quyền gọi external API?

**Fix:**

- Verify Lambda environment variables
- Test callback endpoint manually
- Check Lambda VPC/network config nếu API trong private network

---

## 7. Checklist hoàn chỉnh

### Setup AWS

- [ ] S3 bucket `educonnect-bucket` ở Singapore
- [ ] S3 CORS configured
- [ ] Lambda function deployed với FFmpeg
- [ ] S3 Event trigger configured
- [ ] CloudFront distribution created
- [ ] CloudFront OAC created và attached
- [ ] S3 bucket policy updated
- [ ] CloudFront key pair created

### Setup Backend

- [ ] Environment variables configured
- [ ] CloudFront private key lưu trong resources hoặc env
- [ ] Database tables created
- [ ] API endpoints working

### Setup Frontend

- [ ] hls.js installed
- [ ] Video service functions implemented
- [ ] Upload component working

### Test Flow

- [ ] Create VideoLesson → OK
- [ ] Get Upload URL → OK
- [ ] Upload MP4 → OK (verify trong S3)
- [ ] Lambda triggered → OK (check logs)
- [ ] HLS files created → OK (verify trong S3)
- [ ] Status = READY → OK
- [ ] Get Streaming URL → OK
- [ ] Video playback → OK

---

## 8. Tài liệu tham khảo

- [AWS S3 Documentation](https://docs.aws.amazon.com/s3/)
- [AWS Lambda Documentation](https://docs.aws.amazon.com/lambda/)
- [AWS CloudFront Documentation](https://docs.aws.amazon.com/cloudfront/)
- [HLS.js Documentation](https://github.com/video-dev/hls.js/)
- [FFmpeg HLS Guide](https://ffmpeg.org/ffmpeg-formats.html#hls)

---

**Lưu ý:** Tài liệu này được cập nhật dựa trên codebase hiện tại. Nếu có thay đổi trong code, vui lòng cập nhật tài liệu tương ứng.
