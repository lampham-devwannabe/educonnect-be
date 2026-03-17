package com.sep.educonnect.repository;

import com.sep.educonnect.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    @Query("SELECT w FROM Wishlist w WHERE w.user.userId = :userId AND w.course.id = :courseId")
    Optional<Wishlist> findByUserIdAndCourse_Id(String userId, Long courseId);

    @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM Wishlist w WHERE w.user.userId = :userId AND w.course.id = :courseId")
    boolean existsByUserIdAndCourse_Id(String userId, Long courseId);

    @Query("SELECT w FROM Wishlist w WHERE w.user.userId = :userId")
    List<Wishlist> findByUserId(String userId);
}
