package com.cherrypick.app.domain.notification.repository;

import com.cherrypick.app.domain.notification.entity.NotificationHistory;
import com.cherrypick.app.domain.notification.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 알림 히스토리 리포지토리
 */
@Repository
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {

    /**
     * 사용자별 알림 목록 조회 (최신순)
     */
    Page<NotificationHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 사용자별 읽지 않은 알림 개수
     */
    long countByUserIdAndIsReadFalse(Long userId);
    
    /**
     * 특정 타입의 알림 목록 조회
     */
    Page<NotificationHistory> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, NotificationType type, Pageable pageable);
    
    /**
     * 사용자의 모든 알림을 읽음 처리
     */
    @Modifying
    @Query("UPDATE NotificationHistory n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);
    
    /**
     * 특정 리소스 관련 알림 조회
     */
    Page<NotificationHistory> findByUserIdAndResourceIdOrderByCreatedAtDesc(Long userId, Long resourceId, Pageable pageable);
}