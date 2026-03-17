# Tutor Exam APIs - Thống kê và Quản lý Kết quả

Tài liệu này mô tả các API mới được tách ra từ API `/api/tutor/exams/{examId}/results` để cải thiện hiệu năng và dễ sử dụng hơn.

## 1. Thống kê Tổng hợp Exam

**Endpoint:** `GET /api/tutor/exams/{examId}/statistics`

**Mô tả:** Lấy thông tin thống kê tổng hợp của exam, bao gồm thống kê theo từng student (số lần thử, điểm cao nhất, điểm trung bình, v.v.)

**Request:**
```bash
curl -X GET http://localhost:8080/api/tutor/exams/1/statistics \
  -H "Authorization: Bearer ${TUTOR_TOKEN}"
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "examId": 1,
    "examTitle": "Math Final Exam",
    "lessonTitle": "Advanced Mathematics",
    "totalSubmissions": 15,
    "totalStudents": 5,
    "studentStatistics": [
      {
        "studentId": "student-1",
        "studentName": "Nguyễn Văn A",
        "attemptCount": 3,
        "bestScore": 95.0,
        "averageScore": 88.5,
        "firstAttemptAt": "2024-01-10T10:00:00",
        "lastAttemptAt": "2024-01-15T14:30:00"
      },
      {
        "studentId": "student-2",
        "studentName": "Trần Thị B",
        "attemptCount": 2,
        "bestScore": 87.5,
        "averageScore": 82.0,
        "firstAttemptAt": "2024-01-12T09:00:00",
        "lastAttemptAt": "2024-01-14T16:20:00"
      }
    ]
  }
}
```

**Lưu ý:** API này chỉ trả về thông tin tổng hợp, không bao gồm chi tiết từng lần làm bài.

---

## 2. Danh sách các Lần làm bài

**Endpoint:** `GET /api/tutor/exams/{examId}/attempts`

**Mô tả:** Lấy danh sách tất cả các lần làm bài của exam. Có thể filter theo studentId.

**Query Parameters:**
- `studentId` (optional): Filter theo studentId cụ thể

**Request:**
```bash
# Lấy tất cả các lần làm bài
curl -X GET http://localhost:8080/api/tutor/exams/1/attempts \
  -H "Authorization: Bearer ${TUTOR_TOKEN}"

# Lấy các lần làm bài của một student cụ thể
curl -X GET "http://localhost:8080/api/tutor/exams/1/attempts?studentId=student-1" \
  -H "Authorization: Bearer ${TUTOR_TOKEN}"
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "examId": 1,
    "examTitle": "Math Final Exam",
    "lessonTitle": "Advanced Mathematics",
    "totalAttempts": 15,
    "attempts": [
      {
        "submissionId": 100,
        "studentId": "student-1",
        "studentName": "Nguyễn Văn A",
        "score": 95.0,
        "totalQuestions": 20,
        "correctAnswers": 19,
        "submittedAt": "2024-01-15T14:30:00",
        "durationSeconds": 1800
      },
      {
        "submissionId": 99,
        "studentId": "student-1",
        "studentName": "Nguyễn Văn A",
        "score": 88.0,
        "totalQuestions": 20,
        "correctAnswers": 17,
        "submittedAt": "2024-01-12T11:20:00",
        "durationSeconds": 2100
      }
    ]
  }
}
```

**Lưu ý:** 
- Danh sách được sắp xếp theo thời gian submit giảm dần (mới nhất trước)
- API này không bao gồm chi tiết answers, chỉ có thông tin tóm tắt

---

## 3. Chi tiết một Lần làm bài

**Endpoint:** `GET /api/tutor/exams/{examId}/attempts/{submissionId}`

**Mô tả:** Lấy chi tiết đầy đủ của một lần làm bài cụ thể, bao gồm tất cả câu trả lời và explanation.

**Request:**
```bash
curl -X GET http://localhost:8080/api/tutor/exams/1/attempts/100 \
  -H "Authorization: Bearer ${TUTOR_TOKEN}"
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "submissionId": 100,
    "studentId": "student-1",
    "studentName": "Nguyễn Văn A",
    "score": 95.0,
    "totalQuestions": 20,
    "correctAnswers": 19,
    "submittedAt": "2024-01-15T14:30:00",
    "durationSeconds": 1800,
    "answers": [
      {
        "quizId": 1,
        "studentAnswer": "A",
        "correctAnswer": "A",
        "isCorrect": true,
        "explanation": "Đây là đáp án đúng vì..."
      },
      {
        "quizId": 2,
        "studentAnswer": "B,C",
        "correctAnswer": "A,B,C",
        "isCorrect": false,
        "explanation": "Câu trả lời đúng phải bao gồm cả A, B và C..."
      }
    ]
  }
}
```

**Lưu ý:** API này trả về đầy đủ thông tin chi tiết, bao gồm tất cả answers với explanation.

---

## So sánh với API cũ

API cũ `/api/tutor/exams/{examId}/results` trả về tất cả thông tin trong một response, gây ra:
- Response quá lớn và chậm
- Khó sử dụng khi chỉ cần thông tin tổng hợp
- Phải load tất cả chi tiết ngay cả khi không cần

Các API mới được tách ra giúp:
- ✅ Tải nhanh hơn: chỉ load dữ liệu cần thiết
- ✅ Dễ sử dụng: API rõ ràng theo mục đích
- ✅ Linh hoạt: có thể filter theo studentId
- ✅ Hiệu năng tốt hơn: giảm tải cho server

---

## Workflow đề xuất

1. **Xem tổng quan:** Sử dụng `/statistics` để xem thống kê tổng hợp
2. **Xem danh sách:** Sử dụng `/attempts` để xem danh sách các lần làm bài (có thể filter theo student)
3. **Xem chi tiết:** Sử dụng `/attempts/{submissionId}` khi cần xem chi tiết một lần làm bài cụ thể

