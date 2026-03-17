# Hướng dẫn Test Exam APIs

Tài liệu này hướng dẫn chi tiết cách test các API Exam cho Student và Tutor.

## Tổng quan

Hệ thống Exam bao gồm các tính năng:

### Student APIs:

- `GET /api/student/exams/{examId}` - Lấy exam để làm (ẩn đáp án)
- `POST /api/student/exams/{examId}/submit` - Nộp bài exam
- `GET /api/student/exams` - Danh sách exam của student (có phân trang)

### Tutor APIs:

- `GET /api/tutor/exams/{examId}/results` - Xem kết quả exam của tất cả students

---

## Chuẩn bị

### 1. Authentication

Đăng nhập để lấy JWT token:

```bash
# Đăng nhập với Student
curl -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "student1",
    "password": "password123"
  }'
```

Lưu token vào biến:

```bash
export STUDENT_TOKEN="your-student-jwt-token-here"
export TUTOR_TOKEN="your-tutor-jwt-token-here"
```

### 2. Tạo dữ liệu cơ bản

Để test Exam APIs, bạn cần có:

1. **Subject** (nếu chưa có)
2. **Syllabus** với **Module** và **Lesson**
3. **Course** liên kết với Syllabus
4. **TutorClass** cho Course
5. **ClassEnrollment** - Student đã enroll vào class
6. **Exam** với **Quiz** và **QuizOption** trong Lesson

**Lưu ý:** Xem file `progress-testing-guide.md` để biết cách tạo dữ liệu từ đầu.

**QUAN TRỌNG:** Nếu bạn nhận được kết quả rỗng khi gọi `GET /api/student/exams`, hãy kiểm tra:

- ✅ Student đã enroll vào TutorClass chưa?
- ✅ Course có Syllabus với Lesson chưa?
- ✅ Lesson có Exam với status = PUBLISHED chưa?

Xem phần **"Hướng dẫn Setup Dữ liệu Test Nhanh"** bên dưới để tạo đầy đủ dữ liệu.

### 3. Tạo Exam và Quiz

#### 3.1. Tạo Exam

**Endpoint:** `POST /api/exams`

**Request:**

```bash
curl -X POST http://localhost:8080/api/exams \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TUTOR_TOKEN}" \
  -d '{
    "lessonId": 1,
    "status": "PUBLISHED",
    "field": "Kiểm tra giữa kỳ - Chương 1"
  }'
```

**Response:**

```json
{
  "code": 1000,
  "result": {
    "examId": 1,
    "lessonId": 1,
    "status": "PUBLISHED",
    "field": "Kiểm tra giữa kỳ - Chương 1",
    "quizzes": [],
    "createdAt": "2024-01-15T10:00:00",
    "modifiedAt": "2024-01-15T10:00:00"
  }
}
```

Lưu `examId`:

```bash
export EXAM_ID=1
```

#### 3.2. Tạo Quiz (Câu hỏi)

**Endpoint:** `POST /api/exams/{examId}/quizzes`

**Lưu ý:** Thay `${EXAM_ID}` bằng examId thực tế (ví dụ: `1`, `16`). Nếu dùng Postman, đảm bảo biến được set đúng.

**Request - Single Choice:**

```bash
curl -X POST http://localhost:8080/api/exams/1/quizzes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TUTOR_TOKEN}" \
  -d '{
    "text": "Tốc độ ánh sáng trong chân không xấp xỉ bằng bao nhiêu?",
    "orderNo": 1,
    "type": "SINGLE_CHOICE",
    "validAnswer": "B",
    "explanation": "Giá trị chuẩn quốc tế là 299,792,458 m/s.",
    "options": [
      {
        "text": "150,000,000 m/s",
        "isCorrect": false
      },
      {
        "text": "299,792,458 m/s",
        "isCorrect": true
      },
      {
        "text": "3,000,000 m/s",
        "isCorrect": false
      },
      {
        "text": "30,000 km/s",
        "isCorrect": false
      }
    ]
  }'
```

**Request - Multiple Choice:**

