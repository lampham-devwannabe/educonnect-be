package com.sep.educonnect.repository;

import com.sep.educonnect.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    @Query("SELECT n FROM Notification n WHERE n.user.userId = :userId ORDER BY n.timestamp DESC")
    Page<Notification> findByUserIdOrderByTimestampDesc(@Param("userId") String userId, Pageable pageable);
    
    @Query("SELECT n FROM Notification n WHERE n.user.userId = :userId AND n.read = false ORDER BY n.timestamp DESC")
    List<Notification> findUnreadByUserId(@Param("userId") String userId);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.userId = :userId AND n.read = false")
    Long countUnreadByUserId(@Param("userId") String userId);
    
    @Query("SELECT n FROM Notification n WHERE n.user.userId = :userId AND n.actionLink LIKE :actionLinkPattern ORDER BY n.timestamp DESC")
    List<Notification> findByUserIdAndActionLinkPattern(@Param("userId") String userId, @Param("actionLinkPattern") String actionLinkPattern);
}

