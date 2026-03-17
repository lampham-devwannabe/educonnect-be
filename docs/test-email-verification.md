# Hướng dẫn test xác thực email với cURL

## Các bước test

### 1. Đăng ký tài khoản mới

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "testuser@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User",
    "dob": "2000-01-01",
    "roleName": "STUDENT"
  }'
```

**Kết quả mong đợi:** Tài khoản được tạo, email xác thực được gửi tự động.

---

### 2. Đăng nhập để lấy JWT token

```bash
curl -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

**Kết quả mong đợi:** 
```json
{
  "code": 1000,
  "result": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "authenticated": true,
    "emailVerified": false
  }
}
```

**Lưu ý:** Lưu lại giá trị `token` để dùng cho các request tiếp theo.

---

### 3. Kiểm tra thông tin user (chưa xác thực)

```bash
curl -X GET http://localhost:8080/api/users/my-info \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Kết quả mong đợi:**
```json
{
  "code": 1000,
  "result": {
    "userId": "...",
    "username": "testuser",
    "email": "testuser@example.com",
    "emailVerified": false,
    "emailVerifiedAt": null,
    ...
  }
}
```

---

### 4. Thử truy cập tính năng cần xác thực (sẽ bị chặn)

```bash
curl -X GET http://localhost:8080/api/courses \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Kết quả mong đợi:**
```json
{
  "code": 1013,
  "message": "Email chưa được xác thực"
}
```

---

### 5. Gửi lại email xác thực

```bash
curl -X POST http://localhost:8080/api/auth/verification/resend \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

**Kết quả mong đợi:**
```json
{
  "code": 1000,
  "message": "Email xác thực đã được gửi",
  "result": "Verification email sent"
}
```

**Lưu ý:** 
- Chỉ có thể gửi lại sau 5 phút (cooldown)
- Nếu gửi quá sớm sẽ nhận lỗi: `VERIFICATION_TOO_SOON`

---

### 6. Xác thực email (sử dụng token từ email)

**Bước 6.1:** Kiểm tra email đã nhận, copy token từ link xác thực.

**Bước 6.2:** Gọi API xác thực:

```bash
curl -X POST http://localhost:8080/api/auth/verification \
  -H "Content-Type: application/json" \
  -d '{
    "token": "YOUR_VERIFICATION_TOKEN_FROM_EMAIL"
  }'
```

**Kết quả mong đợi:**
```json
{
  "code": 1000,
  "message": "Email đã được xác thực thành công",
  "result": "Email verified"
}
```

**Lưu ý:** 
- Token có hiệu lực trong 2 giờ
- Sau khi xác thực, email chào mừng sẽ được gửi tự động

---

### 7. Đăng nhập lại để lấy token mới (có emailVerified = true)

```bash
curl -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

**Kết quả mong đợi:**
```json
{
  "code": 1000,
  "result": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "authenticated": true,
    "emailVerified": true
  }
}
```

---

### 8. Kiểm tra lại thông tin user (đã xác thực)

```bash
curl -X GET http://localhost:8080/api/users/my-info \
  -H "Authorization: Bearer YOUR_NEW_JWT_TOKEN"
```

**Kết quả mong đợi:**
```json
{
  "code": 1000,
  "result": {
    "userId": "...",
    "username": "testuser",
    "email": "testuser@example.com",
    "emailVerified": true,
    "emailVerifiedAt": "2024-01-15T10:30:00",
    ...
  }
}
```

---

### 9. Thử truy cập tính năng (sẽ thành công)

```bash
curl -X GET http://localhost:8080/api/courses \
  -H "Authorization: Bearer YOUR_NEW_JWT_TOKEN"
```

**Kết quả mong đợi:** Trả về danh sách courses hoặc response bình thường (không bị chặn).

---

## Test các trường hợp lỗi

### Test token hết hạn

```bash
curl -X POST http://localhost:8080/api/auth/verification \
  -H "Content-Type: application/json" \
  -d '{
    "token": "EXPIRED_TOKEN"
  }'
```

**Kết quả mong đợi:**
```json
{
  "code": 1015,
  "message": "Token xác thực đã hết hạn"
}
```

### Test token không hợp lệ

```bash
curl -X POST http://localhost:8080/api/auth/verification \
  -H "Content-Type: application/json" \
  -d '{
    "token": "INVALID_TOKEN"
  }'
```

**Kết quả mong đợi:**
```json
{
  "code": 1014,
  "message": "Token xác thực không hợp lệ"
}
```

### Test gửi lại email quá sớm

```bash
# Gửi lần 1
curl -X POST http://localhost:8080/api/auth/verification/resend \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"

# Gửi lần 2 ngay sau đó (sẽ bị chặn)
curl -X POST http://localhost:8080/api/auth/verification/resend \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

**Kết quả mong đợi (lần 2):**
```json
{
  "code": 1016,
  "message": "Vui lòng đợi 5 phút trước khi gửi lại email xác thực"
}
```

---

## Biến môi trường (tùy chọn)

Để dễ test, có thể export các biến:

```bash
export BASE_URL="http://localhost:8080"
export USERNAME="testuser"
export PASSWORD="password123"
export EMAIL="testuser@example.com"
export JWT_TOKEN="your_jwt_token_here"
export VERIFICATION_TOKEN="your_verification_token_here"
```

Sau đó sử dụng trong curl:

```bash
curl -X GET "$BASE_URL/api/users/my-info" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

