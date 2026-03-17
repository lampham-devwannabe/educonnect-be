# Progress Tracking API Guide

Tài liệu này mô tả luồng xử lý tiến độ học tập (`CourseProgress`/`LessonProgress`) để frontend và backend phối hợp tích hợp. Nội dung được kiểm chứng thông qua `ProgressControllerTest`, vì vậy các ví dụ dưới đây phản ánh đúng contract hiện tại.

## 1. Kiến trúc tổng quan

- Mỗi bản ghi `ClassEnrollment` sẽ tự động tạo một `CourseProgress` (gọi từ `ProgressService.createCourseProgress` khi tutor mời học viên hoặc học viên đăng ký lớp).
- `CourseProgress` chứa danh sách `LessonProgress` tương ứng với toàn bộ lesson trong course.
- `LessonProgress` lưu trạng thái: `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`, `FAILED`, `SKIPPED` (chỉ khi lesson optional).
- Phần trăm hoàn thành (`progressPercentage`) được tính tự động dựa trên số lesson ở trạng thái `COMPLETED` hoặc `SKIPPED` (optional).

## 2. Các endpoint

### 2.1 GET `/api/progress/enrollments/{enrollmentId}`

- **Mục đích**: Lấy tổng quan tiến độ của một enrollment, bao gồm danh sách lesson.
- **Điều kiện sử dụng**: Enrollment đã tồn tại và đã được tạo progress.
- **Response** (`CourseProgressResponse`):
  - `enrollmentId`, `courseProgressId`
  - `status`: `CourseProgressStatus`
  - `totalLessons`, `completedLessons`, `progressPercentage`
  - `startedAt`, `completedAt`
  - `lessons`: danh sách lesson progress (xem 2.2)
- **Frontend flow**:
  1. Gọi endpoint sau khi người học vào trang lớp/course.
  2. Hiển thị tổng tiến độ và danh sách lesson cùng trạng thái.
- **Backend lưu ý**: Phương thức `ProgressController#getCourseProgress` gọi `ProgressService.getCourseProgress` và map sang DTO bằng `ProgressMapper`. Nếu enrollment chưa có progress sẽ ném `IllegalArgumentException`.

### 2.2 GET `/api/progress/enrollments/{enrollmentId}/lessons`

- **Mục đích**: Lấy danh sách lesson progress thuần (không kèm metadata course).
- **Khi dùng**: Trường hợp frontend chỉ cần refresh danh sách lesson nhanh mà không lấy trạng thái tổng.
- **Response**: Mảng `LessonProgressResponse` gồm `lessonId`, `lessonTitle`, `status`, `optionalLesson`, `result`, `lastAccessedAt`, `completedAt`.
- **Backend**: Gọi `ProgressService.getLessonProgresses`, mapper trả về DTO.

### 2.3 PUT `/api/progress/lessons`

- **Mục đích**: Cập nhật trạng thái của một lesson.
- **Request body** (`UpdateLessonProgressRequest`):
  - `enrollmentId` (bắt buộc)
  - `lessonId` (bắt buộc)
  - `status` (`LessonProgressStatus`, bắt buộc)
  - `result`: chuỗi, dùng cho quiz/exam (có thể null)
  - `skipOptional`: boolean, chỉ định skip lesson nếu lesson optional (optional)
- **Điều kiện sử dụng**:
  - Lesson phải thuộc course của enrollment.
  - Progress đã tồn tại (nếu chưa, backend cần gọi `createCourseProgress` trước).
- **Luồng gợi ý**:
  1. Khi user bắt đầu xem video → gửi `status = IN_PROGRESS`.
  2. Khi hoàn thành video/lesson → gửi `status = COMPLETED`.
  3. Nếu bài quiz thất bại → `status = FAILED`, `result = "score/..."`.
  4. Nếu lesson optional được bỏ qua → `skipOptional = true`.
- **Backend xử lý**: `ProgressService.updateLessonProgress`
  - Cập nhật `LessonProgress`, set timestamps.
  - Gọi `recalculateCourseProgressInternal` để cập nhật `CourseProgress`.

## 3. Tích hợp với các service khác

- **TutorClassService**: Sau khi tạo `ClassEnrollment`, gọi `progressService.createCourseProgress(enrollmentId)` để đảm bảo có dữ liệu ban đầu.
- **VideoLessonController**: Endpoint `/api/video-lessons/{id}/stream` chấp nhận `enrollmentId` tùy chọn. Nếu truyền, backend tự động cập nhật lesson tương ứng sang `IN_PROGRESS`.
- **Quiz/Exam**: Sau khi chấm điểm, service tương ứng cần gọi PUT `/api/progress/lessons` để cập nhật `status` và `result`.

## 4. Kiểm thử

`src/test/java/com/sep/educonnect/controller/ProgressControllerTest.java` mô phỏng hai workflow chính:

1. **should_getCourseProgress**  
   - Mock service trả về data → kiểm tra JSON response chứa `result.enrollmentId` và danh sách lesson.
2. **should_updateLessonProgress**  
   - Gửi request với `status = COMPLETED`, `result = PASSED` → verify response giữ đúng giá trị.

Frontend có thể dựa vào structure response từ test để xây UI, backend dựa vào test để đảm bảo giữ nguyên contract khi refactor.

## 5. Quick reference

| Use-case                              | Endpoint & Method                 | Ghi chú nhanh                                               |
|--------------------------------------|----------------------------------|-------------------------------------------------------------|
| Load dashboard progress              | `GET /api/progress/enrollments/{enrollmentId}` | Trả cả tổng tiến độ và lessons                             |
| Refresh danh sách lesson             | `GET /api/progress/enrollments/{enrollmentId}/lessons` | Chỉ danh sách lesson                                        |
| Cập nhật trạng thái lesson           | `PUT /api/progress/lessons`       | Gửi `status`, kèm `result`/`skipOptional` khi cần           |
| Auto set IN_PROGRESS khi xem video   | `GET /api/video-lessons/{id}/stream?enrollmentId=...` | Backend tự gọi progress service nếu truyền enrollmentId    |
| Khởi tạo progress khi enroll         | Service nội bộ (`ProgressService.createCourseProgress`) | Được gọi trong `TutorClassService`                         |

Giữ tài liệu này đồng bộ khi bổ sung endpoint mới hoặc thay đổi contract. Frontend nên caching nhẹ để giảm số lần gọi tới backend khi lesson list không đổi.