```bash
curl -X POST http://localhost:8080/api/exams/1/quizzes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TUTOR_TOKEN}" \
  -d '{
    "text": "Hãy chọn các nguyên tố thuộc nhóm halogen:",
    "orderNo": 2,
    "type": "MULTIPLE_CHOICE",
    "validAnswer": "A,B,C,D",
    "explanation": "Tất cả các nguyên tố trên đều thuộc nhóm halogen trong bảng tuần hoàn.",
    "options": [
      {
        "text": "Fluor",
        "isCorrect": true
      },
      {
        "text": "Chlor",
        "isCorrect": true
      },
      {
        "text": "Brom",
        "isCorrect": true
      },
      {
        "text": "Iod",
        "isCorrect": true
      }
    ]
  }'
```

**Request - True/False:**

```bash
curl -X POST http://localhost:8080/api/exams/1/quizzes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TUTOR_TOKEN}" \
  -d '{
    "text": "Trái Đất quay quanh Mặt Trời?",
    "orderNo": 3,
    "type": "TRUE_FALSE",
    "validAnswer": "A",
    "explanation": "Trái Đất quay quanh Mặt Trời theo quỹ đạo hình elip.",
    "options": [
      {
        "text": "True",
        "isCorrect": true
      },
      {
        "text": "False",
        "isCorrect": false
      }
    ]
  }'
```

---

## Test Student APIs

### 1. Lấy danh sách Exam của Student

**Endpoint:** `GET /api/student/exams`

**Request:**

```bash
curl -X GET "http://localhost:8080/api/student/exams?page=0&size=10" \
  -H "Authorization: Bearer ${STUDENT_TOKEN}"
```

**Response:**

```json
{
  "code": 1000,
  "result": {
    "content": [
      {
        "examId": 1,
        "lessonId": 1,
        "lessonTitle": "Bài 1: Giới thiệu về Vật lý",
        "examTitle": "Kiểm tra giữa kỳ - Chương 1",
        "status": "PUBLISHED",
        "submitted": false,
        "bestScore": null,
        "submissionCount": 0
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10
    },
    "totalElements": 1,
    "totalPages": 1
  }
}
```

**Test Cases:**

- ✅ Student chỉ thấy exam của các lessons trong courses đã enroll
- ✅ Chỉ hiển thị exam có status = PUBLISHED
- ✅ Hiển thị `submitted`, `bestScore`, `submissionCount` chính xác

---

### 2. Lấy Exam để làm (ẩn đáp án)

**Endpoint:** `GET /api/student/exams/{examId}`

**Request:**

```bash
curl -X GET http://localhost:8080/api/student/exams/1 \
  -H "Authorization: Bearer ${STUDENT_TOKEN}"
```

**Response:**

```json
{
  "code": 1000,
  "result": {
    "examId": 1,
    "lessonId": 1,
    "status": "PUBLISHED",
    "field": "Kiểm tra giữa kỳ - Chương 1",
    "quizzes": [
      {
        "quizId": 1,
        "examId": 1,
        "text": "Tốc độ ánh sáng trong chân không xấp xỉ bằng bao nhiêu?",
        "orderNo": 1,
        "type": "SINGLE_CHOICE",
        "options": [
          {
            "optionId": 1,
            "quizId": 1,
            "text": "150,000,000 m/s",
            "isCorrect": null
          },
          {
            "optionId": 2,
            "quizId": 1,
            "text": "299,792,458 m/s",
            "isCorrect": null
          },
          {
            "optionId": 3,
            "quizId": 1,
            "text": "3,000,000 m/s",
            "isCorrect": null
          },
          {
            "optionId": 4,
            "quizId": 1,
            "text": "30,000 km/s",
            "isCorrect": null
          }
        ]
      }
    ],
    "createdAt": "2024-01-15T10:00:00",
    "modifiedAt": "2024-01-15T10:00:00"
  }
}
```

**Lưu ý quan trọng:**

- ✅ `validAnswer` và `explanation` **KHÔNG** có trong response (đã bị ẩn)
- ✅ `isCorrect` trong options cũng bị ẩn (null)

