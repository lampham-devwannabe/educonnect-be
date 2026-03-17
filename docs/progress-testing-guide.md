# Hướng dẫn Test Progress Tracking

Tài liệu này hướng dẫn cách tạo dữ liệu từ đầu để test tính năng Progress Tracking.

## Tổng quan

Để test Progress, bạn cần tạo các bản ghi theo thứ tự phụ thuộc:

```
Subject → Syllabus → Module → Lesson → Course → TutorClass → ClassEnrollment → CourseProgress + LessonProgress (tự động)
```

**Lưu ý:** `CourseProgress` và `LessonProgress` sẽ được tạo **tự động** khi học viên được mời vào lớp (ClassEnrollment được tạo).

---

## Chuẩn bị

### 1. Authentication

Đăng nhập để lấy JWT token:

```bash
curl -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "password"
  }'
```

Lưu token vào biến:

```bash
export TOKEN="your-jwt-token-here"
export API_URL="http://localhost:8080"
```

### 2. Tạo Users (nếu chưa có)

#### 2.1. Tạo Tutor/Tutor

```bash
curl -X POST ${API_URL}/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "username": "tutor1",
    "email": "tutor1@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Tutor",
    "roleName": "TUTOR"
  }'
```

Lưu `userId` từ response:

```bash
export TUTOR_ID="tutor1"  # Thay bằng userId thực tế
```

#### 2.2. Tạo Student

```bash
curl -X POST ${API_URL}/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "username": "student1",
    "email": "student1@example.com",
    "password": "password123",
    "firstName": "Jane",
    "lastName": "Student",
    "roleName": "STUDENT"
  }'
```

Lưu `userId`:

```bash
export STUDENT_ID="student1"  # Thay bằng userId thực tế
```

**Lưu ý:** Nếu đã có users sẵn, có thể bỏ qua bước này và dùng `userId` có sẵn.

---

## Bước 1: Tạo Subject (nếu chưa có)

**Endpoint:** `POST /api/subjects`

**Request:**

```bash
curl -X POST ${API_URL}/api/subjects \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Accept-Language: vi" \
  -d '{
    "subjectName": "Lập trình Java"
  }'
```

**Response:**

```json
{
  "code": 1000,
  "result": {
    "subjectId": 1,
    "subjectName": "Lập trình Java"
  }
}
```

Lưu `subjectId`:

```bash
export SUBJECT_ID=1  # Thay bằng ID thực tế
```

**Lưu ý:** Nếu đã có subject, có thể bỏ qua và dùng subject có sẵn.

---

## Bước 2: Tạo Syllabus

**Endpoint:** `POST /api/syllabus`

**Request:**

```bash
curl -X POST ${API_URL}/api/syllabus \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Accept-Language: vi" \
  -d '{
    "subjectId": '${SUBJECT_ID}',
    "name": "Java Cơ bản",
    "levelVi": "Cơ bản",
    "levelEn": "Beginner",
    "targetVi": "Người mới bắt đầu học Java",
    "targetEn": "Beginners learning Java",
    "descriptionVi": "Giáo trình Java cơ bản cho người mới bắt đầu",
    "descriptionEn": "Basic Java syllabus for beginners",
    "status": "PUBLISHED"
  }'
```

**Response:**

```json
{
  "code": 1000,
  "result": {
    "syllabusId": 1,
    "name": "Java Cơ bản",
    "status": "PUBLISHED"
  }
}
```

Lưu `syllabusId`:

```bash
export SYLLABUS_ID=1  # Thay bằng ID thực tế
```

---

## Bước 3: Tạo Module

**Endpoint:** `POST /api/modules`

**Request:**

```bash
curl -X POST ${API_URL}/api/modules \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "syllabusId": '${SYLLABUS_ID}',
    "title": "Module 1: Giới thiệu Java",
    "orderNumber": 1,
    "status": "PUBLISHED"
  }'
```

**Response:**

```json
{
  "code": 1000,
  "result": {
    "moduleId": 1,
    "syllabusId": '${SYLLABUS_ID}',
    "title": "Module 1: Giới thiệu Java",
    "orderNumber": 1
  }
}
```

Lưu `moduleId`:

```bash
export MODULE_ID=1  # Thay bằng ID thực tế
```

---

## Bước 4: Tạo Lessons (ít nhất 2-3 lessons để test progress)

### 4.1. Lesson 1

```bash
curl -X POST ${API_URL}/api/lessons \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "moduleId": '${MODULE_ID}',
    "title": "Lesson 1: Cài đặt môi trường",
    "description": "Học cách cài đặt JDK và IDE",
    "orderNumber": 1,
    "durationMinutes": 30,
    "objectives": "Hiểu cách cài đặt môi trường phát triển Java",
    "status": "PUBLISHED"
  }'
```

