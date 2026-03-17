# Tutor Reports APIs

This document describes two report-related endpoints exposed in `TutorController`:

1. `GET /api/tutor/teacher-report/class/{classId}/exams` — Teacher view of exams for a class (includes per-exam statistics and student details)
2. `GET /api/tutor/student-report/class/{classId}/student/{studentId}/exams` — Student-specific exam list within a class

Both endpoints return an `ApiResponse<T>` wrapper:
```json
{
  "code": 1000,
  "message": null,
  "result": {}
}
```

Base URL: `https://api.educonnect.dev`

Authentication and Authorization
- Both endpoints are in `TutorController` which is annotated with `@PreAuthorize("hasRole('TUTOR')")`, so the user must be authenticated and have the TUTOR role.
- Requests must include: `Authorization: Bearer <JWT>` header.

Common query params for pagination
- `page` (default 0)
- `size` (default 10)

---

## 1) Teacher report — Class exams

- Method: GET
- URL: `/api/tutor/teacher-report/class/{classId}/exams`
- Auth: REQUIRED (role: TUTOR)
- Purpose: Return a paginated list of exams related to the class's syllabus with per-exam statistics and per-student details (including attendance info).

Path parameters
- `classId` (Long) — ID of the class to fetch reports for

Response type
- `ApiResponse<Page<TeacherClassExamResponse>>` where `TeacherClassExamResponse` contains:
  - examId (Long)
  - lessonId (Long)
  - lessonTitle (String)
  - examTitle (String)
  - status (String) — exam status (e.g., PUBLISHED)
  - totalStudents (Integer)
  - submittedCount (Integer)
  - notSubmittedCount (Integer)
  - averageScore (Double | null) — average of best scores among students who submitted
  - attendanceSummary (AttendanceSummary)
    - totalSessions (Integer)
    - averagePresent (Integer)
    - averageAbsent (Integer)
  - studentDetails (List<StudentExamDetail>) — detailed per-student info
    - studentId (String)
    - studentName (String)
    - studentEmail (String)
    - submitted (Boolean)
    - bestScore (Double | null)
    - submissionCount (Integer)
    - attendanceInfo (AttendanceInfo)
      - totalSessions
      - presentCount
      - absentCount
      - attendanceRate (Double)

Behavior notes (service-side)
- The service validates the class exists; if not, it throws `AppException(ErrorCode.CLASS_NOT_FOUND)`.
- If no enrollments, syllabus, lessons, or exams found, it returns an empty Page result.
- The exam list is filtered to PUBLISHED exams for lessons in the class syllabus.
- Student details are aggregated from enrollments, exam submissions, and session attendances.
- Pagination is applied in-memory after mapping (page and size are used to slice the result list).

cURL example

curl -X GET "https://api.educonnect.dev/api/tutor/teacher-report/class/123/exams?page=0&size=10" \
  -H "Authorization: Bearer <JWT>"

Success example (truncated result)

```json
{
  "code": 1000,
  "message": null,
  "result": {
    "content": [
      {
        "examId": 456,
        "lessonId": 789,
        "lessonTitle": "Lesson 1: Basics",
        "examTitle": "Midterm Quiz",
        "status": "PUBLISHED",
        "totalStudents": 20,
        "submittedCount": 18,
        "notSubmittedCount": 2,
        "averageScore": 85.5,
        "attendanceSummary": {
          "totalSessions": 12,
          "averagePresent": 10,
          "averageAbsent": 2
        },
        "studentDetails": [
          {
            "studentId": "stu-1",
            "studentName": "Alice Nguyen",
            "studentEmail": "alice@example.com",
            "submitted": true,
            "bestScore": 90.0,
            "submissionCount": 1,
            "attendanceInfo": {
              "totalSessions": 12,
              "presentCount": 12,
              "absentCount": 0,
              "attendanceRate": 100.0
            }
          }
        ]
      }
    ],
    "pageable": { },
    "totalElements": 3,
    "totalPages": 1,
    "last": true,
    "size": 10,
    "number": 0,
    "sort": { },
    "numberOfElements": 3,
    "first": true
  }
}
```

Common errors
- 404 CLASS_NOT_FOUND (ErrorCode.CLASS_NOT_FOUND) — class id not found
- 401/403 — authentication/authorization errors

---

## 2) Student report — Exams for a student in a class

- Method: GET
- URL: `/api/tutor/student-report/class/{classId}/student/{studentId}/exams`
- Auth: REQUIRED (role: TUTOR)
- Purpose: Return a paginated list of exams for a given student within the specified class. This is intended for tutors to view a specific student's exam records in that class.

Path parameters
- `classId` (Long) — ID of the class
- `studentId` (String) — userId of the student

Response type
- `ApiResponse<Page<StudentExamListItemResponse>>` where `StudentExamListItemResponse` contains:
  - examId (Long)
  - lessonId (Long)
  - lessonTitle (String)
  - examTitle (String)
  - status (String)
  - submitted (Boolean)
  - bestScore (Double | null)
  - submissionCount (Integer)

Behavior notes (service-side)
- The service collects all PUBLISHED exams for lessons in the class syllabus.
- For each exam it queries the submissions by the given student to populate `submitted`, `bestScore`, and `submissionCount`.
- Pagination is applied in-memory after mapping to responses.
- If the class or syllabus/lessons have no exams, an empty Page is returned.

cURL example

curl -X GET "https://api.educonnect.dev/api/tutor/student-report/class/123/student/stu-1/exams?page=0&size=10" \
  -H "Authorization: Bearer <JWT>"

Success example (truncated)

```json
{
  "code": 1000,
  "message": null,
  "result": {
    "content": [
      {
        "examId": 456,
        "lessonId": 789,
        "lessonTitle": "Lesson 1: Basics",
        "examTitle": "Midterm Quiz",
        "status": "PUBLISHED",
        "submitted": true,
        "bestScore": 90.0,
        "submissionCount": 1
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 10,
    "number": 0
  }
}
```

Common errors
- 401/403 — authentication/authorization errors

---

## Integration tips
- Include `Authorization: Bearer <JWT>` header for both endpoints.
- Expect `Page<T>` structure inside `ApiResponse.result` for pagination metadata; clients can use `totalElements` and `totalPages` for UI pagination.
- The service calculates attendance and submission stats by querying enrollments, session attendances, and exam submissions; these can be expensive for large classes. Consider caching or limiting page sizes if necessary.

---

If you'd like, I can:
- Add example JWTs to cURL snippets
- Generate an OpenAPI snippet for these two endpoints
- Add an example client code (JS/TS) to call these endpoints