**Test Cases:**

- ✅ Student chỉ có thể lấy exam của lessons trong courses đã enroll
- ✅ Chỉ có thể lấy exam có status = PUBLISHED
- ✅ Nếu exam status = DRAFT hoặc ARCHIVED → Error: `EXAM_NOT_PUBLISHED`
- ✅ Nếu student chưa enroll course → Error: `EXAM_NOT_ACCESSIBLE`

---

### 3. Nộp bài Exam

**Endpoint:** `POST /api/student/exams/{examId}/submit`

**Request:**

```bash
curl -X POST http://localhost:8080/api/student/exams/1/submit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${STUDENT_TOKEN}" \
  -d '{
    "startedAt": "2024-01-15T10:00:00",
    "answers": [
      {
        "quizId": 1,
        "answer": "B"
      },
      {
        "quizId": 2,
        "answer": "A,B,C,D"
      },
      {
        "quizId": 3,
        "answer": "A"
      }
    ]
  }'
```

**Response:**

```json
{
  "code": 1000,
  "result": {
    "submissionId": 1,
    "score": 100.0,
    "totalQuestions": 3,
    "correctAnswers": 3,
    "submittedAt": "2024-01-15T10:15:30",
    "durationSeconds": 930,
    "answers": [
      {
        "quizId": 1,
        "studentAnswer": "B",
        "correctAnswer": "B",
        "isCorrect": true,
        "explanation": "Giá trị chuẩn quốc tế là 299,792,458 m/s."
      },
      {
        "quizId": 2,
        "studentAnswer": "A,B,C,D",
        "correctAnswer": "A,B,C,D",
        "isCorrect": true,
        "explanation": "Tất cả các nguyên tố trên đều thuộc nhóm halogen trong bảng tuần hoàn."
      },
      {
        "quizId": 3,
        "studentAnswer": "A",
        "correctAnswer": "A",
        "isCorrect": true,
        "explanation": "Trái Đất quay quanh Mặt Trời theo quỹ đạo hình elip."
      }
    ]
  }
}
```

**Lưu ý:**

- ✅ Response hiển thị đầy đủ: `correctAnswer`, `isCorrect`, `explanation`
- ✅ `score` được tính: (correctAnswers / totalQuestions) \* 100
- ✅ `durationSeconds` được tính tự động từ `startedAt` đến `submittedAt`

**Test Cases:**

#### 3.1. Test với câu trả lời đúng

```json
{
  "startedAt": "2024-01-15T10:00:00",
  "answers": [
    {
      "quizId": 1,
      "answer": "B"
    }
  ]
}
```

**Kết quả mong đợi:** `isCorrect: true`, `score: 100.0`

#### 3.2. Test với câu trả lời sai

```json
{
  "startedAt": "2024-01-15T10:00:00",
  "answers": [
    {
      "quizId": 1,
      "answer": "A"
    }
  ]
}
```

**Kết quả mong đợi:** `isCorrect: false`, `score: 0.0`

#### 3.3. Test Multiple Choice (thứ tự khác nhau)

```json
{
  "startedAt": "2024-01-15T10:00:00",
  "answers": [
    {
      "quizId": 2,
      "answer": "B,C,A,D"
    }
  ]
}
```

**Kết quả mong đợi:** `isCorrect: true` (hệ thống tự động sort và so sánh)

#### 3.4. Test Multiple Choice (thiếu đáp án)

```json
{
  "startedAt": "2024-01-15T10:00:00",
  "answers": [
    {
      "quizId": 2,
      "answer": "A,B"
    }
  ]
}
```

**Kết quả mong đợi:** `isCorrect: false`

#### 3.5. Test với quizId không thuộc exam

```json
{
  "startedAt": "2024-01-15T10:00:00",
  "answers": [
    {
      "quizId": 999,
      "answer": "A"
    }
  ]
}
```

**Kết quả mong đợi:** Error: `QUIZ_NOT_EXISTED`

#### 3.6. Test với student chưa enroll

- Dùng token của student khác chưa enroll course
- **Kết quả mong đợi:** Error: `EXAM_NOT_ACCESSIBLE`

