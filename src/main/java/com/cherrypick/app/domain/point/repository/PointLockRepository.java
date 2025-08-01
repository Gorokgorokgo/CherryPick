package com.cherrypick.app.domain.point.repository;

import com.cherrypick.app.domain.point.entity.PointLock;
import com.cherrypick.app.domain.point.enums.PointLockStatus;
import com.cherrypick.app.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PointLockRepository extends JpaRepository<PointLock, Long> {
    
    // 사용자의 활성 포인트 잠금 조회
    List<PointLock> findByUserAndStatus(User user, PointLockStatus status);
    
    // 사용자 ID로 활성 포인트 잠금 조회
    List<PointLock> findByUserIdAndStatus(Long userId, PointLockStatus status);
    
    // 경매의 활성 포인트 잠금 조회
    List<PointLock> findByAuctionIdAndStatus(Long auctionId, PointLockStatus status);
    
    // 사용자의 특정 경매 포인트 잠금 조회
    Optional<PointLock> findByUserIdAndAuctionIdAndStatus(Long userId, Long auctionId, PointLockStatus status);
    
    // 사용자의 잠긴 포인트 총액 계산
    @Query("SELECT COALESCE(SUM(pl.lockedAmount), 0) FROM PointLock pl WHERE pl.user.id = :userId AND pl.status = :status")
    Long calculateTotalLockedAmount(@Param("userId") Long userId, @Param("status") PointLockStatus status);
    
    // 경매별 잠긴 포인트 총액 계산
    @Query("SELECT COALESCE(SUM(pl.lockedAmount), 0) FROM PointLock pl WHERE pl.auction.id = :auctionId AND pl.status = :status")
    Long calculateTotalLockedAmountByAuction(@Param("auctionId") Long auctionId, @Param("status") PointLockStatus status);
}