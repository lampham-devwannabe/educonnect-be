package com.sep.educonnect.repository;

import com.sep.educonnect.entity.VerificationProcess;
import com.sep.educonnect.enums.VerificationStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationProcessRepository extends JpaRepository<VerificationProcess, Long> {
    Optional<VerificationProcess> findByProfileIdAndCompletedAtIsNull(Long profileId);

    Optional<VerificationProcess> findByProfileIdAndCurrentStage(Long profileI, VerificationStage currentStage);

    Page<VerificationProcess> findByCurrentStage(
            VerificationStage stage, Pageable pageable);

    Page<VerificationProcess> findByCurrentStageNotAndCompletedAtIsNull(
            VerificationStage stage, Pageable pageable);

    List<VerificationProcess> findByProfileId(Long profileId);
}
