# Hướng dẫn sử dụng API Upload File S3

## 1. Mục đích

API này cho phép client upload file trực tiếp lên Amazon S3 thông qua Pre-Signed URL, tránh việc server phải xử lý file lớn.

**Hỗ trợ hai chế độ:**

- **Public:** File có thể truy cập trực tiếp qua URL S3.
- **Private:** File chỉ truy cập được qua Pre-Signed GET URL.

---

## 2. Danh sách API

### 2.1. Generate Pre-Signed Upload URL

**Endpoint:**

```
POST /api/files/upload-url
```

**Params:**

```json
{
  "file-id": "test_name",
  "access-type": "PUBLIC"
}
```

- `file-id`: tên của file lưu trong bucket
- `accessType`: `"PUBLIC"` hoặc `"PRIVATE"`

**Response:**

```json
{
  "code": 1000,
  "result": {
    "fileId": "1759241366297_publicFile",
    "url": "https://educonnect-testing.s3.ap-southeast-1.amazonaws.com/1759241366297_publicFile?"
  }
}
```

**Cách sử dụng:**

- Dùng URL này để **PUT** file trực tiếp lên S3.

**Ví dụ với Postman:**

- **Method:** PUT
- **URL:** (Pre-Signed URL trả về)
- **Body:** binary (chọn file muốn tải lên)
- **Headers:**
  - `X-Amz-Acl`: `public-read` hoặc `private` (tùy theo accessType)
  - `Content-Type`: loại file (ví dụ: `image/png`, `application/pdf`)
  - `Content-Disposition`: `inline` (để gắn được vào thẻ img)

---

### 2.2. Generate Pre-Signed Download URL

**Endpoint:**

```
GET /api/files/get-download-url
```

**Params:**

- `file-id`: tên của file lưu trong bucket
- `accessType`: `"PUBLIC"` hoặc `"PRIVATE"`

**Ví dụ:**

```json
{
  "file-id": "1759241366297_publicFile",
  "access-type": "PUBLIC"
}
```

**Response (PRIVATE file):**

```json
{
  "code": 1000,
  "result": {
    "fileId": "1759239793500_privateFile",
    "url": "https://educonnect-testing.s3.ap-southeast-1.amazonaws.com/1759239793500_privateFile?"
  }
}
```

> ⚠️ URL này chỉ tồn tại trong `preSignPeriodMinutes` (ví dụ 15 phút).

**Response (PUBLIC file):**

```json
{
  "fileId": "uploads/test.png",
  "url": "https://bucket-name.s3.ap-southeast-1.amazonaws.com/uploads/test.png",
  "method": "GET"
}
```

---

## 3. Quy trình sử dụng

### 3.1. Upload file PUBLIC

1. Gọi `POST /api/files/upload-url` với `"accessType": "PUBLIC"`.
2. Backend trả về Pre-Signed PUT URL.
3. Client dùng URL đó để upload file:
   ```bash
   curl --upload-file ./test.png "https://...presigned-url..."
   ```
4. File sẽ truy cập trực tiếp qua:
   ```
   https://bucket-name.s3.amazonaws.com/uploads/test.png
   ```

### 3.2. Upload file PRIVATE

1. Gọi `POST /api/files/upload-url` với `"accessType": "PRIVATE"`.
2. Upload file bằng Pre-Signed URL.
3. Muốn tải file → gọi `GET /api/files/download-url?filePath=...` để lấy Pre-Signed GET URL.
4. Chỉ có hiệu lực trong khoảng thời gian được config (ví dụ: 60 phút).

---

## 4. Test với Postman

### Upload (step 1)

```
POST https://api.educonnect.dev/api/files/upload-url
```

**Body (JSON):**

```json
{
  "filePath": "uploads/avatar.png",
  "accessType": "PRIVATE"
}
```

### Upload (step 2)

- PUT vào URL trả về ở step 1
- Header: `Content-Type: image/png`
- Body: binary file

### Download

```
GET https://api.educonnect.dev/api/files/download-url?filePath=uploads/avatar.png
```

- Copy URL trả về, mở trên browser/Postman.

---

## 5. Lưu ý

- Nếu file là **public** thì khi truy cập qua object URL (`https://bucket.s3.amazonaws.com/file.png`) → sẽ hiển thị trực tiếp (nếu ảnh/PDF).
- Nếu file là **private** → cần generate Pre-Signed URL trước khi tải.
- Header `Content-Type` khi PUT cần khớp với loại file upload.
- Pre-Signed URL có thời gian sống ngắn (theo `preSignPeriodMinutes` config trong backend).
