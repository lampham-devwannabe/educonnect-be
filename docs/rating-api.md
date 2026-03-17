# Tutor Rating API

This document summarizes all API endpoints related to tutor ratings (create, update, delete, list, and summary). Each endpoint description includes required authentication, request/response shapes, common error codes, and a cURL example.

Base path: `https://api.educonnect.dev/api/ratings`

Authentication
- Endpoints that modify or read the current student's ratings require a valid JWT in the `Authorization: Bearer <token>` header.
- Endpoints that fetch public data (tutor ratings, summary) are public and do not require authentication.

Response wrapper
All responses use the `ApiResponse<T>` wrapper with the shape:
- code (int) — business code (default 1000 for success)
- message (string) — localized message (may be null)
- result (T) — payload (may be null)

Example:
{
  "code": 1000,
  "message": null,
  "result": { ... }
}

---

## DTO summaries

CreateTutorRatingRequest
- tutorId: String (required)
- rating: Integer (required, 1..5)
- content: String (optional, max 2000)

UpdateTutorRatingRequest
- rating: Integer (optional, 1..5)
- content: String (optional, max 2000)

TutorRatingDTO (response)
- ratingId: Long
- tutorId, tutorName, tutorAvatar
- studentId, studentName, studentAvatar
- rating: Integer
- content: String
- createdAt, modifiedAt
- createdBy, modifiedBy

TutorRatingSummaryDTO
- tutorId, tutorName
- averageRating (Double), totalRatings
- count and percentage for 5..1 stars
- recentRatings: List<TutorRatingDTO> (top 5)

---

## Endpoints

### 1) Create rating
- Method: POST
- URL: `/api/ratings`
- Auth: REQUIRED (role STUDENT)
- Request body: `CreateTutorRatingRequest` JSON

cURL example:

curl -X POST "https://api.educonnect.dev/api/ratings" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT>" \
  -d '{
    "tutorId": "tutor-123",
    "rating": 5,
    "content": "Great explanations and very patient."
  }'

Success response (ApiResponse<TutorRatingDTO>):
{
  "code": 1000,
  "message": null,
  "result": {
    "ratingId": 123,
    "tutorId": "tutor-123",
    "tutorName": "John Doe",
    "studentId": "student-456",
    "rating": 5,
    "content": "Great explanations...",
    "createdAt": "2025-11-28T10:00:00",
    "modifiedAt": null
  }
}

Common errors:
- 400 (code 1001) INVALID_KEY — validation failed (e.g., rating outside 1..5)
- 400 (code 3057) ALREADY_RATED — student already rated this tutor
- 404 (code 1005) USER_NOT_EXISTED — tutor or student not found
- 401 / 403 — authentication/authorization errors

---

### 2) Update rating
- Method: PUT
- URL: `/api/ratings/{ratingId}`
- Auth: REQUIRED (role STUDENT, must be owner)
- Request body: `UpdateTutorRatingRequest` JSON (fields optional)

cURL example:

curl -X PUT "https://api.educonnect.dev/api/ratings/123" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT>" \
  -d '{
    "rating": 4,
    "content": "Updated: good but room for improvement"
  }'

Success response (ApiResponse<TutorRatingDTO>) — updated DTO returned.

Common errors:
- 400 (code 1001) INVALID_KEY — validation failed
- 404 (code 3059) RATING_NOT_FOUND — ratingId not found
- 403 (code 1007) UNAUTHORIZED — not the owner

---

### 3) Delete rating
- Method: DELETE
- URL: `/api/ratings/{ratingId}`
- Auth: REQUIRED (role STUDENT, must be owner)

cURL example:

curl -X DELETE "https://api.educonnect.dev/api/ratings/123" \
  -H "Authorization: Bearer <JWT>"

Success response (ApiResponse<Void>):
{
  "code": 1000,
  "message": "Rating deleted successfully",
  "result": null
}

Common errors:
- 404 (3059) RATING_NOT_FOUND
- 403 (1007) UNAUTHORIZED

---

### 4) Get tutor ratings (paginated)
- Method: GET
- URL: `/api/ratings/tutor/{tutorId}`
- Query: `page` (default 0), `size` (default 10)
- Auth: PUBLIC

cURL example:

curl -X GET "https://api.educonnect.dev/api/ratings/tutor/tutor-123?page=0&size=10"

Success response (ApiResponse<Page<TutorRatingDTO>>): `result` is a Page object with content, totalElements, totalPages, etc.

Notes:
- Returns an empty page if no ratings.

---

### 5) Get tutor rating summary
- Method: GET
- URL: `/api/ratings/tutor/{tutorId}/summary`
- Auth: PUBLIC

cURL example:

curl -X GET "https://api.educonnect.dev/api/ratings/tutor/tutor-123/summary"

Success response (ApiResponse<TutorRatingSummaryDTO>):
{
  "code": 1000,
  "message": null,
  "result": {
    "tutorId": "tutor-123",
    "tutorName": "John Doe",
    "averageRating": 4.8,
    "totalRatings": 25,
    "fiveStarCount": 18,
    "fiveStarPercentage": 72.0,
    "recentRatings": [ /* up to 5 items */ ]
  }
}

Common errors:
- 404 (1005) USER_NOT_EXISTED — if tutor not found

---

### 6) Get current student's ratings
- Method: GET
- URL: `/api/ratings/my-ratings`
- Auth: REQUIRED (role STUDENT)

cURL example:

curl -X GET "https://api.educonnect.dev/api/ratings/my-ratings" \
  -H "Authorization: Bearer <JWT>"

Success response (ApiResponse<List<TutorRatingDTO>>): list of ratings created by the authenticated student.

---

### 7) Get current student's rating for a specific tutor
- Method: GET
- URL: `/api/ratings/my-rating/tutor/{tutorId}`
- Auth: REQUIRED (role STUDENT)

cURL example:

curl -X GET "https://api.educonnect.dev/api/ratings/my-rating/tutor/tutor-123" \
  -H "Authorization: Bearer <JWT>"

Success response (ApiResponse<TutorRatingDTO>): returns the rating DTO if exists, otherwise `result` is `null` (student hasn't rated this tutor yet).

---

## Common error codes related to rating flows
- 1001 INVALID_KEY — validation failure (HTTP 400)
- 1005 USER_NOT_EXISTED — user or tutor not found (HTTP 404)
- 1006 UNAUTHENTICATED — not authenticated (HTTP 401)
- 1007 UNAUTHORIZED — no permission (HTTP 403)
- 3057 ALREADY_RATED — student already rated this tutor (HTTP 400)
- 3059 RATING_NOT_FOUND — rating not found (HTTP 404)

All errors are returned in the `ApiResponse` wrapper. The `message` field contains a localized string resolved via i18n service; clients should display that to users.

---

## Integration tips
- Always send `Authorization: Bearer <token>` for endpoints that require authentication.
- Check both HTTP status and `ApiResponse.code` for business errors.
- When displaying summary, prefer `averageRating` and `totalRatings`; use `recentRatings` for sample feedback.
- For create/update operations, validate rating is an integer between 1 and 5 before calling the API to avoid unnecessary round-trips.

---

If you want, I can also add cURL examples that include example JWTs, or generate an OpenAPI spec for these endpoints.
