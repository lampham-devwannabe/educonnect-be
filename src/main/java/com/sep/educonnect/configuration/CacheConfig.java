package com.sep.educonnect.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return new CustomCacheManager();
    }

    private static class CustomCacheManager implements CacheManager {
        private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>();

        public CustomCacheManager() {
            // Cache for tutors (5 minutes)
            Caffeine<Object, Object> tutorsCaffeine = Caffeine.newBuilder()
                    .maximumSize(2000)
                    .expireAfterWrite(Duration.ofMinutes(5));
            cacheMap.put("tutors", new CaffeineCache("tutors", tutorsCaffeine.build()));

            // Cache for tutorsBySubject (1 day)
            Caffeine<Object, Object> tutorsBySubjectCaffeine = Caffeine.newBuilder()
                    .maximumSize(500)
                    .expireAfterWrite(Duration.ofDays(1));
            cacheMap.put("tutorsBySubject", new CaffeineCache("tutorsBySubject", tutorsBySubjectCaffeine.build()));

            // Cache for topTutors (1 hour)
            Caffeine<Object, Object> topTutorsCaffeine = Caffeine.newBuilder()
                    .maximumSize(100)
                    .expireAfterWrite(Duration.ofHours(1));
            cacheMap.put("topTutors", new CaffeineCache("topTutors", topTutorsCaffeine.build()));

            // Cache for topCourses (1 hour)
            Caffeine<Object, Object> topCoursesCaffeine = Caffeine.newBuilder()
                    .maximumSize(100)
                    .expireAfterWrite(Duration.ofHours(1));
            cacheMap.put("topCourses", new CaffeineCache("topCourses", topCoursesCaffeine.build()));

            // Cache for tutor monthly summaries (24 hours)
            Caffeine<Object, Object> tutorMonthlySummaryCaffeine = Caffeine.newBuilder()
                    .maximumSize(2000)
                    .expireAfterWrite(Duration.ofDays(1));
            cacheMap.put("tutorMonthlySummary", new CaffeineCache("tutorMonthlySummary", tutorMonthlySummaryCaffeine.build()));
        }

        @Override
        public Cache getCache(String name) {
            return cacheMap.get(name);
        }

        @Override
        public Collection<String> getCacheNames() {
            return Collections.unmodifiableSet(cacheMap.keySet());
        }
    }
}
