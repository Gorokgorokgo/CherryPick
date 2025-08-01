package com.cherrypick.app.domain.transaction.repository;

import com.cherrypick.app.domain.transaction.entity.Transaction;
import com.cherrypick.app.domain.transaction.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * 경매 ID로 거래 조회
     */
    Optional<Transaction> findByAuctionId(Long auctionId);

    /**
     * 판매자별 거래 목록 조회
     */
    Page<Transaction> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);

    /**
     * 구매자별 거래 목록 조회
     */
    Page<Transaction> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);

    /**
     * 상태별 거래 조회
     */
    List<Transaction> findByStatus(TransactionStatus status);

    /**
     * 사용자별 거래 조회 (판매자 또는 구매자)
     */
    @Query("SELECT t FROM Transaction t WHERE (t.seller.id = :userId OR t.buyer.id = :userId) ORDER BY t.createdAt DESC")
    Page<Transaction> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * 완료된 거래 중 특정 기간 수수료 합계 (수익 분석용)
     */
    @Query("SELECT SUM(t.commissionFee) FROM Transaction t WHERE t.status = 'COMPLETED' AND t.completedAt BETWEEN :startDate AND :endDate")
    Optional<Long> getCommissionSumByPeriod(@Param("startDate") java.time.LocalDateTime startDate, 
                                           @Param("endDate") java.time.LocalDateTime endDate);
}