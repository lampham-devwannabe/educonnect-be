package com.sep.educonnect.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.educonnect.constant.PredefinedRole;
import com.sep.educonnect.entity.TutorAvailability;
import com.sep.educonnect.entity.TutorProfile;
import com.sep.educonnect.entity.User;
import com.sep.educonnect.enums.ProfileStatus;
import com.sep.educonnect.repository.*;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DebeziumSourceEventListener {
    final Executor executor;
    final DebeziumEngine<ChangeEvent<String, String>> debeziumEngine;
    final TutorProfileRepository tutorProfileRepository;
    final TutorAvailabilityRepository tutorAvailabilityRepository;
    final TutorClassRepository tutorClassRepository;
    final TutorSearchProjectionUtil projectionService;
    final StudentSearchProjectionUtil studentProjectionService;
    final UserRepository userRepository;
    final OpenSearchClient openSearchClient;

    @Value("${spring.opensearch.pipeline:tutor-cdc-normalize-and-embed}")
    String pipelineName;

    public DebeziumSourceEventListener(
            io.debezium.config.Configuration mysqlConnector,
            TutorProfileRepository tutorProfileRepository,
            TutorAvailabilityRepository tutorAvailabilityRepository,
            TutorClassRepository tutorClassRepository,
            TutorSearchProjectionUtil projectionService,
            StudentSearchProjectionUtil studentProjectionService,
            UserRepository userRepository,
            OpenSearchClient openSearchClient) {
        this.tutorProfileRepository = tutorProfileRepository;
        this.tutorAvailabilityRepository = tutorAvailabilityRepository;
        this.tutorClassRepository = tutorClassRepository;
        this.projectionService = projectionService;
        this.studentProjectionService = studentProjectionService;
        this.userRepository = userRepository;
        this.openSearchClient = openSearchClient;
        this.executor = Executors.newSingleThreadExecutor();

        // Use Json format instead of Connect to avoid Kafka dependencies
        this.debeziumEngine = DebeziumEngine.create(Json.class)
                .using(mysqlConnector.asProperties())
                .notifying(this::handleChangeEvent)
                .build();
    }

    private void handleChangeEvent(ChangeEvent<String, String> event) {
        String key = event.key();
        String value = event.value();

        log.debug("Received event - Key: {}", key);

        if (value != null) {
            try {
                // Parse JSON value
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(value);

                // Extract operation and payload
                JsonNode payload = jsonNode.get("payload");
                if (payload != null) {
                    String operation = payload.has("op") ? payload.get("op").asText() : null;
                    JsonNode before = payload.get("before");
                    JsonNode after = payload.get("after");
                    JsonNode source = payload.get("source");

                    // Extract table name from source
                    String tableName = extractTableName(source);

                    // Process based on operation - focus on UPDATE events
                    if (operation != null) {
                        switch (operation) {
                            case "c": // CREATE
                                handleCreate(tableName, after);
                                break;
                            case "u": // UPDATE - most common case
                                handleUpdate(tableName, before, after);
                                break;
                            case "d": // DELETE
                                handleDelete(tableName, before);
                                break;
                            case "r": // READ (snapshot)
                                break;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error processing change event", e);
            }
        }
    }

    private String extractTableName(JsonNode source) {
        if (source != null && source.has("table")) {
            return source.get("table").asText();
        }
        return "unknown";
    }

    private void handleCreate(String tableName, JsonNode record) {
        switch (tableName) {
            case "tutor_profile":
                handleTutorProfileCreate(record);
                break;
            case "class_session":
                handleClassSessionCreate(record);
                break;
            case "tutor_profile_tags":
                handleTagsChange(record);
                break;
            case "tutor_subject":
                handleSubjectChange(record);
                break;
            case "tutor_availability":
                handleAvailabilityCreate(record);
                break;
            case "class_enrollment":
                handleClassEnrollmentCreate(record);
                break;
            case "ratings":
                handleTutorRatingCreate(record);
                break;
            case "users":
                handleUserCreate(record);
                break;
            default:
                log.debug("Ignoring CREATE for table: {}", tableName);
        }
    }

    private void handleUpdate(String tableName, JsonNode before, JsonNode after) {

        switch (tableName) {
            case "tutor_profile":
                handleTutorProfileUpdate(before, after);
                break;
            case "users":
                handleUserUpdate(before, after);
                break;
            case "tutor_availability":
                handleAvailabilityUpdate(after);
                break;
            case "tutor_profile_tags":
                handleTagsChange(after);
                break;
            case "tutor_subject":
                handleSubjectChange(after);
                break;
            case "class_enrollment":
                handleClassEnrollmentUpdate(after);
                break;
            default:
                log.debug("Ignoring update for table: {}", tableName);
        }
    }

    private void handleDelete(String tableName, JsonNode record) {
        log.info("DELETE event: table={}", tableName);
        
        switch (tableName) {
            case "tutor_profile":
                Long profileId = extractProfileId(record);
                if (profileId != null) {
                    try {
                        OpenSearchUtil.deleteDocument(openSearchClient, profileId);
                    } catch (IOException e) {
                        log.error("Failed to delete document: profileId={}", profileId, e);
                    }
                }
                break;
            case "class_session":
                handleClassSessionDelete(record);
                break;
            case "tutor_profile_tags":
                handleTagsChange(record);
                break;
            case "tutor_subject":
                handleSubjectChange(record);
                break;
            default:
                log.debug("Ignoring DELETE for table: {}", tableName);
        }
    }


    // ========== Table-Specific Handlers ==========

    private void handleTutorProfileCreate(JsonNode record) {
        log.info("CREATE event: tutor_profile");
        Long profileId = extractProfileId(record);
        if (profileId == null) return;

        String statusStr = record.has("submission_status") 
                ? record.get("submission_status").asText() 
                : null;
        
        ProfileStatus status = parseProfileStatus(statusStr);
        
        // Only index if APPROVED
        if (status == ProfileStatus.APPROVED) {
            try {
                Map<String, Object> fullDoc = projectionService.buildFullDocument(profileId);
                OpenSearchUtil.indexFullDocument(openSearchClient, profileId, fullDoc, pipelineName);
                log.info("Indexed new APPROVED profile: profileId={}", profileId);
            } catch (Exception e) {
                log.error("Failed to index new profile: profileId={}", profileId, e);
            }
        } else {
            log.debug("Skipping non-APPROVED profile: profileId={}, status={}", profileId, status);
        }
    }

    private void handleTutorProfileUpdate(JsonNode before, JsonNode after) {
        log.info("Tutor profile UPDATE event received");

        Long profileId = extractProfileId(after);
        if (profileId == null) return;

        String beforeStatusStr = before != null && before.has("submission_status")
                ? before.get("submission_status").asText()
                : null;
        String afterStatusStr = after.has("submission_status")
                ? after.get("submission_status").asText()
                : null;

        ProfileStatus beforeStatus = parseProfileStatus(beforeStatusStr);
        ProfileStatus afterStatus = parseProfileStatus(afterStatusStr);

        // KEY CASE: Status changed TO APPROVED - use full document with pipeline
        if (beforeStatus != ProfileStatus.APPROVED && afterStatus == ProfileStatus.APPROVED) {
            try {
                Map<String, Object> fullDoc = projectionService.buildFullDocument(profileId);
                OpenSearchUtil.indexFullDocument(openSearchClient, profileId, fullDoc, pipelineName);
                log.info("Profile approved - indexed full document with pipeline: profileId={}", profileId);
            } catch (Exception e) {
                log.error("Failed to index approved profile: profileId={}", profileId, e);
            }
            return;
        }

//         Status changed FROM APPROVED to non-APPROVED - delete
        if (beforeStatus == ProfileStatus.APPROVED && afterStatus != ProfileStatus.APPROVED) {
            try {
                OpenSearchUtil.deleteDocument(openSearchClient, profileId);
                log.info("Profile unapproved - deleted document: profileId={}", profileId);
            } catch (IOException e) {
                log.error("Failed to delete unapproved profile: profileId={}", profileId, e);
            }
            return;
        }

        // Still APPROVED - partial update for field changes
        if (afterStatus == ProfileStatus.APPROVED) {
            try {
                Map<String, Object> coreFields = projectionService.buildProfileCoreFields(profileId);
                OpenSearchUtil.partialUpdate(openSearchClient, profileId, coreFields);
                log.info("Profile fields updated - partial update: profileId={}", profileId);
            } catch (Exception e) {
                log.error("Failed to update profile fields: profileId={}", profileId, e);
            }
        }
    }

    private void handleUserUpdate(JsonNode before, JsonNode after) {
        String userId = extractStringField(after, "user_id");
        if (userId == null) return;

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        boolean isStudent = isVerifiedStudent(userId);
        Boolean emailVerifiedBefore = extractBooleanField(before, "email_verified");
        Boolean emailVerifiedAfter = Boolean.TRUE.equals(user.getEmailVerified());
        
        if (isStudent) {
            // Handle student creation when email becomes verified
            if (Boolean.FALSE.equals(emailVerifiedBefore) && Boolean.TRUE.equals(emailVerifiedAfter)) {
                handleStudentCreate(userId);
                return;
            }
            // Handle student updates
            if (Boolean.TRUE.equals(emailVerifiedAfter)) {
                handleStudentUpdate(after);
                return;
            }
        }

        // Handle tutor profile updates
        TutorProfile profile = tutorProfileRepository.findByUserUserId(userId).orElse(null);
        if (profile == null || profile.getSubmissionStatus() != ProfileStatus.APPROVED) {
            return;
        }

        try {
            Map<String, Object> avatarAndCountry = projectionService.buildAvatarAndCountry(profile.getId());
            OpenSearchUtil.partialUpdate(openSearchClient, profile.getId(), avatarAndCountry);
            log.info("User fields updated - partial update: profileId={}", profile.getId());
        } catch (Exception e) {
            log.error("Failed to update user fields: profileId={}", profile.getId(), e);
        }
    }

    private void handleAvailabilityUpdate(JsonNode after) {
        // Handle UPDATE event (same logic as CREATE)
        handleAvailabilityCreate(after);
    }

    private void handleAvailabilityCreate(JsonNode record) {
        // When availability is created or updated
        log.info("Availability create/update event received");
        String userId = record.has("user_id") ? record.get("user_id").asText() : null;
        if (userId == null) return;

        TutorProfile profile = tutorProfileRepository.findByUserUserId(userId).orElse(null);
        if (profile == null || profile.getSubmissionStatus() != ProfileStatus.APPROVED) {
            return;
        }

        try {
            TutorAvailability availability = tutorAvailabilityRepository
                    .findByUserUserId(userId)
                    .orElse(null);
            
            Map<String, Object> availabilityDoc = projectionService.buildAvailability(availability);
            OpenSearchUtil.partialUpdate(openSearchClient, profile.getId(), availabilityDoc);
            log.info("Availability created/updated - partial update: profileId={}", profile.getId());
        } catch (Exception e) {
            log.error("Failed to update availability: profileId={}", profile.getId(), e);
        }
    }

    private void handleTagsChange(JsonNode record) {
        // Handles CREATE, UPDATE, and DELETE events for tutor_profile_tags
        // For ManyToMany relationships, all operations require rebuilding the array
        log.info("Tags change event received");
        Long profileId = extractLongField(record, "tutor_profile_id");
        if (profileId == null) return;

        TutorProfile profile = tutorProfileRepository.findById(profileId).orElse(null);
        if (profile == null || profile.getSubmissionStatus() != ProfileStatus.APPROVED) {
            return;
        }

        try {
            Map<String, Object> teachingStyle = projectionService.buildTeachingStyle(profileId);
            OpenSearchUtil.partialUpdate(openSearchClient, profileId, teachingStyle);
            log.info("Teaching style updated: profileId={}", profileId);
        } catch (Exception e) {
            log.error("Failed to update teaching style: profileId={}", profileId, e);
        }
    }

    private void handleSubjectChange(JsonNode record) {
        // Handles CREATE, UPDATE, and DELETE events for tutor_subject
        // For ManyToMany relationships, all operations require rebuilding the array
        Long profileId = extractLongField(record, "profile_id");
        if (profileId == null) return;

        TutorProfile profile = tutorProfileRepository.findById(profileId).orElse(null);
        if (profile == null || profile.getSubmissionStatus() != ProfileStatus.APPROVED) {
            return;
        }

        try {
            Map<String, Object> subjects = projectionService.buildSubjects(profileId);
            OpenSearchUtil.partialUpdate(openSearchClient, profileId, subjects);
            log.info("Subjects updated: profileId={}", profileId);
        } catch (Exception e) {
            log.error("Failed to update subjects: profileId={}", profileId, e);
        }
    }

    private void handleClassSessionCreate(JsonNode record) {
        // Increment totalLessons when a new class session is created
        Long profileId = resolveTutorProfileFromClassSession(record);
        if (profileId == null) return;

        try {
            String scriptSource = """
                if (ctx._source.totalLessons == null) {
                  ctx._source.totalLessons = 1;
                } else {
                  ctx._source.totalLessons += 1;
                }
                """;
            
            OpenSearchUtil.scriptUpdate(openSearchClient, profileId, scriptSource, null);
            log.info("Class session created - incremented totalLessons: profileId={}", profileId);
        } catch (Exception e) {
            log.error("Failed to increment totalLessons: profileId={}", profileId, e);
        }
    }

    private void handleClassSessionDelete(JsonNode record) {
        // Decrement totalLessons when a class session is deleted
        Long profileId = resolveTutorProfileFromClassSession(record);
        if (profileId == null) return;

        try {
            String scriptSource = """
                if (ctx._source.totalLessons != null && ctx._source.totalLessons > 0) {
                  ctx._source.totalLessons -= 1;
                }
                """;
            
            OpenSearchUtil.scriptUpdate(openSearchClient, profileId, scriptSource, null);
            log.info("Class session deleted - decremented totalLessons: profileId={}", profileId);
        } catch (Exception e) {
            log.error("Failed to decrement totalLessons: profileId={}", profileId, e);
        }
    }

    /**
     * Resolves tutor profile ID from class_session event.
     * Chain: class_session.class_id -> tutor_class.tutor_id -> user.userId -> tutor_profile
     */
    private Long resolveTutorProfileFromClassSession(JsonNode record) {
        // Extract class_id from class_session record
        Long classId = extractLongField(record, "class_id");
        if (classId == null) {
            log.warn("Could not extract class_id from class_session event");
            return null;
        }

        // Find TutorClass by id
        var tutorClass = tutorClassRepository.findById(classId).orElse(null);
        if (tutorClass == null || tutorClass.getTutor() == null) {
            log.warn("TutorClass not found or has no tutor: classId={}", classId);
            return null;
        }

        // Get tutor userId
        String tutorUserId = tutorClass.getTutor().getUserId();
        if (tutorUserId == null) {
            log.warn("Tutor has no userId: classId={}", classId);
            return null;
        }

        // Find TutorProfile by user.userId
        var profile = tutorProfileRepository.findByUserUserId(tutorUserId).orElse(null);
        if (profile == null) {
            log.debug("TutorProfile not found for userId: {}", tutorUserId);
            return null;
        }

        // Only update if APPROVED
        if (profile.getSubmissionStatus() != ProfileStatus.APPROVED) {
            log.debug("TutorProfile not APPROVED, skipping: profileId={}, status={}",
                    profile.getId(), profile.getSubmissionStatus());
            return null;
        }

        return profile.getId();
    }

    // ========== Student Event Handlers ==========

    private void handleUserCreate(JsonNode record) {
        String userId = extractStringField(record, "user_id");
        if (userId == null) return;

        Boolean emailVerified = extractBooleanField(record, "email_verified");
        if (Boolean.TRUE.equals(emailVerified) && isVerifiedStudent(userId)) {
            handleStudentCreate(userId);
        }
    }

    private void handleStudentCreate(String userId) {
        try {
            Map<String, Object> fullDoc = studentProjectionService.buildFullDocument(userId);
            OpenSearchUtil.indexStudentDocument(openSearchClient, userId, fullDoc);
            log.info("Student created - indexed document: userId={}", userId);
        } catch (Exception e) {
            log.error("Failed to create student document: userId={}", userId, e);
        }
    }

    private void handleStudentUpdate(JsonNode record) {
        String userId = extractStringField(record, "user_id");
        if (userId == null) return;

        try {
            Map<String, Object> basicFields = studentProjectionService.buildBasicFields(userId);
            
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getPreferences() != null) {
                basicFields.putAll(studentProjectionService.buildPreferredTeachingStyles(user.getPreferences()));
            }

            OpenSearchUtil.partialUpdateStudent(openSearchClient, userId, basicFields);
            log.info("Student updated - partial update: userId={}", userId);
        } catch (Exception e) {
            log.error("Failed to update student: userId={}", userId, e);
        }
    }

    private void handleClassEnrollmentCreate(JsonNode record) {
        String studentId = extractStringField(record, "student_id");
        if (studentId == null || !isVerifiedStudent(studentId)) {
            return;
        }

        try {
            Map<String, Object> enrollments = studentProjectionService.buildEnrollments(studentId);
            OpenSearchUtil.partialUpdateStudent(openSearchClient, studentId, enrollments);
            log.info("Enrollment updated - student enrollments: userId={}", studentId);
        } catch (Exception e) {
            log.error("Failed to update enrollments: userId={}", studentId, e);
        }
    }

    private void handleClassEnrollmentUpdate(JsonNode after) {
        // When enrollment is updated, rebuild enrollments array
        handleClassEnrollmentCreate(after);
    }

    private void handleTutorRatingCreate(JsonNode record) {
        String studentId = extractStringField(record, "student_id");
        if (studentId == null || !isVerifiedStudent(studentId)) {
            return;
        }

        try {
            Map<String, Object> tutorRatings = studentProjectionService.buildTutorRatings(studentId);
            OpenSearchUtil.partialUpdateStudent(openSearchClient, studentId, tutorRatings);
            log.info("Tutor rating added - updated student ratings: userId={}", studentId);
        } catch (Exception e) {
            log.error("Failed to update tutor ratings: userId={}", studentId, e);
        }
    }

    // ========== Helper Methods ==========

    private Long extractProfileId(JsonNode record) {
        if (record == null) return null;
        return extractLongField(record, "id");
    }

    private Long extractLongField(JsonNode record, String fieldName) {
        if (record == null || !record.has(fieldName)) return null;
        JsonNode field = record.get(fieldName);
        if (field.isNumber()) {
            return field.asLong();
        } else if (field.isTextual()) {
            try {
                return Long.parseLong(field.asText());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private ProfileStatus parseProfileStatus(String statusStr) {
        if (statusStr == null) return ProfileStatus.DRAFT;
        try {
            return ProfileStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ProfileStatus.DRAFT;
        }
    }


    private Boolean extractBooleanField(JsonNode record, String fieldName) {
        if (record == null || !record.has(fieldName)) return null;
        JsonNode field = record.get(fieldName);
        if (field.isBoolean()) {
            return field.asBoolean();
        } else if (field.isNumber()) {
            return field.asInt() != 0;
        } else if (field.isTextual()) {
            String text = field.asText();
            return "true".equalsIgnoreCase(text) || "1".equals(text);
        }
        return null;
    }

    private String extractStringField(JsonNode record, String fieldName) {
        if (record == null || !record.has(fieldName)) return null;
        JsonNode field = record.get(fieldName);
        return field.isTextual() ? field.asText() : null;
    }

    private boolean isVerifiedStudent(String userId) {
        User user = userRepository.findByIdWithRole(userId).orElse(null);
        return user != null 
                && Boolean.TRUE.equals(user.getEmailVerified())
                && user.getRole() != null 
                && PredefinedRole.STUDENT_ROLE.equals(user.getRole().getName());
    }

    @PostConstruct
    private void start() {
        this.executor.execute(debeziumEngine);
        log.info("Debezium engine started successfully");
    }

    @PreDestroy
    private void stop() throws IOException {
        if (this.debeziumEngine != null) {
            this.debeziumEngine.close();
            log.info("Debezium engine stopped");
        }
    }
}
