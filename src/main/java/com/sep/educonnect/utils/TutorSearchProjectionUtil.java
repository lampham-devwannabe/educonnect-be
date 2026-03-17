package com.sep.educonnect.utils;

import com.sep.educonnect.entity.*;
import com.sep.educonnect.enums.ProfileStatus;
import com.sep.educonnect.enums.TeachingSlot;
import com.sep.educonnect.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service for building partial field projections for OpenSearch tutor documents.
 * Each method builds only the fields needed for a specific update, avoiding full document rebuilds.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TutorSearchProjectionUtil {

    private final TutorProfileRepository tutorProfileRepository;
    private final TutorAvailabilityRepository tutorAvailabilityRepository;
    private final SyllabusRepository syllabusRepository;

    /**
     * Builds full document - used only when status changes to APPROVED or embedding fields change.
     * This method aggregates all data and builds a complete document structure.
     */
    public Map<String, Object> buildFullDocument(Long profileId) {
        TutorProfile profile = tutorProfileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("TutorProfile not found: " + profileId));

        if (profile.getSubmissionStatus() != ProfileStatus.APPROVED) {
            throw new IllegalStateException("Profile must be APPROVED to build full document: " + profileId);
        }

        User user = profile.getUser();
        if (user == null) {
            throw new IllegalStateException("User not found for profile: " + profileId);
        }

        TutorAvailability availability = tutorAvailabilityRepository
                .findByUserUserId(user.getUserId())
                .orElse(null);

        Map<String, Object> doc = new HashMap<>();
        doc.put("id", profile.getId());

        // Name (bilingual) - flat fields
        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") +
                " " + (user.getLastName() != null ? user.getLastName() : "");
        doc.put("name_en", fullName.trim());
        doc.put("name_vi", fullName.trim());

        // Headline (bilingual) - flat fields
        doc.put("headline_en", profile.getBioEn() != null ? profile.getBioEn() : "");
        doc.put("headline_vi", profile.getBioVi() != null ? profile.getBioVi() : "");

        // Description (bilingual) - flat fields
        doc.put("description_en", profile.getDescEn() != null ? profile.getDescEn() : "");
        doc.put("description_vi", profile.getDescVi() != null ? profile.getDescVi() : "");

        // Avatar
        doc.put("avatarUrl", user.getAvatar());

        // Country (parsed from address)
        doc.put("country", parseCountry(user.getAddress()));

        // Price
        doc.put("price", profile.getHourlyRate() != null ? profile.getHourlyRate() : BigDecimal.ZERO);

        // Rating and counts
        doc.put("rating", profile.getRating() != null ? profile.getRating() : 0.0);
        doc.put("studentsCount", profile.getStudentCount() != null ? profile.getStudentCount() : 0);
        doc.put("numberReviews", profile.getReviewCount() != null ? profile.getReviewCount() : 0);

        // Experience
        Integer yearExperience = parseYears(profile.getExperience());
        doc.put("yearExperience", yearExperience);
        doc.put("isProfessional", yearExperience > 5);

        // Teaching style (tags)
        doc.put("teachingStyle", buildTeachingStyle(profileId));

        // Subjects with levels
        doc.put("subject", buildSubjects(profileId));

        // Availability
        doc.put("availability", buildAvailability(availability));

        // Total lessons
        doc.put("totalLessons", 0);

        return doc;
    }

    /**
     * Builds profile core fields: price, rating, counts, experience, isProfessional, videoLink.
     */
    public Map<String, Object> buildProfileCoreFields(Long profileId) {
        TutorProfile profile = tutorProfileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("TutorProfile not found: " + profileId));

        Map<String, Object> fields = new HashMap<>();
        fields.put("price", profile.getHourlyRate() != null ? profile.getHourlyRate() : BigDecimal.ZERO);
        fields.put("rating", profile.getRating() != null ? profile.getRating() : 0.0);
        fields.put("studentsCount", profile.getStudentCount() != null ? profile.getStudentCount() : 0);
        fields.put("numberReviews", profile.getReviewCount() != null ? profile.getReviewCount() : 0);

        Integer yearExperience = parseYears(profile.getExperience());
        fields.put("yearExperience", yearExperience);
        fields.put("isProfessional", yearExperience > 5);
        fields.put("videoLink", profile.getVideoLink());

        return fields;
    }

    /**
     * Builds availability array from TutorAvailability entity.
     */
    public Map<String, Object> buildAvailability(TutorAvailability availability) {
        Map<String, Object> fields = new HashMap<>();

        if (availability == null) {
            fields.put("availability", Collections.emptyList());
            return fields;
        }

        List<Map<String, Object>> availabilityList = new ArrayList<>();
        String[] dayNames = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        int[] dayOfWeekValues = {1, 2, 3, 4, 5, 6, 0};

        for (int i = 0; i < dayNames.length; i++) {
            String dayName = dayNames[i];
            int dayOfWeek = dayOfWeekValues[i];
            List<Integer> slotNumbers = availability.getSlotsByDay(dayOfWeek);

            if (slotNumbers != null && !slotNumbers.isEmpty()) {
                Collections.sort(slotNumbers);

                int groupStart = 0;
                for (int j = 1; j <= slotNumbers.size(); j++) {
                    // Check if we've reached end or found a gap
                    boolean isEndOfList = (j == slotNumbers.size());
                    boolean isGap = !isEndOfList && (slotNumbers.get(j) != slotNumbers.get(j - 1) + 1);

                    if (isEndOfList || isGap) {
                        // Process the group from groupStart to j-1
                        Integer firstSlot = slotNumbers.get(groupStart);
                        Integer lastSlot = slotNumbers.get(j - 1);

                        Optional<TeachingSlot> startSlotOpt = TeachingSlot.findBySlotNumber(firstSlot);
                        Optional<TeachingSlot> endSlotOpt = TeachingSlot.findBySlotNumber(lastSlot);

                        if (startSlotOpt.isPresent() && endSlotOpt.isPresent()) {
                            Map<String, Object> slotMap = new HashMap<>();
                            slotMap.put("day", dayName);
                            slotMap.put("startTime", startSlotOpt.get().getStartTime().toSecondOfDay() / 60);
                            slotMap.put("endTime", endSlotOpt.get().getEndTime().toSecondOfDay() / 60);
                            availabilityList.add(slotMap);
                        }

                        // Start new group
                        groupStart = j;
                    }
                }
            }
        }

        fields.put("availability", availabilityList);
        return fields;
    }

    /**
     * Builds teaching style array from tags.
     */
    public Map<String, Object> buildTeachingStyle(Long profileId) {
        TutorProfile profile = tutorProfileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("TutorProfile not found: " + profileId));

        Set<Tag> tags = profile.getTags();
        if (tags == null || tags.isEmpty()) {
            return Map.of("teachingStyle", Collections.emptyList());
        }

        List<Map<String, String>> teachingStyleList = tags.stream()
                .map(tag -> {
                    Map<String, String> tagMap = new HashMap<>();
                    tagMap.put("en", tag.getNameEn() != null ? tag.getNameEn() : "");
                    tagMap.put("vi", tag.getNameVi() != null ? tag.getNameVi() : "");
                    return tagMap;
                })
                .toList();

        return Map.of("teachingStyle", teachingStyleList);
    }

    /**
     * Builds subjects array with nested levels from syllabus.
     */
    public Map<String, Object> buildSubjects(Long profileId) {
        TutorProfile profile = tutorProfileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("TutorProfile not found: " + profileId));

        Set<Subject> subjects = profile.getSubjects();
        if (subjects == null || subjects.isEmpty()) {
            return Map.of("subject", Collections.emptyList());
        }

        List<Map<String, Object>> subjectList = new ArrayList<>();

        for (Subject subject : subjects) {
            Map<String, Object> subjectMap = new HashMap<>();
            subjectMap.put("en", subject.getNameEn() != null ? subject.getNameEn() : "");
            subjectMap.put("vi", subject.getNameVi() != null ? subject.getNameVi() : "");

            // Hash ID from English name
            String nameEn = subject.getNameEn() != null ? subject.getNameEn() : "";
            subjectMap.put("id", Math.abs(nameEn.hashCode()));

            // Fetch levels (syllabus) for this subject
            List<Syllabus> syllabi = syllabusRepository.findBySubjectIdAndNotDeleted(subject.getSubjectId());
            List<Map<String, Object>> levelList = new ArrayList<>();

            for (Syllabus syllabus : syllabi) {
                Map<String, Object> levelMap = new HashMap<>();
                levelMap.put("en", syllabus.getLevelEn() != null ? syllabus.getLevelEn() : "");
                levelMap.put("vi", syllabus.getLevelVi() != null ? syllabus.getLevelVi() : "");

                // Hash ID from English level name
                String levelEn = syllabus.getLevelEn() != null ? syllabus.getLevelEn() : "";
                levelMap.put("id", Math.abs(levelEn.hashCode()));

                levelList.add(levelMap);
            }

            subjectMap.put("level", levelList);
            subjectList.add(subjectMap);
        }

        return Map.of("subject", subjectList);
    }

    /**
     * Builds avatar and country fields from User.
     */
    public Map<String, Object> buildAvatarAndCountry(Long profileId) {
        TutorProfile profile = tutorProfileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("TutorProfile not found: " + profileId));

        User user = profile.getUser();
        if (user == null) {
            throw new IllegalStateException("User not found for profile: " + profileId);
        }

        Map<String, Object> fields = new HashMap<>();
        fields.put("avatarUrl", user.getAvatar());
        fields.put("country", parseCountry(user.getAddress()));
        return fields;
    }

    // ========== Helper Methods ==========

    /**
     * Parses years from experience string (e.g., "5 years" -> 5).
     */
    private Integer parseYears(String experience) {
        if (experience == null || experience.trim().isEmpty()) {
            return 0;
        }

        // Try to extract number from string like "5 years", "10 years", etc.
        String cleaned = experience.trim().toLowerCase();
        String[] parts = cleaned.split("\\s+");
        for (String part : parts) {
            try {
                return Integer.parseInt(part);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Parses country from address string.
     * This is a simple implementation - can be enhanced with proper country lookup/mapping.
     */
    private Map<String, String> parseCountry(String address) {
        Map<String, String> country = new HashMap<>();
        
        if (address == null || address.trim().isEmpty()) {
            country.put("code", "VN"); // Default to Vietnam
            country.put("name_en", "Vietnam");
            country.put("name_vi", "Việt Nam");
            return country;
        }

        // Simple heuristic: check for common country names
        String addrLower = address.toLowerCase();
        if (addrLower.contains("vietnam") || addrLower.contains("việt nam") || addrLower.contains("vn")) {
            country.put("code", "VN");
            country.put("name_en", "Vietnam");
            country.put("name_vi", "Việt Nam");
        } else if (addrLower.contains("usa") || addrLower.contains("united states") || addrLower.contains("us")) {
            country.put("code", "US");
            country.put("name_en", "United States");
            country.put("name_vi", "Hoa Kỳ");
        } else {
            // Default to Vietnam if cannot determine
            country.put("code", "VN");
            country.put("name_en", "Vietnam");
            country.put("name_vi", "Việt Nam");
        }

        return country;
    }
}

