package com.cherrypick.app.domain.point.repository;

import com.cherrypick.app.domain.user.User;
import com.cherrypick.app.domain.point.entity.PointTransaction;
import com.cherrypick.app.domain.point.enums.PointTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
    
    // 사용자의 포인트 거래 내역 조회
    Page<PointTransaction> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    // 사용자 ID로 거래 내역 조회 (PointService에서 사용)
    Page<PointTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    // 사용자의 특정 타입 거래 내역 조회
    Page<PointTransaction> findByUserAndTypeOrderByCreatedAtDesc(User user, PointTransactionType type, Pageable pageable);
    
    // 특정 기간의 거래 내역 조회
    @Query("SELECT pt FROM PointTransaction pt WHERE pt.user = :user AND pt.createdAt BETWEEN :startDate AND :endDate ORDER BY pt.createdAt DESC")
    List<PointTransaction> findByUserAndDateRange(@Param("user") User user,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);
    
    // 사용자의 총 충전 금액
    @Query("SELECT COALESCE(SUM(pt.amount), 0) FROM PointTransaction pt WHERE pt.user = :user AND pt.type = 'CHARGE'")
    Long getTotalChargedAmount(@Param("user") User user);
    
    // 사용자의 총 사용 금액
    @Query("SELECT COALESCE(SUM(pt.amount), 0) FROM PointTransaction pt WHERE pt.user = :user AND pt.type = 'USE'")
    Long getTotalUsedAmount(@Param("user") User user);
}