# Checklist Chuẩn hóa Thuật Ngữ Tutor

Tài liệu này tóm tắt các điểm cần rà soát khi đảm bảo toàn bộ backend dùng thống nhất thuật ngữ **tutor** cho role giảng dạy.

## 1. Roles & Constant
- `PredefinedRole.TUTOR_ROLE` tồn tại và được sử dụng thay cho mọi giá trị khác.
- Các seed data trong `ApplicationInitConfig` tạo user mặc định với username/password `tutor`.
- Bảng `role` trong DB chỉ chứa giá trị `TUTOR` cho role giảng dạy.

## 2. Security & Authorization
- Tất cả `@PreAuthorize` dùng `hasRole('TUTOR')` hoặc `hasAnyRole(..., 'TUTOR')` nơi phù hợp.
- `SecurityConfig` chỉ expose các endpoint công khai có chứa từ khóa `tutor` (`/api/course/by-tutor`, `/api/students/tutor-profile`, ...).

## 3. Domain Model & Repository
- `Course` dùng field `tutor` với `@JoinColumn(name = "tutor_id")`.
- Bất kỳ quan hệ nào trước đây mô tả giáo viên đều chuyển sang `tutor`.
- `CourseRepository` và các repository khác dùng method như `findByTutor_UserIdAndIsDeletedFalse` hoặc `existsByTutorIdAndLessonId`.

## 4. DTOs, Mapper, Service
- Request/response DTO dùng `tutorId`, `TutorInfo`, `TutorGeneralResponse`, ...
- `CourseMapper` và các mapper khác chỉ có method `toTutorInfo`.
- Service layer chỉ cung cấp API như `getCoursesByTutor`, `getTutorsBySubject`, `TutorExamService.getExamResults(...)`.

## 5. Controller & Endpoint Naming
- API path public/private đều sử dụng `/tutor` (ví dụ: `/api/course/by-tutor`, `/api/students/tutor-profile`, `/api/tutor/exams/...`).
- Các biến request (`@RequestParam`, `@PathVariable`) được đặt tên `tutorId`, `tutorProfileId`, ...

## 6. Seed Data, Env Var & Tooling
- Biến môi trường, sample command và script dùng tên `TUTOR_TOKEN`, `TUTOR_USER_ID`, `TUTOR_ID`.
- Không còn biến môi trường nào mang prefix cũ liên quan tới role giảng dạy trước đây.

## 7. Tests & Fixtures
- Toàn bộ unit test, mock data, builder default user đều chuyển sang `tutor` (`tutor1`, `tutor-1`, ...).
- Tên file test và class phản ánh `Tutor...` (ví dụ `TutorExamServiceTest`).

## 8. Docs & Comment
- `docs/exam-testing-guide.md`, `docs/progress-testing-guide.md`, `docs/course-structure-relationships.md` cùng mọi comment/code snippet sử dụng `tutor`.
- Hướng dẫn tích hợp API cập nhật path/param mới.

## 9. Checklist sau cùng
- [x] Code main không còn string cũ mô tả role giảng dạy trước đây.
- [x] Test suite chỉ đề cập tới `tutor`.
- [x] Tài liệu và ví dụ CLI đã đổi sang biến `TUTOR_*`.
- [x] Seed data/tài khoản mặc định dùng role `TUTOR`.