---

## Test Tutor APIs

### 1. Xem kết quả Exam

**Endpoint:** `GET /api/tutor/exams/{examId}/results`

**Request:**

```bash
curl -X GET http://localhost:8080/api/tutor/exams/1/results \
  -H "Authorization: Bearer ${TUTOR_TOKEN}"
```

**Response:**

```json
{
  "code": 1000,
  "result": {
    "examId": 1,
    "examTitle": "Kiểm tra giữa kỳ - Chương 1",
    "lessonTitle": "Bài 1: Giới thiệu về Vật lý",
    "totalSubmissions": 2,
    "results": [
      {
        "submissionId": 1,
        "studentId": "student-uuid-1",
        "studentName": "Nguyễn Văn A",
        "score": 100.0,
        "totalQuestions": 3,
        "correctAnswers": 3,
        "submittedAt": "2024-01-15T10:15:30",
        "durationSeconds": 930,
        "answers": [
          {
            "quizId": 1,
            "studentAnswer": "B",
            "correctAnswer": "B",
            "isCorrect": true,
            "explanation": "Giá trị chuẩn quốc tế là 299,792,458 m/s."
          },
          {
            "quizId": 2,
            "studentAnswer": "A,B,C,D",
            "correctAnswer": "A,B,C,D",
            "isCorrect": true,
            "explanation": "Tất cả các nguyên tố trên đều thuộc nhóm halogen trong bảng tuần hoàn."
          },
          {
            "quizId": 3,
            "studentAnswer": "A",
            "correctAnswer": "A",
            "isCorrect": true,
            "explanation": "Trái Đất quay quanh Mặt Trời theo quỹ đạo hình elip."
          }
        ]
      },
      {
        "submissionId": 2,
        "studentId": "student-uuid-2",
        "studentName": "Trần Thị B",
        "score": 66.67,
        "totalQuestions": 3,
        "correctAnswers": 2,
        "submittedAt": "2024-01-15T11:20:15",
        "durationSeconds": 1200,
        "answers": [
          {
            "quizId": 1,
            "studentAnswer": "A",
            "correctAnswer": "B",
            "isCorrect": false,
            "explanation": "Giá trị chuẩn quốc tế là 299,792,458 m/s."
          },
          {
            "quizId": 2,
            "studentAnswer": "A,B,C,D",
            "correctAnswer": "A,B,C,D",
            "isCorrect": true,
            "explanation": "Tất cả các nguyên tố trên đều thuộc nhóm halogen trong bảng tuần hoàn."
          },
          {
            "quizId": 3,
            "studentAnswer": "A",
            "correctAnswer": "A",
            "isCorrect": true,
            "explanation": "Trái Đất quay quanh Mặt Trời theo quỹ đạo hình elip."
          }
        ]
      }
    ]
  }
}
```

**Test Cases:**

#### 1.1. Tutor xem kết quả exam của course mình dạy

- ✅ Hiển thị tất cả submissions của exam
- ✅ Hiển thị đầy đủ thông tin: student name, score, answers, explanation

#### 1.2. Tutor xem kết quả exam của course khác

- Dùng token của tutor khác
- **Kết quả mong đợi:** Error: `INVALID_EXAM_OWNER`

#### 1.3. Exam chưa có submission nào

- Tạo exam mới, chưa có student nào submit
- **Kết quả mong đợi:** `totalSubmissions: 0`, `results: []`

---

## Test Cases - Edge Cases

### 1. Validation Errors

#### 1.1. Exam không tồn tại

```bash
curl -X GET http://localhost:8080/api/student/exams/999 \
  -H "Authorization: Bearer ${STUDENT_TOKEN}"
```

**Kết quả:** Error: `EXAM_NOT_EXISTED` (3051)

#### 1.2. Exam status = DRAFT

- Tạo exam với status = "DRAFT"
- Student cố gắng lấy exam
- **Kết quả:** Error: `EXAM_NOT_PUBLISHED` (3057)

#### 1.3. Student chưa enroll course

