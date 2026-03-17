---
applyTo: "**"
---

# Hướng dẫn Review Code cho GitHub Copilot

## Ngôn ngữ giao tiếp

- **Luôn sử dụng tiếng Việt** khi review code, giải thích, và đưa ra gợi ý
- Tất cả comments, suggestions, và feedback phải bằng tiếng Việt
- Khi được yêu cầu review Pull Request, hãy review toàn bộ code changes và đưa ra feedback chi tiết

## Quy tắc Review Code

### 1. Code Quality & Standards

- Kiểm tra code có tuân thủ coding standards của dự án không
- Đảm bảo code dễ đọc, dễ maintain, và có cấu trúc rõ ràng
- Naming conventions:
  - camelCase cho biến và phương thức
  - PascalCase cho class và interface
  - UPPER_SNAKE_CASE cho constants
  - Tên phải có ý nghĩa, tránh viết tắt không rõ ràng
- Đảm bảo không có code duplicate không cần thiết (DRY principle)
- Kiểm tra code có follow SOLID principles không

### 2. Security

- **Quan trọng**: Kiểm tra kỹ các vấn đề bảo mật
- SQL injection: Đảm bảo sử dụng parameterized queries hoặc JPA methods
- XSS vulnerabilities: Validate và sanitize user input
- Authentication & Authorization: Kiểm tra đúng user có quyền thực hiện action không
- Sensitive data: Đảm bảo passwords, API keys, tokens không bị expose trong code/logs
- Input validation: Validate tất cả input từ user
- File upload: Kiểm tra file type, size limits, và sanitize file names

### 3. Performance

- Kiểm tra N+1 query problems trong JPA/Hibernate
- Đảm bảo có pagination cho list queries (không load toàn bộ data)
- Kiểm tra memory leaks, resource leaks (đóng connections, streams)
- Đánh giá độ phức tạp của algorithms (tránh O(n²) không cần thiết)
- Kiểm tra có sử dụng caching hợp lý không
- Lazy loading vs Eager loading trong JPA

### 4. Error Handling

- Đảm bảo có proper exception handling (không để exception bị nuốt)
- Kiểm tra error messages có meaningful và helpful không
- Đảm bảo không có silent failures
- Sử dụng custom exceptions khi cần thiết (AppException, ErrorCode)
- Logging: Đảm bảo có log errors đầy đủ nhưng không log sensitive data

### 5. Testing

- Kiểm tra có unit tests cho logic quan trọng không
- Đảm bảo test coverage đầy đủ cho business logic
- Kiểm tra test cases có cover edge cases không
- Đảm bảo tests có thể chạy độc lập (không phụ thuộc vào nhau)
- Mock external dependencies đúng cách

### 6. Spring Boot Best Practices

- Dependency injection: Sử dụng constructor injection (không dùng @Autowired trên field)
- @Transactional: Đảm bảo được sử dụng đúng scope và isolation level
- Kiểm tra circular dependencies
- Configuration: Đảm bảo được externalize (không hardcode)
- Bean scope: Sử dụng đúng scope (singleton, prototype, request, session)

### 7. Database & JPA

- Database queries: Kiểm tra có optimized không (indexes, query plans)
- Đảm bảo có proper indexes cho foreign keys và columns thường query
- Transaction boundaries: Đảm bảo transaction được quản lý đúng
- Constraints: Đảm bảo có proper database constraints và validations
- Entity relationships: Kiểm tra @OneToMany, @ManyToOne, @ManyToMany được map đúng
- Cascade operations: Đảm bảo cascade được set đúng (tránh xóa nhầm)

### 8. API Design (REST)

- RESTful conventions: Đảm bảo follow REST principles
- HTTP status codes: Sử dụng đúng status codes (200, 201, 400, 401, 403, 404, 500)
- Request/Response DTOs: Tách biệt DTOs, không expose entities trực tiếp
- Validation: Sử dụng @Valid, @NotNull, @Size, etc. cho request DTOs
- API versioning: Nếu có breaking changes, cần versioning
- Documentation: Đảm bảo có Swagger/OpenAPI documentation

### 9. Documentation

- JavaDoc: Có JavaDoc cho public methods và complex logic
- Code comments: Comments rõ ràng khi code phức tạp (nhưng code nên tự giải thích)
- README: Update README nếu có thay đổi lớn về setup/configuration
- API docs: Đảm bảo Swagger docs được update

### 10. Git & Commit Messages

- Commit messages: Phải theo format Conventional Commits
  - Format: `type: description`
  - Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `perf`
  - Description bằng tiếng Anh, ngắn gọn, rõ ràng
  - Ví dụ: `feat: add user authentication endpoint`
- Không commit sensitive data (passwords, keys, tokens)
- .gitignore: Đảm bảo được cấu hình đúng

### 11. Dependencies & Libraries

- Kiểm tra dependencies có cần thiết không (tránh bloat)
- Đảm bảo versions được update và không có security vulnerabilities
- Kiểm tra có conflicting dependencies không

### 12. Internationalization (i18n)

