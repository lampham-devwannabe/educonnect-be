## Hướng dẫn frontend cho xác thực email

- Sau khi đăng nhập, đọc trường `emailVerified` trong JSON trả về từ `POST /api/auth/token` và claim `isVerified` trong JWT để cập nhật state phía client.
- Nếu chưa xác thực:
  - Hiển thị banner cố định ở dashboard hoặc modal chặn thao tác, nội dung: “Bạn cần xác thực email để dùng đầy đủ tính năng”.
  - Gộp nút `Gửi lại email xác thực` gọi `POST /api/auth/verification/resend` (yêu cầu đính kèm token đăng nhập). Vô hiệu nút trong 5 phút sau khi gọi thành công để khớp với giới hạn backend.
- Khi người dùng bấm liên kết trong email, điều hướng tới trang `/verify-email` trên frontend, trang này cần gọi `POST /api/auth/verification` với body `{ "token": "<token từ query>" }`, hiển thị trạng thái thành công/thất bại theo thông báo trả về.
- Với các trang/tính năng yêu cầu tài khoản đã xác thực, kiểm tra `emailVerified` trước khi render. Nếu false, chuyển hướng về trang nhắc xác thực.
- Bắt lỗi HTTP 403 với mã `error.email.not.verified` và chuyển người dùng tới trang xác thực kèm thông báo.