- Dùng token của student chưa enroll course chứa exam
- **Kết quả:** Error: `EXAM_NOT_ACCESSIBLE` (3056)

#### 1.4. Submit với answers rỗng

```json
{
  "startedAt": "2024-01-15T10:00:00",
  "answers": []
}
```

**Kết quả:** Validation error: "Answers list cannot be empty"

#### 1.5. Submit thiếu startedAt

```json
{
  "answers": [
    {
      "quizId": 1,
      "answer": "B"
    }
  ]
}
```

**Kết quả:** Validation error: "Started at time is required"

### 2. Tính điểm

#### 2.1. Một phần câu trả lời đúng

- Exam có 3 câu, student trả lời đúng 2 câu
- **Kết quả:** `score: 66.67`, `correctAnswers: 2`

#### 2.2. Tất cả câu trả lời sai

- Exam có 3 câu, student trả lời sai tất cả
- **Kết quả:** `score: 0.0`, `correctAnswers: 0`

#### 2.3. Case-insensitive comparison

```json
{
  "quizId": 1,
  "answer": "b" // lowercase
}
```

**Kết quả:** `isCorrect: true` (hệ thống tự động convert sang uppercase)

#### 2.4. Trim whitespace

```json
{
  "quizId": 1,
  "answer": "  B  " // có spaces
}
```

**Kết quả:** `isCorrect: true` (hệ thống tự động trim)

### 3. Multiple Choice

#### 3.1. Thứ tự khác nhau nhưng đúng

```json
{
  "quizId": 2,
  "answer": "D,C,B,A" // đáp án đúng là "A,B,C,D"
}
```

**Kết quả:** `isCorrect: true` (hệ thống sort trước khi so sánh)

#### 3.2. Có duplicate

```json
{
  "quizId": 2,
  "answer": "A,A,B,C" // có duplicate
}
```

**Kết quả:** Cần test thực tế (có thể cần normalize)

### 4. Phân trang

#### 4.1. Lấy danh sách exam với phân trang

```bash
curl -X GET "http://localhost:8080/api/student/exams?page=0&size=5" \
  -H "Authorization: Bearer ${STUDENT_TOKEN}"
```

**Kết quả:** Trả về tối đa 5 exam

#### 4.2. Page không hợp lệ

```bash
curl -X GET "http://localhost:8080/api/student/exams?page=-1&size=10" \
  -H "Authorization: Bearer ${STUDENT_TOKEN}"
```

**Kết quả:** Spring tự động xử lý (page >= 0)

---

## Luồng test hoàn chỉnh

### Scenario 1: Student làm exam lần đầu

1. **Lấy danh sách exam**

   ```bash
   GET /api/student/exams
   ```

   → Thấy exam, `submitted: false`

2. **Lấy exam để làm**

   ```bash
   GET /api/student/exams/{examId}
   ```

   → Nhận exam với đáp án bị ẩn

3. **Nộp bài**

   ```bash
   POST /api/student/exams/{examId}/submit
   ```

   → Nhận kết quả với điểm số và đáp án đúng

4. **Kiểm tra lại danh sách**
   ```bash
   GET /api/student/exams
   ```
   → `submitted: true`, `bestScore: 100.0`, `submissionCount: 1`

### Scenario 2: Student làm lại exam (cải thiện điểm)

1. **Nộp bài lần 2 với điểm thấp hơn**

   ```bash
   POST /api/student/exams/{examId}/submit
   ```

   → `score: 66.67`

2. **Kiểm tra danh sách**

   ```bash
   GET /api/student/exams
   ```

   → `bestScore: 100.0` (giữ điểm cao nhất), `submissionCount: 2`

3. **Nộp bài lần 3 với điểm cao hơn**

   ```bash
   POST /api/student/exams/{examId}/submit
   ```

   → `score: 83.33`

4. **Kiểm tra lại**
   ```bash
   GET /api/student/exams
   ```
   → `bestScore: 100.0` (vẫn giữ điểm cao nhất)

### Scenario 3: Tutor xem kết quả