- Đảm bảo messages được externalize (không hardcode strings)
- Sử dụng message properties files
- Kiểm tra có support đa ngôn ngữ không

## Format Review khi Review PR

Khi review Pull Request, luôn theo format sau:

```
## 📋 Tổng quan
[Đánh giá tổng quan về PR - tóm tắt những gì đã thay đổi]

## ✅ Điểm tốt
- [Điểm tích cực 1]
- [Điểm tích cực 2]

## ⚠️ Cần cải thiện (Suggestions)
- **[File/Class]**: [Vấn đề cụ thể]
  - **Vấn đề**: [Mô tả vấn đề]
  - **Gợi ý**: [Cách sửa cụ thể]
  - **Lý do**: [Tại sao cần sửa]

## 🔒 Security Concerns
- [Nếu có vấn đề bảo mật - đây là BLOCKER]

## 🚀 Performance Issues
- [Nếu có vấn đề về performance]

## 🐛 Potential Bugs
- [Nếu phát hiện bugs tiềm ẩn]

## 📝 Gợi ý cải thiện (Nice to have)
- [Các gợi ý cải thiện không critical]

## ✅ Checklist
- [ ] Code đã được test
- [ ] Không có security vulnerabilities
- [ ] Performance được tối ưu
- [ ] Documentation đã được update
- [ ] Không có breaking changes (hoặc đã được document)
```

## Mức độ ưu tiên khi Review

1. **CRITICAL (Phải sửa ngay)**:

   - Security vulnerabilities
   - Data loss risks
   - Breaking changes không được document
   - Code không compile hoặc tests fail

2. **HIGH (Nên sửa)**:

   - Performance issues nghiêm trọng
   - Potential bugs
   - Code quality issues lớn
   - Missing error handling

3. **MEDIUM (Nên cân nhắc)**:

   - Code style inconsistencies
   - Missing tests
   - Documentation thiếu

4. **LOW (Nice to have)**:
   - Code improvements nhỏ
   - Refactoring suggestions
   - Style improvements

## Lưu ý đặc biệt

- **Luôn constructive và professional** trong feedback
- **Đưa ra giải pháp cụ thể**, không chỉ chỉ ra vấn đề
- **Ưu tiên các vấn đề về security và performance**
- **Tôn trọng coding style hiện tại** của dự án
- **Đề xuất improvements** nhưng không quá strict nếu code đã hoạt động tốt
- **Khen ngợi code tốt** để khuyến khích best practices
- **Giải thích rõ ràng** tại sao cần sửa (giúp developer học hỏi)

## Context về dự án

- Dự án sử dụng **Spring Boot 3.4.9** với **Java 21**
- Database: **MySQL** với **JPA/Hibernate**
- Framework: **Spring Data JPA**, **Spring Security**, **Spring Web**
- Testing: **JUnit 5**, **Mockito**, **Testcontainers**
- Build tool: **Maven**
- Code style: Follow Spring Boot conventions và Java best practices

## Code Conventions hiện tại của dự án

### 1. Naming Conventions

- **Classes/Interfaces**: PascalCase
  - Ví dụ: `TutorProfileService`, `VideoLessonController`, `TutorClassMapper`
- **Methods/Variables**: camelCase
  - Ví dụ: `getTutorProfile()`, `tutorProfileRepository`, `saveTutorProfile()`
- **Constants**: UPPER_SNAKE_CASE trong class constants
  - Ví dụ: `ADMIN_ROLE`, `TUTOR_ROLE` trong `PredefinedRole`
  - Constants được nhóm trong static nested classes: `SearchConstants.Tutor.ID`
- **Packages**: lowercase với dots
  - Ví dụ: `com.sep.educonnect.service`, `com.sep.educonnect.dto.tutor.request`
- **DTOs**:
  - Request DTOs: `*Request` (ví dụ: `SubmitProfileRequest`, `VideoLessonRequest`)
  - Response DTOs: `*Response` (ví dụ: `TutorStudentResponse`, `VideoLessonResponse`)

### 2. Lombok Usage (Bắt buộc)

- **Service/Controller/Repository**:
  - `@RequiredArgsConstructor` cho constructor injection
  - `@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)` cho final fields
  - `@Slf4j` cho logging
- **Entity**:
  - `@Getter`, `@Setter`, `@SuperBuilder`, `@NoArgsConstructor`, `@AllArgsConstructor`
  - `@FieldDefaults(level = AccessLevel.PRIVATE)`
