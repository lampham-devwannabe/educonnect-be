# Mối quan hệ giữa các bảng Course Structure

## Tổng quan cấu trúc

Hệ thống quản lý nội dung học tập được tổ chức theo cấu trúc phân cấp như sau:

```
Syllabus → Module → Lesson → { Exam → Quiz → QuizOption }
                              { VideoLesson }
```

## Chi tiết mối quan hệ

### 1. Syllabus (Giáo trình)

**Bảng:** `syllabus`

**Mối quan hệ:**

- **1 Syllabus** → **Nhiều Course** (One-to-Many)

  - Một giáo trình có thể được sử dụng cho nhiều khóa học
  - `Course.syllabus_id` → `Syllabus.syllabus_id`

- **1 Syllabus** → **Nhiều Module** (One-to-Many)
  - Một giáo trình chứa nhiều module
  - `Module.syllabus_id` → `Syllabus.syllabus_id`

**Vai trò:**

- Định nghĩa cấu trúc nội dung học tập
- Chứa thông tin mô tả, mục tiêu, đối tượng học viên
- Có thể được tái sử dụng cho nhiều khóa học khác nhau

---

### 2. Course (Khóa học)

**Bảng:** `course`

**Mối quan hệ:**

- **Nhiều Course** → **1 Syllabus** (Many-to-One)

  - Mỗi khóa học thuộc về một giáo trình
  - `Course.syllabus_id` → `Syllabus.syllabus_id`

- **1 Course** → **Nhiều TutorClass** (One-to-Many)

  - Một khóa học có thể có nhiều lớp học (class)
  - `TutorClass.course_id` → `Course.id`

- **1 Course** → **1 Tutor** (Many-to-One)
  - Mỗi khóa học có một gia sư/tutor phụ trách
  - `Course.tutor_id` → `User.user_id`

**Vai trò:**

- Đại diện cho một khóa học cụ thể
- Chứa thông tin giá, loại khóa học (SELF_PACED, REGULAR)
- Liên kết với giáo trình để lấy nội dung học tập

**Lưu ý:**

- Course không trực tiếp chứa Module/Lesson
- Course lấy nội dung thông qua Syllabus → Module → Lesson

---

### 3. Module (Mô-đun)

**Bảng:** `module`

**Mối quan hệ:**

- **Nhiều Module** → **1 Syllabus** (Many-to-One)

  - Mỗi module thuộc về một giáo trình
  - `Module.syllabus_id` → `Syllabus.syllabus_id`

- **1 Module** → **Nhiều Lesson** (One-to-Many)
  - Một module chứa nhiều bài học
  - `Lesson.module_id` → `Module.module_id`

**Vai trò:**

- Nhóm các bài học có liên quan
- Có thứ tự (`order_number`) để sắp xếp
- Có trạng thái: DRAFT, PUBLISHED, ARCHIVED

**Ví dụ:**

- Module 1: "Giới thiệu về Java"
- Module 2: "Cú pháp cơ bản"
- Module 3: "Hướng đối tượng"

---

### 4. Lesson (Bài học)

**Bảng:** `lesson`

**Mối quan hệ:**

- **Nhiều Lesson** → **1 Module** (Many-to-One)

  - Mỗi bài học thuộc về một module
  - `Lesson.module_id` → `Module.module_id`

- **1 Lesson** → **Nhiều Exam** (One-to-Many)

  - Một bài học có thể có nhiều bài kiểm tra
  - `Exam.lesson_id` → `Lesson.lesson_id`

- **1 Lesson** → **Nhiều VideoLesson** (One-to-Many, optional)
  - Một bài học có thể có nhiều video
  - `VideoLesson.lesson_id` → `Lesson.lesson_id` (nullable)

**Vai trò:**

- Đơn vị học tập nhỏ nhất
- Chứa nội dung bài học, mục tiêu học tập
- Có thứ tự trong module (`order_number`)
- Có trạng thái: DRAFT, PUBLISHED, ARCHIVED

**Lưu ý:**

- Một lesson có thể có cả Exam và VideoLesson
- VideoLesson có thể không gắn với lesson (standalone video)

---

### 5. Exam (Bài kiểm tra)

**Bảng:** `exam`

**Mối quan hệ:**

- **Nhiều Exam** → **1 Lesson** (Many-to-One)

  - Mỗi bài kiểm tra thuộc về một bài học
  - `Exam.lesson_id` → `Lesson.lesson_id`

- **1 Exam** → **Nhiều Quiz** (One-to-Many)
  - Một bài kiểm tra chứa nhiều câu hỏi
  - `Quiz.exam_id` → `Exam.exam_id`

**Vai trò:**

- Tập hợp các câu hỏi kiểm tra kiến thức
- Có trạng thái: DRAFT, PUBLISHED, ARCHIVED
- Có field để phân loại (ví dụ: "Lý thuyết", "Thực hành")

**Lưu ý:**

- Một lesson có thể có nhiều exam (ví dụ: Exam lý thuyết, Exam thực hành)

---

### 6. Quiz (Câu hỏi)

**Bảng:** `quiz`

**Mối quan hệ:**

- **Nhiều Quiz** → **1 Exam** (Many-to-One)

  - Mỗi câu hỏi thuộc về một bài kiểm tra
  - `Quiz.exam_id` → `Exam.exam_id`

- **1 Quiz** → **Nhiều QuizOption** (One-to-Many)
  - Một câu hỏi có nhiều lựa chọn
  - `QuizOption.quiz_id` → `Quiz.quiz_id`

**Vai trò:**

- Câu hỏi trong bài kiểm tra
- Có loại: SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE
- Có đáp án đúng (`valid_answer`)
- Có giải thích (`explanation`)
- Có thứ tự (`order_no`)