1. **Xem kết quả exam**

   ```bash
   GET /api/tutor/exams/{examId}/results
   ```

   → Thấy tất cả submissions của students

2. **Kiểm tra thông tin chi tiết**
   - Student name
   - Score
   - Answers của từng câu
   - Explanation

---

## Lưu ý quan trọng

1. **Security:**

   - Student chỉ có thể làm exam của courses đã enroll
   - Tutor chỉ có thể xem kết quả exam của courses mình dạy
   - Tất cả endpoints đều yêu cầu authentication

2. **Data Integrity:**

   - Exam phải có status = PUBLISHED mới cho phép student làm
   - QuizId trong submission phải thuộc về exam đó

3. **Performance:**

   - Danh sách exam có phân trang (mặc định 10 items/page)
   - Có thể điều chỉnh `page` và `size` parameters

4. **Answer Format:**
   - Single Choice: `"A"`, `"B"`, `"C"`, `"D"`
   - Multiple Choice: `"A,B,C"` hoặc `"A, B, C"` (có thể có spaces)
   - True/False: `"A"` (True) hoặc `"B"` (False)

---

## Troubleshooting

### Lỗi: EXAM_NOT_ACCESSIBLE

**Nguyên nhân:** Student chưa enroll course chứa exam
**Giải pháp:** Tạo ClassEnrollment cho student vào TutorClass

### Lỗi: EXAM_NOT_PUBLISHED

**Nguyên nhân:** Exam có status = DRAFT hoặc ARCHIVED
**Giải pháp:** Cập nhật exam status = PUBLISHED

### Lỗi: INVALID_EXAM_OWNER

**Nguyên nhân:** Tutor không phải owner của course chứa exam
**Giải pháp:** Dùng token của tutor đúng (tutor tạo course)

### Lỗi: QUIZ_NOT_EXISTED

**Nguyên nhân:** QuizId trong submission không thuộc về exam
**Giải pháp:** Kiểm tra lại quizId trong request

### Lỗi: INVALID_KEY (1001) - "Yêu cầu không hợp lệ"

**Nguyên nhân phổ biến:**

1. **ExamId trong URL không hợp lệ**: URL có chứa ký tự không phải số (ví dụ: `$16` thay vì `16`)

   - **Giải pháp:** Đảm bảo examId trong URL là số nguyên (ví dụ: `/api/exams/16/quizzes` thay vì `/api/exams/$16/quizzes`)
   - Nếu dùng Postman với biến, đảm bảo biến được set đúng giá trị số

2. **JSON format không hợp lệ**: Request body có cú pháp JSON sai

   - **Giải pháp:** Kiểm tra lại JSON format, đảm bảo các dấu ngoặc, dấu phẩy đúng

3. **Thiếu required fields**: Request body thiếu các trường bắt buộc
   - **Giải pháp:** Kiểm tra lại các trường: `text`, `orderNo`, `type`, `options`

**Ví dụ lỗi:**

```bash
# ❌ SAI - có ký tự $ trong URL
curl -X POST http://localhost:8080/api/exams/$16/quizzes

# ✅ ĐÚNG - examId là số
curl -X POST http://localhost:8080/api/exams/16/quizzes
```

---

## Kết luận

Tài liệu này cung cấp hướng dẫn đầy đủ để test tất cả các tính năng Exam. Hãy đảm bảo:

1. ✅ Đã tạo đầy đủ dữ liệu (Course, Lesson, Exam, Quiz)
2. ✅ Student đã enroll vào class
3. ✅ Exam có status = PUBLISHED
4. ✅ Sử dụng đúng token (STUDENT_TOKEN hoặc TUTOR_TOKEN)

Nếu gặp vấn đề, kiểm tra lại các bước chuẩn bị và validation errors.

---

## Hướng dẫn Setup Dữ liệu Test Nhanh

Nếu bạn nhận được kết quả rỗng (`"content": []`), hãy làm theo các bước sau để tạo dữ liệu test:

### Bước 1: Lấy User IDs

**Lấy Student ID:**