- **DTO**:
  - `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
  - `@FieldDefaults(level = AccessLevel.PRIVATE)`
- **Không dùng** `@Autowired` trên field - chỉ dùng constructor injection

### 3. Dependency Injection Pattern

```java
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TutorProfileService {
    final TutorProfileRepository tutorProfileRepository;
    final UserRepository userRepository;
    // ... other dependencies
}
```

### 4. Entity Pattern

- Tất cả entities **extends BaseEntity** (có auditing fields: createdBy, createdAt, modifiedBy, modifiedAt, isDeleted)
- Sử dụng `@SuperBuilder` thay vì `@Builder` khi extends BaseEntity
- JPA annotations:
  - `@Entity`, `@Table(name = "table_name")`
  - `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`
  - Relationships: `@ManyToOne`, `@OneToMany`, `@ManyToMany`, `@OneToOne`
  - `@JsonIgnore` cho sensitive fields hoặc relationships không cần expose
  - `@Enumerated(EnumType.STRING)` cho enums

### 5. Controller Pattern

- `@RestController` với `@RequestMapping("/api/...")`
- Tất cả endpoints trả về `ApiResponse<T>` wrapper
- Sử dụng `@RequiredArgsConstructor` thay vì `@Autowired`
- Method naming: động từ (create, get, update, delete, list)
- Pagination: `@RequestParam(defaultValue = "0") int page`, `@RequestParam(defaultValue = "10") int size`

### 6. Service Pattern

- `@Service` với `@Transactional` (class level)
- `@RequiredArgsConstructor` cho DI
- Methods trả về entities hoặc DTOs
- Sử dụng `SecurityContextHolder.getContext()` để lấy current user
- Exception handling: `orElseThrow(() -> new AppException(ErrorCode.XXX))`

### 7. Repository Pattern

- Extends `JpaRepository<Entity, ID>`
- `@Repository` annotation
- Custom queries với `@Query` annotation
- Return `Optional<T>` cho find methods
- Method naming: `findBy...`, `existsBy...`, `countBy...`

### 8. Exception Handling

- Custom exception: `AppException extends RuntimeException`
- Error codes trong enum `ErrorCode` với message keys
- Global exception handler: `GlobalExceptionHandler` với `@ControllerAdvice`
- Tất cả exceptions được map về `ApiResponse` format
- Sử dụng i18n service cho error messages

### 9. DTO Pattern

- Request DTOs trong package `dto.*.request`
- Response DTOs trong package `dto.*.response`
- Common wrapper: `ApiResponse<T>` với fields: `code`, `message`, `result`
- Default code: `1000` cho success
- `@JsonInclude(JsonInclude.Include.NON_NULL)` để loại bỏ null fields

### 10. Mapper Pattern (MapStruct)

- Interface với `@Mapper(componentModel = "spring")`
- Methods: `toResponse()`, `toEntity()`, `toResponseList()`
- Sử dụng `@Context` cho localization helper
- Mapping annotations: `@Mapping(target = "...", source = "...")`

### 11. Constants Pattern

- Constants được nhóm trong classes với private constructor
- Ví dụ: `PredefinedRole`, `SearchConstants`, `TemplateMail`, `Language`
- Static nested classes để nhóm constants: `SearchConstants.Tutor`, `SearchConstants.Index`

### 12. Testing Pattern

- JUnit 5 với `@ExtendWith(MockitoExtension.class)`
- `@DisplayName` cho test descriptions
- `@Mock` cho dependencies, `@InjectMocks` cho class under test
- Test method naming: `should_expectedBehavior_when_stateUnderTest()`
- Given-When-Then pattern trong test methods
- `@BeforeEach` và `@AfterEach` cho setup/teardown
- Sử dụng `MockHelper` utility cho security context mocking

### 13. Logging Pattern

- Sử dụng `@Slf4j` Lombok annotation
- Log levels:
  - `log.info()` cho business events
  - `log.warn()` cho warnings
  - `log.error()` cho errors (luôn kèm exception)
- Format: `log.info("Message with {} and {}", param1, param2)`

### 14. API Response Format

```java
ApiResponse.<T>builder()
    .code(1000)  // default
    .message("OK")  // optional
    .result(data)
    .build()
```

### 15. Security Context Pattern

```java
var context = SecurityContextHolder.getContext();
String name = context.getAuthentication().getName();
User user = userRepository.findByUsername(name)
    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
```

### 16. Validation Pattern

- Sử dụng Jakarta Validation: `@Valid`, `@NotNull`, `@Size`, etc.
- Validation trong DTOs (request objects)
- Error messages qua i18n service

### 17. Transaction Management

- `@Transactional` ở class level cho services
- Sử dụng `jakarta.transaction.Transactional` (không phải Spring)
- Read-only transactions khi chỉ đọc: `@Transactional(readOnly = true)`

### 18. Internationalization (i18n)

- Messages trong `messages.properties` và `messages_en.properties`
- Sử dụng `I18nService` để lấy messages
- Error codes có message keys: `ErrorCode.USER_NOT_EXISTED.getMessageKey()`

### 19. Query Pattern

- Prefer Spring Data JPA methods: `findBy...`, `existsBy...`
- Custom queries với `@Query` khi cần
- Sử dụng `@Param` cho named parameters
- JOIN FETCH để tránh N+1: `LEFT JOIN FETCH t.tags`

### 20. Builder Pattern

- Sử dụng Lombok `@Builder` hoặc `@SuperBuilder`
- Entity creation: `Entity.builder().field(value).build()`
- DTO creation: `Dto.builder().field(value).build()`
