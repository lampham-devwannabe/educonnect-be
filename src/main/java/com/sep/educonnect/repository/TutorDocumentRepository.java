package com.sep.educonnect.repository;

import com.sep.educonnect.entity.TutorDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TutorDocumentRepository extends JpaRepository<TutorDocument, Long> {
    List<TutorDocument> findByProfileUserUserId(String profileId);
    Optional<TutorDocument> findByProfileUserUserIdAndId(String profileId, Long Id);

}
