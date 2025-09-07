package com.cherrypick.app.domain.auction.repository;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
    
    // === 고급 검색 메소드들 ===
    
    // 키워드 검색 (제목 + 설명)
    @Query("SELECT a FROM Auction a WHERE a.status = :status " +
           "AND (LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Auction> searchByKeyword(@Param("keyword") String keyword, @Param("status") AuctionStatus status, Pageable pageable);
    
    // 가격 범위로 검색
    @Query("SELECT a FROM Auction a WHERE a.status = :status " +
           "AND a.currentPrice >= :minPrice AND a.currentPrice <= :maxPrice")
    Page<Auction> findByPriceRange(@Param("minPrice") BigDecimal minPrice, 
                                  @Param("maxPrice") BigDecimal maxPrice, 
                                  @Param("status") AuctionStatus status, 
                                  Pageable pageable);
    
    // 마감 임박 경매 검색 (N시간 이내)
    @Query("SELECT a FROM Auction a WHERE a.status = 'ACTIVE' " +
           "AND a.endAt BETWEEN :now AND :endTime " +
           "ORDER BY a.endAt ASC")
    Page<Auction> findEndingSoon(@Param("now") LocalDateTime now, 
                                @Param("endTime") LocalDateTime endTime, 
                                Pageable pageable);
    
    // 입찰 수가 N개 이상인 경매
    @Query("SELECT a FROM Auction a WHERE a.status = :status AND a.bidCount >= :minBidCount")
    Page<Auction> findByMinBidCount(@Param("minBidCount") Integer minBidCount, 
                                   @Param("status") AuctionStatus status, 
                                   Pageable pageable);
    
    // 복합 검색 (키워드 + 카테고리 + 지역 + 가격범위)
    @Query("SELECT a FROM Auction a WHERE " +
           "(:status IS NULL OR a.status = :status) " +
           "AND (:keyword IS NULL OR " +
           "     LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:category IS NULL OR a.category = :category) " +
           "AND (:regionScope IS NULL OR a.regionScope = :regionScope) " +
           "AND (:regionCode IS NULL OR a.regionCode = :regionCode) " +
           "AND (:minPrice IS NULL OR a.currentPrice >= :minPrice) " +
           "AND (:maxPrice IS NULL OR a.currentPrice <= :maxPrice) " +
           "AND (:minBidCount IS NULL OR a.bidCount >= :minBidCount) " +
           "AND (:endingSoonTime IS NULL OR a.endAt <= :endingSoonTime)")
    Page<Auction> searchAuctions(@Param("status") AuctionStatus status,
                                @Param("keyword") String keyword,
                                @Param("category") Category category,
                                @Param("regionScope") RegionScope regionScope,
                                @Param("regionCode") String regionCode,
                                @Param("minPrice") BigDecimal minPrice,
                                @Param("maxPrice") BigDecimal maxPrice,
                                @Param("minBidCount") Integer minBidCount,
                                @Param("endingSoonTime") LocalDateTime endingSoonTime,
                                Pageable pageable);
    
    // 조회수 높은 순으로 정렬
    Page<Auction> findByStatusOrderByViewCountDesc(AuctionStatus status, Pageable pageable);
    
    // 입찰 수 높은 순으로 정렬
    Page<Auction> findByStatusOrderByBidCountDesc(AuctionStatus status, Pageable pageable);
    
    // 현재 가격 낮은 순으로 정렬
    Page<Auction> findByStatusOrderByCurrentPriceAsc(AuctionStatus status, Pageable pageable);
    
    // 현재 가격 높은 순으로 정렬
    Page<Auction> findByStatusOrderByCurrentPriceDesc(AuctionStatus status, Pageable pageable);
    
    // 마감 임박 순으로 정렬 (진행중인 경매만)
    @Query("SELECT a FROM Auction a WHERE a.status = 'ACTIVE' ORDER BY a.endAt ASC")
    Page<Auction> findActiveAuctionsOrderByEndingSoon(Pageable pageable);
    
    // 경매 강제 종료 (테스트용)
    @Modifying
    @Query("UPDATE Auction a SET a.endAt = :endTime WHERE a.id = :auctionId")
    void updateAuctionEndTime(@Param("auctionId") Long auctionId, @Param("endTime") LocalDateTime endTime);
}