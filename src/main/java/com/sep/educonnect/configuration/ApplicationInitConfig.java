package com.sep.educonnect.configuration;

import com.sep.educonnect.constant.PredefinedRole;
import com.sep.educonnect.entity.Role;
import com.sep.educonnect.entity.Tag;
import com.sep.educonnect.entity.User;
import com.sep.educonnect.repository.RoleRepository;
import com.sep.educonnect.repository.TagRepository;
import com.sep.educonnect.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@ConditionalOnProperty("spring.profiles.active != 'local'")
public class ApplicationInitConfig {

    PasswordEncoder passwordEncoder;

    @NonFinal
    static final String ADMIN_USER_NAME = "admin";

    @NonFinal
    static final String ADMIN_PASSWORD = "admin";

    // create account for staff, student, tutor
    static final String STAFF_USER_NAME = "staff";
    static final String STAFF_PASSWORD = "staff";
    static final String STUDENT_USER_NAME = "student";
    static final String STUDENT_PASSWORD = "student";
    static final String TUTOR_USER_NAME = "tutor";
    static final String TUTOR_PASSWORD = "tutor";

    @Bean
    @ConditionalOnProperty(prefix = "spring", value = "datasource.driver-class-name", havingValue = "com.mysql.cj.jdbc.Driver")
    ApplicationRunner applicationRunner(UserRepository userRepository, RoleRepository roleRepository,
                                        TagRepository tagRepository) {
        log.info("Initializing application.....");
        return args -> {
            seedDataInit(userRepository, roleRepository, tagRepository);
            log.info("Application initialization completed .....");

            log.info("Access Swagger UI at: https://api.educonnect.dev/swagger-ui/index.html");

            // log all environment variables
            StringJoiner envVars = new StringJoiner(", ");
            System.getenv().forEach((key, value) -> envVars.add(key + "=" + value));
            log.info("Environment Variables: " + envVars.toString());
        };
    }

    @Transactional
    public void seedDataInit(UserRepository userRepository, RoleRepository roleRepository,
                             TagRepository tagRepository) {

        // ===== 1. Seed Roles =====
        seedRoles(roleRepository);

        // ===== 2. Seed Teaching Style Tags =====
        seedTeachingStyleTags(tagRepository);

        // ===== 3. Seed Users =====
        seedUsers(userRepository, roleRepository);
    }

    private void seedRoles(RoleRepository roleRepository) {
        if (roleRepository.findByName(PredefinedRole.ADMIN_ROLE).isEmpty()) {
            roleRepository.save(Role.builder().name(PredefinedRole.ADMIN_ROLE).build());
            log.info("Admin role created");
        }

        if (roleRepository.findByName(PredefinedRole.TUTOR_ROLE).isEmpty()) {
            roleRepository.save(Role.builder().name(PredefinedRole.TUTOR_ROLE).build());
            log.info("Tutor role created");
        }

        if (roleRepository.findByName(PredefinedRole.STUDENT_ROLE).isEmpty()) {
            roleRepository.save(Role.builder().name(PredefinedRole.STUDENT_ROLE).build());
            log.info("Student role created");
        }

        if (roleRepository.findByName(PredefinedRole.STAFF_ROLE).isEmpty()) {
            roleRepository.save(Role.builder().name(PredefinedRole.STAFF_ROLE).build());
            log.info("Staff role created");
        }
    }

    private void seedTeachingStyleTags(TagRepository tagRepository) {
        List<Tag> existingTags = tagRepository.findAll();

        if (existingTags.isEmpty()) {
            List<Tag> teachingStyles = Arrays.asList(
                    Tag.builder().nameEn("Interactive").nameVi("Tương tác").build(),
                    Tag.builder().nameEn("Conversational").nameVi("Hội thoại").build(),
                    Tag.builder().nameEn("Structured Lessons").nameVi("Bài học có cấu trúc").build(),
                    Tag.builder().nameEn("Practice-Oriented").nameVi("Tập trung thực hành").build(),
                    Tag.builder().nameEn("Exam-Oriented").nameVi("Tập trung luyện thi").build(),
                    Tag.builder().nameEn("Discussion-Based").nameVi("Thảo luận nhóm").build(),
                    Tag.builder().nameEn("Visual & Multimedia Aids").nameVi("Hình ảnh & đa phương tiện").build(),
                    Tag.builder().nameEn("Step-by-Step Explanation").nameVi("Giải thích từng bước").build(),
                    Tag.builder().nameEn("Project-Based Learning").nameVi("Học qua dự án").build(),
                    Tag.builder().nameEn("Gamified Learning").nameVi("Học qua trò chơi").build(),
                    Tag.builder().nameEn("Flexible Pace").nameVi("Tiến độ linh hoạt").build(),
                    Tag.builder().nameEn("Student-Centered").nameVi("Lấy học viên làm trung tâm").build(),
                    Tag.builder().nameEn("Intensive Coaching").nameVi("Huấn luyện chuyên sâu").build());

            tagRepository.saveAll(teachingStyles);
            log.info("Created {} teaching style tags", teachingStyles.size());
        } else {
            log.info("Teaching style tags already exist, skipping...");
        }
    }

    private void seedUsers(UserRepository userRepository, RoleRepository roleRepository) {
        // Admin user
        if (userRepository.findByUsername(ADMIN_USER_NAME).isEmpty()) {
            User user = User.builder()
                    .username(ADMIN_USER_NAME)
                    .password(passwordEncoder.encode(ADMIN_PASSWORD))
                    .role(roleRepository.findByName(PredefinedRole.ADMIN_ROLE)
                            .orElseThrow(IllegalStateException::new))
                    .build();

            userRepository.save(user);
            log.warn("Admin user has been created with default password: {}", ADMIN_PASSWORD);
        }

        // Staff user
        if (userRepository.findByUsername(STAFF_USER_NAME).isEmpty()) {
            User user = User.builder()
                    .username(STAFF_USER_NAME)
                    .password(passwordEncoder.encode(STAFF_PASSWORD))
                    .role(roleRepository.findByName(PredefinedRole.STAFF_ROLE)
                            .orElseThrow(IllegalStateException::new))
                    .build();

            userRepository.save(user);
            log.warn("Staff user has been created with default password: {}", STAFF_PASSWORD);
        }

        // Student user
        if (userRepository.findByUsername(STUDENT_USER_NAME).isEmpty()) {
            User user = User.builder()
                    .username(STUDENT_USER_NAME)
                    .password(passwordEncoder.encode(STUDENT_PASSWORD))
                    .role(roleRepository.findByName(PredefinedRole.STUDENT_ROLE)
                            .orElseThrow(IllegalStateException::new))
                    .build();

            userRepository.save(user);
            log.warn("Student user has been created with default password: {}", STUDENT_PASSWORD);
        }

        // Tutor user with teaching styles
        if (userRepository.findByUsername(TUTOR_USER_NAME).isEmpty()) {

            User user = User.builder()
                    .username(TUTOR_USER_NAME)
                    .password(passwordEncoder.encode(TUTOR_PASSWORD))
                    .role(roleRepository.findByName(PredefinedRole.TUTOR_ROLE)
                            .orElseThrow(IllegalStateException::new))
                    .build();

            userRepository.save(user);
            log.warn("Tutor user has been created with default password: {}", TUTOR_PASSWORD);
        }
    }

}