```bash
# Đăng nhập với student và lấy userId từ JWT token hoặc
curl -X GET http://localhost:8080/api/users/my-info \
  -H "Authorization: Bearer ${STUDENT_TOKEN}"
```

**Lấy Tutor ID:**

```bash
# Đăng nhập với tutor và lấy userId
curl -X GET http://localhost:8080/api/users/my-info \
  -H "Authorization: Bearer ${TUTOR_TOKEN}"
```

Lưu các IDs:

```bash
export STUDENT_USER_ID="b1074c6a-0979-436e-ae81-b0bb3296fde6"  # Thay bằng userId thực tế
export TUTOR_USER_ID="388dcc33-1ff1-4d5a-89ae-c03d9984f706"  # Thay bằng userId thực tế
```

### Bước 2: Tạo Subject (nếu chưa có)

```bash
curl -X POST http://localhost:8080/api/subjects \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TUTOR_TOKEN}" \
  -H "Accept-Language: vi" \
  -d '{
    "nameVi": "Toán học",
    "nameEn": "Mathematics"
  }'
```

Lưu `subjectId`:

```bash
export SUBJECT_ID=1  # Thay bằng ID thực tế
```

### Bước 3: Tạo Syllabus

```bash
curl -X POST http://localhost:8080/api/syllabus \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TUTOR_TOKEN}" \
  -H "Accept-Language: vi" \
  -d '{
    "subjectIds": ['${SUBJECT_ID}'],
    "nameVi": "Giáo trình Toán cơ bản",
    "nameEn": "Basic Mathematics Syllabus",
    "descriptionVi": "Giáo trình cho người mới bắt đầu",
    "descriptionEn": "Syllabus for beginners"
  }'
```

Lưu `syllabusId`:

```bash
export SYLLABUS_ID=1  # Thay bằng ID thực tế
```

### Bước 4: Tạo Module

```bash
curl -X POST http://localhost:8080/api/syllabus/'${SYLLABUS_ID}'/modules \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TUTOR_TOKEN}" \
  -H "Accept-Language: vi" \
  -d '{
    "titleVi": "Module 1: Cơ bản",
    "titleEn": "Module 1: Basics",
    "orderNumber": 1
  }'
```

Lưu `moduleId`:

```bash
export MODULE_ID=1  # Thay bằng ID thực tế
```

### Bước 5: Tạo Lesson

```bash
curl -X POST http://localhost:8080/api/lessons \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TUTOR_TOKEN}" \
  -d '{
    "moduleId": '${MODULE_ID}',
    "title": "Bài 1: Giới thiệu về Toán học",
    "description": "Bài học đầu tiên về toán học",
    "orderNumber": 1,
    "durationMinutes": 60,
    "objectives": "Hiểu cơ bản về toán học",
    "status": "PUBLISHED"
  }'
```

Lưu `lessonId`:

```bash
export LESSON_ID=1  # Thay bằng ID thực tế
```

### Bước 6: Tạo Course

```bash
curl -X POST http://localhost:8080/api/course \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TUTOR_TOKEN}" \
  -d '{
    "syllabusId": '${SYLLABUS_ID}',
    "name": "Khóa học Toán cơ bản",
    "price": 500000,
    "isCombo": false,
    "type": "SELF_PACED",
    "description": "Khóa học toán cơ bản cho người mới bắt đầu",
    "tutorId": "'${TUTOR_USER_ID}'"
  }'
```

Lưu `courseId`:

```bash
export COURSE_ID=1  # Thay bằng ID thực tế
```

### Bước 7: Tạo TutorClass

**Lưu ý:** Tutor cần có TutorAvailability trước. Nếu chưa có:

```bash
curl -X PUT http://localhost:8080/api/tutor/availability \
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
curl -X POST http://localhost:8080/api/tutor/classes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TUTOR_TOKEN}" \
  -d '{
    "courseId": '${COURSE_ID}',
    "title": "Lớp Toán T2-T4-T6",
    "description": "Lớp học Toán vào thứ 2, 4, 6",
    "maxStudents": 10,
    "startDate": "2025-01-01",
    "endDate": "2025-03-31",
    "weeklySchedules": [
      {
        "dayOfWeek": "MONDAY",
        "slotNumbers": [1, 2]
      }
    ]
  }'
```

