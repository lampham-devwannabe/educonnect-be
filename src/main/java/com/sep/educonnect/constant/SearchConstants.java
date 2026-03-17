package com.sep.educonnect.constant;

import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

public class SearchConstants {
    public static class Index {
        public static final IndexCoordinates TUTORS = IndexCoordinates.of("tutors_embed");
        public static final IndexCoordinates STUDENTS = IndexCoordinates.of("students_v1");
    }

    public static class Fuzzy {
        public static final String LEVEL = "1";
        public static final Integer PREFIX_LENGTH = 2;
    }

    public static class Tutor {
        // Basic Info
        public static final String ID = "id";
        public static final String AVATAR_URL = "avatarUrl";
        public static final String IS_PROFESSIONAL = "isProfessional";

        // Name (multilingual)
        public static final String NAME = "name";
        public static final String NAME_VI = "name.vi";
        public static final String NAME_EN = "name.en";

        // Headline (multilingual)
        public static final String HEADLINE = "headline";
        public static final String HEADLINE_EN = "headline.en";
        public static final String HEADLINE_VI = "headline.vi";

        // Description (multilingual)
        public static final String DESCRIPTION = "description";
        public static final String DESCRIPTION_EN = "description.en";
        public static final String DESCRIPTION_VI = "description.vi";

        // Pricing & Reviews
        public static final String PRICE = "price";
        public static final String RATING = "rating";
        public static final String NUMBER_REVIEWS = "numberReviews";

        // Statistics
        public static final String STUDENTS_COUNT = "studentsCount";
        public static final String TOTAL_LESSONS = "totalLessons";

        public static final String EMBEDDING = "embedding";
        // Country
        public static final String COUNTRY = "country";
        public static final String COUNTRY_CODE = "country.code";
        public static final String COUNTRY_NAME_EN = "country.name_en";
        public static final String COUNTRY_NAME_VI = "country.name_vi";

        // Subject & Level
        public static final String SUBJECT = "subject";
        public static final String SUBJECT_EN = "subject.en";
        public static final String SUBJECT_VI = "subject.vi";

        public static final String LEVEL = "subject.level";
        public static final String LEVEL_EN = "subject.level.en";
        public static final String LEVEL_VI = "subject.level.vi";

        // Teaching Style (multilingual)
        public static final String TEACHING_STYLE = "teachingStyle";
        public static final String TEACHING_STYLE_EN = "teachingStyle.en";
        public static final String TEACHING_STYLE_VI = "teachingStyle.vi";

        // Availability
        public static final String AVAILABILITY = "availability";
        public static final String AVAILABILITY_DAY = "availability.day";
        public static final String AVAILABILITY_START_TIME = "availability.startTime";
        public static final String AVAILABILITY_END_TIME = "availability.endTime";
    }
}