Lưu `lessonId`:

```bash
export LESSON_1_ID=1  # Thay bằng ID thực tế
```

### 4.2. Lesson 2

```bash
curl -X POST ${API_URL}/api/lessons \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "moduleId": '${MODULE_ID}',
    "title": "Lesson 2: Hello World",
    "description": "Viết chương trình Java đầu tiên",
    "orderNumber": 2,
    "durationMinutes": 45,
    "objectives": "Viết và chạy chương trình Hello World",
    "status": "PUBLISHED"
  }'
```

```bash
export LESSON_2_ID=2  # Thay bằng ID thực tế
```

### 4.3. Lesson 3 (optional, để test progress percentage)

```bash
curl -X POST ${API_URL}/api/lessons \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "moduleId": '${MODULE_ID}',
    "title": "Lesson 3: Biến và Kiểu dữ liệu",
    "description": "Tìm hiểu về biến và các kiểu dữ liệu trong Java",
    "orderNumber": 3,
    "durationMinutes": 60,
    "objectives": "Hiểu về biến và kiểu dữ liệu",
    "status": "PUBLISHED"
  }'
```

```bash
export LESSON_3_ID=3  # Thay bằng ID thực tế
```

**Lưu ý:** Cần ít nhất 2-3 lessons để test progress percentage (ví dụ: 1/3 = 33%, 2/3 = 67%, 3/3 = 100%).

---

## Bước 5: Tạo Course

**Endpoint:** `POST /api/course`

**Request:**

```bash
curl -X POST ${API_URL}/api/course \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "syllabusId": '${SYLLABUS_ID}',
    "name": "Khóa học Java Cơ bản",
    "price": 500000,
    "isCombo": false,
    "type": "SELF_PACED",
    "description": "Khóa học Java cơ bản cho người mới bắt đầu",
    "tutorId": "'${TUTOR_ID}'"
  }'
```

**Response:**

```json
{
  "code": 1000,
  "result": {
    "id": 1,
    "name": "Khóa học Java Cơ bản",
    "syllabus": {
      "syllabusId": '${SYLLABUS_ID}'
    }
  }
}
```

Lưu `courseId`:

```bash
export COURSE_ID=1  # Thay bằng ID thực tế
```

---

## Bước 6: Tạo TutorClass

**Endpoint:** `POST /api/tutor/classes`

**Yêu cầu:** Phải đăng nhập với tài khoản TUTOR (tutor1).

**Lấy token của tutor:**

```bash
curl -X POST ${API_URL}/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "email": "tutor1@example.com",
    "password": "password123"
  }'
```

Lưu token:

```bash
export TUTOR_TOKEN="tutor-jwt-token"
```

**Lưu ý:** Tutor phải có TutorAvailability. Nếu chưa có, cần tạo trước:

```bash
curl -X PUT ${API_URL}/api/tutor/availability \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TUTOR_TOKEN}" \
  -d '{
    "mondaySlots": "1,2,3",
    "tuesdaySlots": "1,2,3",
    "wednesdaySlots": "1,2,3",
    "thursdaySlots": "1,2,3",
    "fridaySlots": "1,2,3",
    "isWorkOnMonday": true,
    "isWorkOnTuesday": true,
    "isWorkOnWednesday": true,
    "isWorkOnThursday": true,
    "isWorkOnFriday": true
  }'
```

**Tạo TutorClass:**

```bash
curl -X POST ${API_URL}/api/tutor/classes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TUTOR_TOKEN}" \
  -d '{
    "courseId": '${COURSE_ID}',
    "title": "Lớp Java T2-T4-T6",
    "description": "Lớp học Java vào thứ 2, 4, 6",
    "maxStudents": 10,
    "startDate": "2025-01-01",
    "endDate": "2025-03-31",
    "weeklySchedules": [
      {
        "dayOfWeek": "MONDAY",
        "slotNumbers": [1, 2]
      },
      {
        "dayOfWeek": "WEDNESDAY",
        "slotNumbers": [1, 2]
      },
      {
        "dayOfWeek": "FRIDAY",
        "slotNumbers": [1, 2]
      }
    ]
  }'
```

**Response:**

```json
{
  "code": 1000,
  "result": {
    "id": 1,
    "title": "Lớp Java T2-T4-T6",
    "course": {
      "id": '${COURSE_ID}'
    }
  }
}
```

Lưu `classId`:

```bash
export CLASS_ID=1  # Thay bằng ID thực tế
```

---

## Bước 7: Mời Student vào Class (Tạo ClassEnrollment + Progress tự động)

