package com.sep.educonnect.repository;

import com.sep.educonnect.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends JpaRepository<Tag,Long> {

    @Query(
            """
                    SELECT t FROM Tag t
                    WHERE t.isDeleted = false
                      AND (:name IS NULL OR (LOWER(t.nameVi) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(t.nameEn) LIKE LOWER(CONCAT('%', :name, '%'))))
                    """)
    Page<Tag> searchTags(@Param("name") String name, Pageable pageable);
}
