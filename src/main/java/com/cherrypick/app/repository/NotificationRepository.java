package com.cherrypick.app.repository;

import com.cherrypick.app.entity.Notification;
import com.cherrypick.app.entity.User;
import com.cherrypick.app.entity.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 Repository
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 사용자별 알림 목록 조회 (최신순)
     */
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * 사용자별 읽지 않은 알림 목록 조회
     */
    Page<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * 사용자별 읽지 않은 알림 개수 조회
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user = :user AND n.isRead = false")
    Long countUnreadNotificationsByUser(@Param("user") User user);

    /**
     * 사용자별 특정 유형 알림 조회
     */
    Page<Notification> findByUserAndTypeOrderByCreatedAtDesc(User user, NotificationType type, Pageable pageable);

    /**
     * 발송되지 않은 알림 조회
     */
    List<Notification> findByIsSentFalseOrderByCreatedAtAsc();

    /**
     * 특정 경매 관련 알림 조회
     */
    List<Notification> findByAuctionOrderByCreatedAtDesc(com.cherrypick.app.entity.Auction auction);

    /**
     * 사용자의 모든 알림을 읽음 처리
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    void markAllAsReadByUser(@Param("user") User user);

    /**
     * 특정 유형의 알림을 읽음 처리
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user AND n.type = :type AND n.isRead = false")
    void markAsReadByUserAndType(@Param("user") User user, @Param("type") NotificationType type);

    /**
     * 오래된 읽은 알림 삭제 (30일 이상)
     */
    @Query("SELECT n FROM Notification n WHERE n.isRead = true AND n.createdAt < :cutoffDate")
    List<Notification> findOldReadNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 사용자별 알림 유형 통계
     */
    @Query("SELECT n.type, COUNT(n), " +
           "COUNT(CASE WHEN n.isRead = true THEN 1 END), " +
           "COUNT(CASE WHEN n.isRead = false THEN 1 END) " +
           "FROM Notification n WHERE n.user = :user " +
           "GROUP BY n.type")
    List<Object[]> getNotificationStatisticsByUser(@Param("user") User user);

    /**
     * 일별 알림 발송 통계
     */
    @Query("SELECT DATE(n.createdAt), n.type, COUNT(n) " +
           "FROM Notification n WHERE n.createdAt >= :since " +
           "GROUP BY DATE(n.createdAt), n.type " +
           "ORDER BY DATE(n.createdAt) DESC")
    List<Object[]> getDailyNotificationStatistics(@Param("since") LocalDateTime since);

    /**
     * 발송 실패한 알림 조회 (재시도용)
     */
    @Query("SELECT n FROM Notification n WHERE n.isSent = false " +
           "AND n.createdAt < :retryTime " +
           "ORDER BY n.createdAt ASC")
    List<Notification> findFailedNotifications(@Param("retryTime") LocalDateTime retryTime);

    /**
     * 사용자의 최근 알림 조회 (특정 개수)
     */
    List<Notification> findTop10ByUserOrderByCreatedAtDesc(User user);

    /**
     * 시스템 공지 알림 조회 (모든 사용자 대상)
     */
    List<Notification> findByTypeAndCreatedAtAfterOrderByCreatedAtDesc(
            NotificationType type, LocalDateTime since);

    /**
     * 사용자별 특정 기간 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user " +
           "AND n.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY n.createdAt DESC")
    List<Notification> findByUserAndDateRange(@Param("user") User user,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    /**
     * 중복 알림 방지를 위한 최근 동일 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user " +
           "AND n.type = :type AND n.auction = :auction " +
           "AND n.createdAt >= :since " +
           "ORDER BY n.createdAt DESC")
    List<Notification> findRecentSimilarNotifications(@Param("user") User user,
                                                       @Param("type") NotificationType type,
                                                       @Param("auction") com.cherrypick.app.entity.Auction auction,
                                                       @Param("since") LocalDateTime since);
}