**Endpoint:** `POST /api/tutor/classes/invite`

**Yêu cầu:** Phải đăng nhập với tài khoản TUTOR.

**Request:**

```bash
curl -X POST ${API_URL}/api/tutor/classes/invite \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TUTOR_TOKEN}" \
  -d '{
    "classId": '${CLASS_ID}',
    "studentIds": ["'${STUDENT_ID}'"]
  }'
```

**Response:**

```json
{
  "code": 1000,
  "message": "Students invited successfully"
}
```

**Quan trọng:** Sau khi mời student, hệ thống sẽ **tự động**:
1. Tạo `ClassEnrollment` cho student
2. Tạo `CourseProgress` cho enrollment
3. Tạo `LessonProgress` cho **tất cả lessons** trong course (từ Syllabus → Module → Lesson)

---

## Bước 8: Lấy Enrollment ID

Để test progress, bạn cần `enrollmentId`. Có thể lấy từ database hoặc thông qua API (nếu có).

**Tạm thời:** Có thể query database:

```sql
SELECT id, student_id, class_id 
FROM class_enrollment 
WHERE student_id = 'student1' AND class_id = 1;
```

Hoặc nếu có API lấy enrollments của student:

```bash
# Giả sử có API này (cần kiểm tra)
curl -X GET ${API_URL}/api/students/enrollments \
  -H "Authorization: Bearer ${STUDENT_TOKEN}"
```

Lưu `enrollmentId`:

```bash
export ENROLLMENT_ID=1  # Thay bằng ID thực tế
```

---

## Bước 9: Test Progress APIs

### 9.1. Lấy Course Progress

**Endpoint:** `GET /api/progress/enrollments/{enrollmentId}`

```bash
curl -X GET ${API_URL}/api/progress/enrollments/${ENROLLMENT_ID} \
  -H "Authorization: Bearer ${STUDENT_TOKEN}"
```

**Response mong đợi:**

```json
{
  "code": 1000,
  "result": {
    "courseProgressId": 1,
    "enrollmentId": '${ENROLLMENT_ID}',
    "status": "NOT_STARTED",
    "totalLessons": 3,
    "completedLessons": 0,
    "progressPercentage": 0,
    "lessons": [
      {
        "lessonId": '${LESSON_1_ID}',
        "status": "NOT_STARTED",
        "optionalLesson": false
      },
      {
        "lessonId": '${LESSON_2_ID}',
        "status": "NOT_STARTED",
        "optionalLesson": false
      },
      {
        "lessonId": '${LESSON_3_ID}',
        "status": "NOT_STARTED",
        "optionalLesson": false
      }
    ]
  }
}
```

### 9.2. Update Lesson Progress (Khi học viên xem lesson)

**Endpoint:** `PUT /api/progress/lessons`

**Khi học viên bắt đầu xem lesson:**

```bash
curl -X PUT ${API_URL}/api/progress/lessons \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${STUDENT_TOKEN}" \
  -d '{
    "enrollmentId": '${ENROLLMENT_ID}',
    "lessonId": '${LESSON_1_ID}',
    "status": "IN_PROGRESS"
  }'
```

**Khi học viên hoàn thành lesson:**

```bash
curl -X PUT ${API_URL}/api/progress/lessons \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${STUDENT_TOKEN}" \
  -d '{
    "enrollmentId": '${ENROLLMENT_ID}',
    "lessonId": '${LESSON_1_ID}',
    "status": "COMPLETED"
  }'
```

**Khi học viên làm quiz và có kết quả:**

```bash
curl -X PUT ${API_URL}/api/progress/lessons \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${STUDENT_TOKEN}" \
  -d '{
    "enrollmentId": '${ENROLLMENT_ID}',
    "lessonId": '${LESSON_1_ID}',
    "status": "COMPLETED",
    "result": "PASSED"
  }'
```

### 9.3. Kiểm tra Progress sau khi update

```bash
curl -X GET ${API_URL}/api/progress/enrollments/${ENROLLMENT_ID} \
  -H "Authorization: Bearer ${STUDENT_TOKEN}"
```

**Response mong đợi (sau khi hoàn thành 1 lesson):**

```json
{
  "code": 1000,
  "result": {
    "courseProgressId": 1,
    "enrollmentId": '${ENROLLMENT_ID}',
    "status": "IN_PROGRESS",
    "totalLessons": 3,
    "completedLessons": 1,
    "progressPercentage": 33,
    "lessons": [
      {
        "lessonId": '${LESSON_1_ID}',
        "status": "COMPLETED",
        "result": "PASSED",
        "lastAccessedAt": "2025-01-15T10:30:00"
      },
      {
        "lessonId": '${LESSON_2_ID}',
        "status": "NOT_STARTED"
      },
      {
        "lessonId": '${LESSON_3_ID}',
        "status": "NOT_STARTED"
      }
    ]
  }
}
```

