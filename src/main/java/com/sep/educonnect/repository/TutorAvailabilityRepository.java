package com.sep.educonnect.repository;

import com.sep.educonnect.entity.TutorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TutorAvailabilityRepository extends JpaRepository<TutorAvailability, Long> {
    Optional<TutorAvailability> findByUserUserId(String userId);
}
