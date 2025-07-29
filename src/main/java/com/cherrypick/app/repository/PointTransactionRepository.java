package com.cherrypick.app.repository;

import com.cherrypick.app.entity.PointTransaction;
import com.cherrypick.app.entity.User;
import com.cherrypick.app.entity.enums.PointTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 포인트 거래 내역 Repository
 */
@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    /**
     * 사용자별 포인트 거래 내역 조회 (최신순)
     */
    Page<PointTransaction> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * 사용자별 특정 유형 거래 내역 조회
     */
    Page<PointTransaction> findByUserAndTransactionTypeOrderByCreatedAtDesc(
            User user, PointTransactionType transactionType, Pageable pageable);

    /**
     * 사용자의 총 충전 금액 조회
     */
    @Query("SELECT COALESCE(SUM(pt.amount), 0) FROM PointTransaction pt " +
           "WHERE pt.user = :user AND pt.transactionType = :type")
    BigDecimal getTotalAmountByUserAndType(@Param("user") User user, 
                                           @Param("type") PointTransactionType type);

    /**
     * 특정 기간 동안의 거래 내역 조회
     */
    @Query("SELECT pt FROM PointTransaction pt WHERE pt.user = :user " +
           "AND pt.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY pt.createdAt DESC")
    List<PointTransaction> findByUserAndDateRange(@Param("user") User user,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    /**
     * 사용자의 월별 포인트 사용 패턴 조회
     */
    @Query("SELECT YEAR(pt.createdAt), MONTH(pt.createdAt), SUM(pt.amount) " +
           "FROM PointTransaction pt WHERE pt.user = :user " +
           "GROUP BY YEAR(pt.createdAt), MONTH(pt.createdAt) " +
           "ORDER BY YEAR(pt.createdAt) DESC, MONTH(pt.createdAt) DESC")
    List<Object[]> getMonthlyTransactionSummary(@Param("user") User user);

    /**
     * 외부 거래 ID로 거래 조회
     */
    List<PointTransaction> findByExternalTransactionId(String externalTransactionId);

    /**
     * 경매별 포인트 거래 조회
     */
    List<PointTransaction> findByAuctionOrderByCreatedAtDesc(
            com.cherrypick.app.entity.Auction auction);

    /**
     * 최근 거래 내역 조회 (금액별)
     */
    @Query("SELECT pt FROM PointTransaction pt WHERE pt.user = :user " +
           "AND ABS(pt.amount) >= :minAmount " +
           "ORDER BY pt.createdAt DESC")
    List<PointTransaction> findRecentTransactionsByMinAmount(@Param("user") User user,
                                                             @Param("minAmount") BigDecimal minAmount,
                                                             Pageable pageable);

    /**
     * 일일 거래 통계 조회
     */
    @Query("SELECT DATE(pt.createdAt), pt.transactionType, COUNT(pt), SUM(pt.amount) " +
           "FROM PointTransaction pt WHERE pt.createdAt >= :since " +
           "GROUP BY DATE(pt.createdAt), pt.transactionType " +
           "ORDER BY DATE(pt.createdAt) DESC")
    List<Object[]> getDailyTransactionStatistics(@Param("since") LocalDateTime since);

    /**
     * 사용자의 평균 거래 금액 조회
     */
    @Query("SELECT AVG(ABS(pt.amount)) FROM PointTransaction pt " +
           "WHERE pt.user = :user AND pt.transactionType = :type")
    BigDecimal getAverageTransactionAmount(@Param("user") User user, 
                                           @Param("type") PointTransactionType type);

    /**
     * 보류 중인 거래 조회 (외부 시스템 연동용)
     */
    @Query("SELECT pt FROM PointTransaction pt WHERE pt.externalTransactionId IS NOT NULL " +
           "AND pt.createdAt >= :since " +
           "ORDER BY pt.createdAt ASC")
    List<PointTransaction> findPendingExternalTransactions(@Param("since") LocalDateTime since);

    /**
     * 사용자의 거래 유형별 통계
     */
    @Query("SELECT pt.transactionType, COUNT(pt), SUM(pt.amount), AVG(pt.amount) " +
           "FROM PointTransaction pt WHERE pt.user = :user " +
           "GROUP BY pt.transactionType")
    List<Object[]> getTransactionStatisticsByType(@Param("user") User user);
}