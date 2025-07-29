package com.cherrypick.app.repository;

import com.cherrypick.app.entity.Auction;
import com.cherrypick.app.entity.User;
import com.cherrypick.app.entity.enums.AuctionStatus;
import com.cherrypick.app.entity.enums.Category;
import com.cherrypick.app.entity.enums.RegionScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 경매 Repository
 */
@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {

    /**
     * 진행 중인 경매 목록 조회 (페이지네이션)
     */
    Page<Auction> findByStatusOrderByCreatedAtDesc(AuctionStatus status, Pageable pageable);

    /**
     * 카테고리별 진행 중인 경매 조회
     */
    Page<Auction> findByStatusAndCategoryOrderByCreatedAtDesc(AuctionStatus status, Category category, Pageable pageable);

    /**
     * 지역별 진행 중인 경매 조회
     */
    @Query("SELECT a FROM Auction a WHERE a.status = :status AND " +
           "(a.region LIKE %:region% OR a.regionScope = :regionScope) " +
           "ORDER BY a.createdAt DESC")
    Page<Auction> findByStatusAndRegion(@Param("status") AuctionStatus status,
                                        @Param("region") String region,
                                        @Param("regionScope") RegionScope regionScope,
                                        Pageable pageable);

    /**
     * 가격 범위별 진행 중인 경매 조회
     */
    @Query("SELECT a FROM Auction a WHERE a.status = :status AND " +
           "a.currentPrice BETWEEN :minPrice AND :maxPrice " +
           "ORDER BY a.createdAt DESC")
    Page<Auction> findByStatusAndPriceRange(@Param("status") AuctionStatus status,
                                            @Param("minPrice") BigDecimal minPrice,
                                            @Param("maxPrice") BigDecimal maxPrice,
                                            Pageable pageable);

    /**
     * 복합 검색 (제목, 설명 검색)
     */
    @Query("SELECT a FROM Auction a WHERE a.status = :status AND " +
           "(LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY a.createdAt DESC")
    Page<Auction> findByStatusAndKeyword(@Param("status") AuctionStatus status,
                                         @Param("keyword") String keyword,
                                         Pageable pageable);

    /**
     * 판매자별 경매 목록 조회
     */
    Page<Auction> findBySellerOrderByCreatedAtDesc(User seller, Pageable pageable);

    /**
     * 판매자별 특정 상태 경매 조회
     */
    Page<Auction> findBySellerAndStatusOrderByCreatedAtDesc(User seller, AuctionStatus status, Pageable pageable);

    /**
     * 종료 임박 경매 조회 (1시간 이내)
     */
    @Query("SELECT a FROM Auction a WHERE a.status = :status AND " +
           "a.endAt BETWEEN :now AND :oneHourLater " +
           "ORDER BY a.endAt ASC")
    List<Auction> findEndingSoonAuctions(@Param("status") AuctionStatus status,
                                         @Param("now") LocalDateTime now,
                                         @Param("oneHourLater") LocalDateTime oneHourLater);

    /**
     * 만료된 경매 조회
     */
    @Query("SELECT a FROM Auction a WHERE a.status = :status AND a.endAt < :now")
    List<Auction> findExpiredAuctions(@Param("status") AuctionStatus status,
                                      @Param("now") LocalDateTime now);

    /**
     * 인기 경매 조회 (입찰 수 기준)
     */
    @Query("SELECT a FROM Auction a WHERE a.status = :status " +
           "ORDER BY a.bidCount DESC, a.createdAt DESC")
    Page<Auction> findPopularAuctions(@Param("status") AuctionStatus status, Pageable pageable);

    /**
     * 최근 낙찰 경매 조회
     */
    @Query("SELECT a FROM Auction a WHERE a.status = :status AND a.winningBidder IS NOT NULL " +
           "ORDER BY a.updatedAt DESC")
    Page<Auction> findRecentCompletedAuctions(@Param("status") AuctionStatus status, Pageable pageable);

    /**
     * 지역 확장 대상 경매 조회 (입찰 수가 적은 경매)
     */
    @Query("SELECT a FROM Auction a WHERE a.status = :status AND " +
           "a.bidCount < :minBidCount AND " +
           "a.endAt > :now AND " +
           "a.regionScope != :maxRegionScope " +
           "ORDER BY a.createdAt ASC")
    List<Auction> findAuctionsForRegionExpansion(@Param("status") AuctionStatus status,
                                                  @Param("minBidCount") Integer minBidCount,
                                                  @Param("now") LocalDateTime now,
                                                  @Param("maxRegionScope") RegionScope maxRegionScope);

    /**
     * 카테고리별 평균 낙찰가 조회
     */
    @Query("SELECT a.category, AVG(a.currentPrice) FROM Auction a " +
           "WHERE a.status = :status AND a.winningBidder IS NOT NULL " +
           "GROUP BY a.category")
    List<Object[]> findAveragePriceByCategory(@Param("status") AuctionStatus status);

    /**
     * 사용자의 낙찰한 경매 조회
     */
    Page<Auction> findByWinningBidderOrderByUpdatedAtDesc(User winningBidder, Pageable pageable);
}