---

### 7. QuizOption (Lựa chọn đáp án)

**Bảng:** `quiz_option`

**Mối quan hệ:**

- **Nhiều QuizOption** → **1 Quiz** (Many-to-One)
  - Mỗi lựa chọn thuộc về một câu hỏi
  - `QuizOption.quiz_id` → `Quiz.quiz_id`

**Vai trò:**

- Các phương án trả lời cho câu hỏi
- Có flag `isCorrect` để đánh dấu đáp án đúng

---

### 8. VideoLesson (Video bài học)

**Bảng:** `video_lesson`

**Mối quan hệ:**

- **Nhiều VideoLesson** → **1 Lesson** (Many-to-One, optional)

  - Mỗi video có thể gắn với một bài học
  - `VideoLesson.lesson_id` → `Lesson.lesson_id` (nullable)

- **VideoLesson** → **ClassSession** (Many-to-One, optional)
  - Video có thể là recording của một buổi học
  - `VideoLesson.session_id` → `ClassSession.id` (nullable)

**Vai trò:**

- Lưu trữ video bài học
- Có thể là video bài giảng hoặc recording buổi học
- Quản lý file S3, HLS streaming
- Có trạng thái xử lý: UPLOADING, PROCESSING, READY, FAILED
- Có kiểm soát truy cập: ENROLLED_ONLY, PUBLIC, PREVIEW

**Lưu ý:**

- `lesson_id` có thể null nếu video standalone
- `session_id` có thể null nếu không phải recording
- Một lesson có thể có nhiều video (ví dụ: video lý thuyết, video thực hành)

---

## Sơ đồ mối quan hệ tổng thể

```
Syllabus (1)
  ├── Course (N) ──→ Teacher (User)
  │     └── TutorClass (N) ──→ ClassEnrollment (N) ──→ CourseProgress (1)
  │
  └── Module (N)
        └── Lesson (N)
              ├── Exam (N)
              │     └── Quiz (N)
              │           └── QuizOption (N)
              │
              └── VideoLesson (N, optional)
                    └── ClassSession (1, optional - nếu là recording)
```

## Luồng truy cập nội dung

### Khi học viên học một khóa học:

1. **Lấy Course** → Tìm `TutorClass` của học viên
2. **Lấy Syllabus** → Từ `Course.syllabus_id`
3. **Lấy Modules** → Từ `Syllabus` (sắp xếp theo `order_number`)
4. **Lấy Lessons** → Từ mỗi `Module` (sắp xếp theo `order_number`)
5. **Lấy nội dung bài học:**
   - **VideoLesson**: Từ `Lesson.lesson_id` (nếu có)
   - **Exam**: Từ `Lesson.lesson_id` (nếu có)
6. **Lấy Quiz**: Từ `Exam.exam_id`
7. **Lấy QuizOption**: Từ `Quiz.quiz_id`

### Khi tracking progress:

1. **CourseProgress** → Từ `ClassEnrollment` (1:1)
2. **LessonProgress** → Từ `CourseProgress` (1:N)
   - Mỗi `LessonProgress` gắn với một `Lesson.lesson_id`
   - Track trạng thái học tập của từng lesson

## Lưu ý quan trọng

1. **Syllabus là template:**

   - Syllabus định nghĩa cấu trúc nội dung
   - Nhiều Course có thể dùng chung một Syllabus
   - Khi thay đổi Syllabus, tất cả Course dùng Syllabus đó sẽ bị ảnh hưởng

2. **Course không trực tiếp chứa Module/Lesson:**

   - Course chỉ tham chiếu đến Syllabus
   - Module và Lesson thuộc về Syllabus, không thuộc Course
   - Điều này cho phép tái sử dụng Syllabus cho nhiều Course

3. **VideoLesson có thể standalone:**

   - `lesson_id` có thể null
   - Video có thể không gắn với lesson cụ thể
   - Có thể là video preview hoặc video chung

4. **Một Lesson có thể có nhiều Exam:**

   - Ví dụ: Exam lý thuyết, Exam thực hành
   - Mỗi Exam có thể có nhiều Quiz

5. **Progress tracking:**
   - `CourseProgress` track tiến độ của toàn bộ course
   - `LessonProgress` track tiến độ từng lesson
   - Progress được tạo tự động khi học viên enroll vào class

## Ví dụ thực tế

### Khóa học "Java Cơ bản"

```
Syllabus: "Java Programming Fundamentals"
  ├── Course: "Java Cơ bản - Khóa 1" (giá: 500,000 VNĐ)
  │     └── TutorClass: "Lớp Java T2-T4-T6"
  │
  └── Module 1: "Giới thiệu Java"
        ├── Lesson 1: "Cài đặt môi trường"
        │     ├── VideoLesson: "Hướng dẫn cài đặt JDK"
        │     └── Exam: "Kiểm tra cài đặt"
        │           └── Quiz 1: "JDK là gì?"
        │                 ├── QuizOption A: "Java Development Kit" (✓)
        │                 └── QuizOption B: "Java Database Kit"
        │
        └── Lesson 2: "Hello World"
              ├── VideoLesson: "Viết chương trình đầu tiên"
              └── Exam: "Bài tập Hello World"
```

## API liên quan

- `GET /api/courses/{courseId}` - Lấy thông tin course và syllabus
- `GET /api/lessons/{lessonId}` - Lấy thông tin lesson
- `GET /api/video-lessons/{videoLessonId}/stream` - Stream video
- `GET /api/progress/enrollments/{enrollmentId}` - Lấy progress của học viên
- `PUT /api/progress/lessons` - Cập nhật progress khi học lesson
