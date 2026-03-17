# EduConnect

EduConnect leverages Spring Boot to provide a comprehensive learning ecosystem. By prioritizing efficient matching tutor algorithms, the platform offers users adaptive tracking, secure transactions, and high-quality video-on-demand lessons for an integrated educational experience.
## Key Features

- **Smart Tutor Search**: OpenSearch + rerank model support for relevance-tuned tutor discovery.
- **Course & Lesson Management**: Subjects, lessons, quizzes/exams, invitations, and attendance flows.
- **Progress Tracking**: Course/Lesson progress states with auto-updates when learners watch videos or finish quizzes.
- **Video on Demand (HLS)**: S3 direct upload, Lambda-based FFmpeg transcode, CloudFront signed streaming.
- **Scheduling & Live**: Zoom integration for online sessions and booking workflows.
- **Notifications**: WebSocket push with typed links and updatable invites.
- **Payments**: PayOS checkout with return/cancel URLs.
- **Email & Files**: Mailgun templated emails, AWS S3 storage.
- **AI Assistant**: OpenAI ChatGPT integration for user support.
- **Security & Reliability**: JWT/OAuth2 auth, rate limiting (Bucket4j), input validation, i18n (VI/EN).
- **Multi-language**: i18n support for Vietnamese and English
- End-to-end VOD pipeline (S3 presigned upload → Lambda FFmpeg → CloudFront signed HLS streaming).
- Tutor exam result APIs split into statistics, attempt list, and attempt detail for better performance.
- Progress tracking APIs documented with auto-updates when streaming lessons.
- Notification patterns with action links and updatable invitations.
- PayOS configuration for payments and callback URLs.
- Recommendation API hook for tutor/course suggestions.

## Technology Stack

- **Backend**: Spring Boot 3.4.9
- **Java**: 21
- **Database**: MySQL
- **Search Engine**: OpenSearch (+ rerank model)
- **Authentication**: JWT + OAuth2
- **File Storage**: AWS S3
- **Email Service**: Mailgun
- **AI**: OpenAI ChatGPT
- **Video**: AWS Lambda (FFmpeg) + S3 + CloudFront (signed URLs)
- **Payments**: PayOS
- **Video Conference**: Zoom API
- **Rate Limiting**: Bucket4j
- **Logging**: Logback with JSON format
- **Documentation**: Swagger/OpenAPI

## System Requirements

- Java 21+
- Maven 3.6+
- MySQL 8.0+
- OpenSearch 2.x
- Docker (optional)

## Installation and Setup

### 1. Clone repository

```bash
git clone <repository-url>
cd educonnect
```

### 2. Database setup

Create MySQL database:

```sql
CREATE DATABASE educonnect;
```

### 3. Environment configuration

Create `.env` file from `.env.example` and configure variables:

```bash
# Database
DB_HOST=localhost
DB_PORT=3306
DB_NAME=educonnect
DB_USERNAME=your_username
DB_PASSWORD=your_password

# OpenSearch
OPENSEARCH_HOST=http://localhost:9200
OPENSEARCH_USER=admin
OPENSEARCH_PASSWORD=admin
OPENSEARCH_MODEL=your_rerank_model # optional

# JWT
JWT_SIGNER_KEY=your_jwt_secret_key
JWT_VALID_DURATION=3600
JWT_REFRESHABLE_DURATION=36000

# AWS S3
S3_ACCESS_KEY=your_s3_access_key
S3_SECRET_KEY=your_s3_secret_key
CDN_DOMAIN=your_cloudfront_domain   # e.g. dxxx.cloudfront.net
KEY_PAIR_ID=your_cloudfront_key_id  # for signed URLs

# Mailgun
MAILGUN_DOMAIN=your_mailgun_domain
MAILGUN_FROM=noreply@yourdomain.com
MAILGUN_NAME=EduConnect
MAILGUN_API_KEY=your_mailgun_api_key

# OpenAI
OPENAI_API_KEY=your_openai_api_key

# Zoom
ZOOM_ACCOUNT_ID=your_zoom_account_id
ZOOM_CLIENT_ID=your_zoom_client_id
ZOOM_CLIENT_SECRET=your_zoom_client_secret

# Payments (PayOS)
PAYOS_CLIENT_ID=your_payos_client_id
PAYOS_CLIENT_SECRET=your_payos_client_secret
PAYOS_CHECKSUM_KEY=your_payos_checksum_key
PAYOS_RETURN_URL=https://your-frontend.com/payments/success
PAYOS_CANCEL_URL=https://your-frontend.com/payments/cancel

# Video pipeline
VIDEO_LAMBDA_FUNCTION_NAME=educonnect-video-transcoder
VIDEO_LAMBDA_REGION=ap-southeast-1
CALLBACK_API_URL=https://api.educonnect.dev/api/video-lessons/callback

# Recommendation
RECOMMENDATION_API_URL=https://recommendation.educonnect.dev

# Frontend base URL (used in links)
FRONTEND_URL=https://educonnect.dev

# Logging
LOG_PATH=./logs

# Spring Profile
SPRING_PROFILES_ACTIVE=dev
```