### 9.4. Test Skip Optional Lesson (nếu có)

Nếu lesson có `optionalLesson = true`:

```bash
curl -X PUT ${API_URL}/api/progress/lessons \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${STUDENT_TOKEN}" \
  -d '{
    "enrollmentId": '${ENROLLMENT_ID}',
    "lessonId": '${LESSON_3_ID}',
    "status": "SKIPPED",
    "skipOptional": true
  }'
```

### 9.5. Test Failed Lesson

```bash
curl -X PUT ${API_URL}/api/progress/lessons \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${STUDENT_TOKEN}" \
  -d '{
    "enrollmentId": '${ENROLLMENT_ID}',
    "lessonId": '${LESSON_2_ID}',
    "status": "FAILED",
    "result": "FAILED"
  }'
```

Sau đó kiểm tra course progress sẽ có `status: "FAILED"`.

---

## Bước 10: Test Integration với Video Streaming

Khi học viên xem video lesson, progress sẽ tự động update:

```bash
curl -X GET "${API_URL}/api/video-lessons/{videoLessonId}/stream?enrollmentId=${ENROLLMENT_ID}" \
  -H "Authorization: Bearer ${STUDENT_TOKEN}"
```

Hệ thống sẽ tự động:
1. Update `LessonProgress.status = IN_PROGRESS`
2. Update `LessonProgress.lastAccessedAt`
3. Recalculate `CourseProgress`

---

## Checklist Test Cases

- [ ] Tạo dữ liệu từ đầu (Subject → Syllabus → Module → Lesson → Course → TutorClass)
- [ ] Mời student vào class → Kiểm tra CourseProgress và LessonProgress được tạo tự động
- [ ] Lấy course progress → Kiểm tra status = NOT_STARTED, progressPercentage = 0
- [ ] Update lesson progress → IN_PROGRESS → Kiểm tra course progress status = IN_PROGRESS
- [ ] Update lesson progress → COMPLETED → Kiểm tra completedLessons tăng, percentage tăng
- [ ] Hoàn thành tất cả lessons → Kiểm tra status = COMPLETED, percentage = 100
- [ ] Test failed lesson → Kiểm tra course progress status = FAILED
- [ ] Test skip optional lesson → Kiểm tra lesson status = SKIPPED, vẫn tính vào completed
- [ ] Test integration với video streaming → Kiểm tra progress tự động update
- [ ] Test với quiz result → Kiểm tra result được lưu trong LessonProgress

---

## Troubleshooting

### Lỗi: "CourseProgress not found for enrollment"

**Nguyên nhân:** ClassEnrollment chưa có CourseProgress.

**Giải pháp:** 
- Kiểm tra xem đã mời student vào class chưa
- Kiểm tra xem `TutorClassService.inviteStudents()` có gọi `progressService.createCourseProgress()` không

### Lỗi: "LessonProgress not found"

**Nguyên nhân:** Course chưa có lessons hoặc progress chưa được tạo đầy đủ.

**Giải pháp:**
- Kiểm tra Syllabus có Module và Lesson không
- Kiểm tra Course có link đúng Syllabus không
- Kiểm tra `ProgressService.createCourseProgress()` có tạo LessonProgress cho tất cả lessons không

### Progress percentage không đúng

**Nguyên nhân:** Logic tính toán hoặc completedLessons không được update.

**Giải pháp:**
- Kiểm tra `ProgressService.recalculateCourseProgress()`
- Kiểm tra xem lesson status có đúng không (COMPLETED, SKIPPED)
- Kiểm tra xem optional lessons có được tính đúng không

---

## Script tự động (tùy chọn)

Có thể tạo script bash để tự động tạo tất cả dữ liệu:

```bash
#!/bin/bash
# progress-test-setup.sh

# Set variables
API_URL="http://localhost:8080"
TOKEN="your-admin-token"

# 1. Create Subject
SUBJECT_ID=$(curl -s -X POST ${API_URL}/api/subjects ... | jq -r '.result.subjectId')

# 2. Create Syllabus
SYLLABUS_ID=$(curl -s -X POST ${API_URL}/api/syllabus ... | jq -r '.result.syllabusId')

# ... tiếp tục các bước khác

echo "Setup completed! Enrollment ID: ${ENROLLMENT_ID}"
```

---

## Tài liệu liên quan

- [Progress Tracking API Documentation](./progress-tracking.md)
- [Course Structure Relationships](./course-structure-relationships.md)