Lưu `classId`:

```bash
export CLASS_ID=1  # Thay bằng ID thực tế
```

### Bước 8: Mời Student vào Class (Tạo ClassEnrollment)

**QUAN TRỌNG:** Đây là bước bắt buộc để student có thể thấy exam!

```bash
curl -X POST http://localhost:8080/api/tutor/classes/invite \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TUTOR_TOKEN}" \
  -d '{
    "classId": '${CLASS_ID}',
    "studentIds": ["'${STUDENT_USER_ID}'"]
  }'
```

**Response mong đợi:**

```json
{
  "code": 1000,
  "message": "Students invited successfully"
}
```

**Sau bước này:**

- ✅ ClassEnrollment được tạo
- ✅ CourseProgress được tạo tự động
- ✅ LessonProgress được tạo tự động cho tất cả lessons

### Bước 9: Tạo Exam (với status = PUBLISHED)

```bash
curl -X POST http://localhost:8080/api/exams \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TUTOR_TOKEN}" \
  -d '{
    "lessonId": '${LESSON_ID}',
    "status": "PUBLISHED",
    "field": "Kiểm tra giữa kỳ - Chương 1"
  }'
```

Lưu `examId`:

```bash
export EXAM_ID=1  # Thay bằng ID thực tế
```

### Bước 10: Tạo Quiz cho Exam

```bash
curl -X POST http://localhost:8080/api/exams/'${EXAM_ID}'/quizzes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TUTOR_TOKEN}" \
  -d '{
    "text": "Tốc độ ánh sáng trong chân không xấp xỉ bằng bao nhiêu?",
    "orderNo": 1,
    "type": "SINGLE_CHOICE",
    "validAnswer": "B",
    "explanation": "Giá trị chuẩn quốc tế là 299,792,458 m/s.",
    "options": [
      {
        "text": "150,000,000 m/s",
        "isCorrect": false
      },
      {
        "text": "299,792,458 m/s",
        "isCorrect": true
      },
      {
        "text": "3,000,000 m/s",
        "isCorrect": false
      },
      {
        "text": "30,000 km/s",
        "isCorrect": false
      }
    ]
  }'
```

### Bước 11: Test lại API Student

Bây giờ test lại:

```bash
curl -X GET "http://localhost:8080/api/student/exams?page=0&size=10" \
  -H "Authorization: Bearer ${STUDENT_TOKEN}"
```

**Kết quả mong đợi:**

```json
{
  "code": 1000,
  "result": {
    "content": [
      {
        "examId": 1,
        "lessonId": 1,
        "lessonTitle": "Bài 1: Giới thiệu về Toán học",
        "examTitle": "Kiểm tra giữa kỳ - Chương 1",
        "status": "PUBLISHED",
        "submitted": false,
        "bestScore": null,
        "submissionCount": 0
      }
    ],
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

## Checklist để Test Exam APIs

Trước khi test, đảm bảo bạn đã có:

- [ ] ✅ Subject đã tạo
- [ ] ✅ Syllabus đã tạo và link với Subject
- [ ] ✅ Module đã tạo và link với Syllabus
- [ ] ✅ Lesson đã tạo và link với Module (status = PUBLISHED)
- [ ] ✅ Course đã tạo và link với Syllabus
- [ ] ✅ Course có tutorId = TUTOR_USER_ID
- [ ] ✅ TutorClass đã tạo và link với Course
- [ ] ✅ **ClassEnrollment đã tạo** (Student đã được mời vào class)
- [ ] ✅ Exam đã tạo và link với Lesson (status = PUBLISHED)
- [ ] ✅ Quiz đã tạo và link với Exam

**Lưu ý quan trọng:**

- Nếu thiếu bất kỳ bước nào, API sẽ trả về kết quả rỗng
- Đặc biệt chú ý: **ClassEnrollment** phải được tạo (bước 8)
- Exam phải có **status = PUBLISHED** (không phải DRAFT)
