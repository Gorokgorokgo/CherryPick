package com.cherrypick.app.domain.user.repository;

import com.cherrypick.app.domain.user.entity.ExperienceHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 경험치 히스토리 리포지토리
 * - Fetch Join 활용으로 N+1 문제 방지
 * - 인덱스 기반 효율적인 조회
 */
@Repository
public interface ExperienceHistoryRepository extends JpaRepository<ExperienceHistory, Long> {

    /**
     * 사용자별 최근 경험치 히스토리 조회 (Fetch Join)
     */
    @Query("SELECT eh FROM ExperienceHistory eh " +
           "JOIN FETCH eh.user " +
           "WHERE eh.user.id = :userId " +
           "ORDER BY eh.createdAt DESC")
    Page<ExperienceHistory> findByUserIdWithUser(@Param("userId") Long userId, Pageable pageable);

    /**
     * 사용자별 타입별 최근 경험치 히스토리 조회
     */
    @Query("SELECT eh FROM ExperienceHistory eh " +
           "WHERE eh.user.id = :userId " +
           "AND eh.type = :type " +
           "ORDER BY eh.createdAt DESC")
    List<ExperienceHistory> findByUserIdAndType(
        @Param("userId") Long userId,
        @Param("type") ExperienceHistory.ExperienceType type,
        Pageable pageable
    );

    /**
     * 사용자별 레벨업 히스토리 조회
     */
    @Query("SELECT eh FROM ExperienceHistory eh " +
           "WHERE eh.user.id = :userId " +
           "AND eh.isLevelUp = true " +
           "ORDER BY eh.createdAt DESC")
    List<ExperienceHistory> findLevelUpHistoryByUserId(@Param("userId") Long userId);

    /**
     * 최근 알림 미발송 이벤트 조회 (배치 알림 전송용)
     */
    @Query("SELECT eh FROM ExperienceHistory eh " +
           "JOIN FETCH eh.user " +
           "WHERE eh.notificationSent = false " +
           "AND eh.createdAt > :since " +
           "ORDER BY eh.createdAt ASC")
    List<ExperienceHistory> findUnsentNotifications(@Param("since") LocalDateTime since);

    /**
     * 사용자별 기간별 총 획득 경험치
     */
    @Query("SELECT COALESCE(SUM(eh.expGained), 0) FROM ExperienceHistory eh " +
           "WHERE eh.user.id = :userId " +
           "AND eh.type = :type " +
           "AND eh.createdAt BETWEEN :startDate AND :endDate")
    Integer sumExpGainedByUserAndTypeAndPeriod(
        @Param("userId") Long userId,
        @Param("type") ExperienceHistory.ExperienceType type,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 사용자별 특정 사유 경험치 획득 횟수 (조작 방지 검증용)
     */
    @Query("SELECT COUNT(eh) FROM ExperienceHistory eh " +
           "WHERE eh.user.id = :userId " +
           "AND eh.reason = :reason " +
           "AND eh.createdAt > :since")
    Long countByUserIdAndReasonSince(
        @Param("userId") Long userId,
        @Param("reason") String reason,
        @Param("since") LocalDateTime since
    );
}
