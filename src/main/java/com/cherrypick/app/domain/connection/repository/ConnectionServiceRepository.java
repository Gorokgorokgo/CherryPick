package com.cherrypick.app.domain.connection.repository;

import com.cherrypick.app.domain.connection.entity.ConnectionService;
import com.cherrypick.app.domain.connection.enums.ConnectionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectionServiceRepository extends JpaRepository<ConnectionService, Long> {
    
    /**
     * 판매자별 연결 서비스 목록 조회 (최신순)
     */
    Page<ConnectionService> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);
    
    /**
     * 구매자별 연결 서비스 목록 조회 (최신순)
     */
    Page<ConnectionService> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);
    
    /**
     * 특정 경매의 연결 서비스 조회
     */
    Optional<ConnectionService> findByAuctionId(Long auctionId);
    
    /**
     * 상태별 연결 서비스 목록 조회
     */
    List<ConnectionService> findByStatus(ConnectionStatus status);
    
    /**
     * 판매자별 특정 상태의 연결 서비스 개수
     */
    @Query("SELECT COUNT(cs) FROM ConnectionService cs WHERE cs.seller.id = :sellerId AND cs.status = :status")
    Long countBySellerIdAndStatus(@Param("sellerId") Long sellerId, @Param("status") ConnectionStatus status);
    
    /**
     * 구매자별 특정 상태의 연결 서비스 개수
     */
    @Query("SELECT COUNT(cs) FROM ConnectionService cs WHERE cs.buyer.id = :buyerId AND cs.status = :status")
    Long countByBuyerIdAndStatus(@Param("buyerId") Long buyerId, @Param("status") ConnectionStatus status);
    
    /**
     * 판매자의 총 수수료 수입 조회
     */
    @Query("SELECT COALESCE(SUM(cs.connectionFee), 0) FROM ConnectionService cs WHERE cs.seller.id = :sellerId AND cs.status = 'COMPLETED'")
    Long getTotalRevenueBySellerIdAndCompleted(@Param("sellerId") Long sellerId);
}