package com.sep.educonnect.utils;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class RevenueCalculationUtil {

    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile("(\\d+)\\s*years?", Pattern.CASE_INSENSITIVE);

    // Experience ranges and corresponding percentage ranges
    private enum ExperienceLevel {
        JUNIOR(0, 2, 0.50, 0.60),      // 50-60% for online, 15-25% for self-paced
        MID_LEVEL(3, 5, 0.55, 0.65),   // 55-65% for online, 20-30% for self-paced
        SENIOR(6, 10, 0.60, 0.70),     // 60-70% for online, 25-35% for self-paced
        EXPERT(11, Integer.MAX_VALUE, 0.65, 0.70); // 65-70% for online, 30-35% for self-paced

        private final int minYears;
        private final int maxYears;
        private final double onlineMinPercent;
        private final double onlineMaxPercent;

        ExperienceLevel(int minYears, int maxYears, double onlineMinPercent, double onlineMaxPercent) {
            this.minYears = minYears;
            this.maxYears = maxYears;
            this.onlineMinPercent = onlineMinPercent;
            this.onlineMaxPercent = onlineMaxPercent;
        }

        public double getOnlineMinPercent() { return onlineMinPercent; }
        public double getOnlineMaxPercent() { return onlineMaxPercent; }

        // For self-paced courses, use 15-35% range regardless of experience
        public double getSelfPacedMinPercent() { return 0.15; }
        public double getSelfPacedMaxPercent() { return 0.35; }

        public static ExperienceLevel fromYears(int years) {
            for (ExperienceLevel level : values()) {
                if (years >= level.minYears && years <= level.maxYears) {
                    return level;
                }
            }
            return JUNIOR; // Default fallback
        }
    }

    /**
     * Calculate tutor's revenue share for online courses
     * Formula: (completedSessions / totalSessions) * coursePrice * tutorPercentage
     */
    public static BigDecimal calculateOnlineCourseRevenue(int completedSessions, int totalSessions,
                                                         BigDecimal coursePrice, String experience) {
        if (coursePrice == null || coursePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (totalSessions <= 0) {
            log.warn("Invalid total sessions count: {}", totalSessions);
            return BigDecimal.ZERO;
        }

        if (completedSessions < 0) {
            log.warn("Invalid completed sessions count: {}", completedSessions);
            return BigDecimal.ZERO;
        }

        try {
            // Calculate completion ratio
            double completionRatio = Math.min((double) completedSessions / totalSessions, 1.0);

            // Get tutor percentage based on experience
            double tutorPercentage = getTutorPercentage(experience, true);

            // Calculate revenue: completionRatio * coursePrice * tutorPercentage
            BigDecimal revenue = coursePrice
                .multiply(BigDecimal.valueOf(completionRatio))
                .multiply(BigDecimal.valueOf(tutorPercentage));

            return revenue.setScale(2, RoundingMode.HALF_UP);

        } catch (Exception e) {
            log.error("Error calculating online course revenue: completedSessions={}, totalSessions={}, coursePrice={}, experience={}",
                     completedSessions, totalSessions, coursePrice, experience, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Calculate tutor's revenue share for self-paced courses
     * Formula: coursePrice * tutorPercentage
     */
    public static BigDecimal calculateSelfPacedCourseRevenue(BigDecimal coursePrice, String experience) {
        if (coursePrice == null || coursePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        try {
            double tutorPercentage = getTutorPercentage(experience, false);
            BigDecimal revenue = coursePrice.multiply(BigDecimal.valueOf(tutorPercentage));
            return revenue.setScale(2, RoundingMode.HALF_UP);

        } catch (Exception e) {
            log.error("Error calculating self-paced course revenue: coursePrice={}, experience={}",
                     coursePrice, experience, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Parse experience string and return years of experience
     */
    private static int parseExperienceYears(String experience) {
        if (experience == null || experience.trim().isEmpty()) {
            return 0; // Default to junior level
        }

        try {
            Matcher matcher = EXPERIENCE_PATTERN.matcher(experience);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception e) {
            log.warn("Failed to parse experience string: {}", experience, e);
        }

        return 0; // Default to junior level
    }

    /**
     * Get tutor percentage based on experience level
     * For online courses: use the full range (50-70%)
     * For self-paced courses: use reduced range (15-35%)
     */
    private static double getTutorPercentage(String experience, boolean isOnlineCourse) {
        int years = parseExperienceYears(experience);
        ExperienceLevel level = ExperienceLevel.fromYears(years);

        if (isOnlineCourse) {
            // For online courses, use experience-based percentage within 50-70% range
            double range = level.getOnlineMaxPercent() - level.getOnlineMinPercent();
            // Use midpoint of the range for simplicity
            return level.getOnlineMinPercent() + (range / 2);
        } else {
            // For self-paced courses, use experience-based percentage within 15-35% range
            double range = level.getSelfPacedMaxPercent() - level.getSelfPacedMinPercent();
            // Use midpoint of the range for simplicity
            return level.getSelfPacedMinPercent() + (range / 2);
        }
    }
}