### 4. Run the application

#### Using Maven:

```bash
# Compile and run
mvn spring-boot:run

# Or build JAR file
mvn clean package
java -jar target/educonnect-0.0.1-SNAPSHOT.jar
```

#### Using Docker:

```bash
# Build Docker image
docker build -t educonnect .

# Run container
docker run -p 8080:8080 --env-file .env educonnect
```

### 5. Access the application

- **API**: https://api.educonnect.dev
- **Swagger UI**: https://api.educonnect.dev/swagger-ui/index.html
- **API Documentation**: https://api.educonnect.dev/v3/api-docs

## 📁 Project Structure

```
src/main/java/com/sep/educonnect/
├── configuration/     # Spring configuration
├── constant/          # Constants
├── controller/        # REST Controllers
├── dto/              # Data Transfer Objects
├── entity/           # JPA Entities
├── enums/            # Enumerations
├── exception/        # Exception handling
├── helper/           # Utility helpers
├── mapper/           # MapStruct mappers
├── repository/       # JPA Repositories
├── service/          # Business logic
├── utils/            # Utility classes
├── validator/        # Custom validators
└── wrapper/          # Response wrappers
lambda/               # FFmpeg Lambda for VOD transcoding
docs/                 # Feature-specific guides (VOD, exams, progress, notifications, etc.)
```

## 🔧 Configuration

### Database

The application uses MySQL as the primary database. Configuration in `application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
```

### OpenSearch

OpenSearch configuration for search functionality:

```yaml
spring:
  opensearch:
    uris: ${OPENSEARCH_HOST}
    username: ${OPENSEARCH_USER}
    password: ${OPENSEARCH_PASSWORD}
```

### Logging

The logging system uses Logback with JSON format:

- **System logs**: `./logs/system.log`
- **Search logs**: `./logs/search.log`
- **Timezone**: Asia/Ho_Chi_Minh (UTC+7)

## Notifications

- WebSocket push via `NotificationService`, with typed links (`NotificationLink`) and updatable invites.
- Common patterns: booking creation, status changes, booking invite updates, attendance, schedule changes.
- Refer to `docs/notification-backend-integration.md` for ready-to-use snippets.

## Payments

- PayOS integration via client id/secret + checksum; configure return/cancel URLs in `.env`.

## Recommendation

- External recommendation service configured via `RECOMMENDATION_API_URL` for tutor/course suggestions.

## Testing

```bash
# Run all tests
mvn test
```

## Monitoring and Logging

### Log Files

- **System logs**: Records all system activities
- **Search logs**: Records search queries with metadata

### Metrics

- Rate limiting metrics
- API response times
- Database connection pool
- OpenSearch query performance

## Security

- **Authentication**: JWT tokens
- **Authorization**: Role-based access control
- **Rate Limiting**: API protection against abuse
- **Input Validation**: Validation for all inputs
- **CORS**: CORS configuration for frontend

## API Endpoints

Base URL: `https://api.educonnect.dev`

### Authentication

- `POST /api/auth/login` - Login
- `POST /api/auth/register` - Register
- `POST /api/auth/refresh` - Refresh token

##  Deployment

### Docker Deployment

```bash
# Build image
docker build -t educonnect:latest .

# Run with environment variables
docker run -d \
  --name educonnect \
  -p 8080:8080 \
  --env-file .env \
  educonnect:latest
```

### Production Configuration

1. Configure production database
2. Setup OpenSearch cluster
3. Configure AWS S3 credentials
4. Setup Mailgun domain
5. Configure Zoom API credentials
6. Setup OpenAI API key

## Acknowledgments

- Spring Boot team
- OpenSearch community
- AWS S3 service
- Mailgun service
- OpenAI API
- Zoom API
