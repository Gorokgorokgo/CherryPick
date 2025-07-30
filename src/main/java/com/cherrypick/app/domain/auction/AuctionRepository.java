package com.cherrypick.app.domain.auction;

import com.cherrypick.app.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {
    
    // 진행 중인 경매 조회
    Page<Auction> findByStatusOrderByCreatedAtDesc(AuctionStatus status, Pageable pageable);
    
    // 카테고리별 경매 조회
    Page<Auction> findByStatusAndCategoryOrderByCreatedAtDesc(AuctionStatus status, Category category, Pageable pageable);
    
    // 카테고리별 활성 경매 조회 (Service에서 사용)
    Page<Auction> findByCategoryAndStatusOrderByCreatedAtDesc(Category category, AuctionStatus status, Pageable pageable);
    
    // 지역별 경매 조회
    Page<Auction> findByRegionScopeAndStatusOrderByCreatedAtDesc(RegionScope regionScope, AuctionStatus status, Pageable pageable);
    
    Page<Auction> findByRegionScopeAndRegionCodeAndStatusOrderByCreatedAtDesc(RegionScope regionScope, String regionCode, AuctionStatus status, Pageable pageable);
    
    // 사용자 ID로 경매 조회
    Page<Auction> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);
    
    // 사용자의 판매 경매 조회
    Page<Auction> findBySellerOrderByCreatedAtDesc(User seller, Pageable pageable);
    
    // TODO: 낙찰 기능 구현 후 추가 예정
    // Page<Auction> findByWinnerOrderByCreatedAtDesc(User winner, Pageable pageable);
    
    // 종료 시간이 지난 진행 중인 경매 조회
    @Query("SELECT a FROM Auction a WHERE a.status = 'ACTIVE' AND a.endAt < :now")
    List<Auction> findExpiredActiveAuctions(@Param("now") LocalDateTime now);
    
    // 제목으로 검색
    @Query("SELECT a FROM Auction a WHERE a.status = 'ACTIVE' AND a.title LIKE %:keyword%")
    Page<Auction> findByTitleContaining(@Param("keyword") String keyword, Pageable pageable);
}