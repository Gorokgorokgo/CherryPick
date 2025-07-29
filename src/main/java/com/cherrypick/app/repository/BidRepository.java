package com.cherrypick.app.repository;

import com.cherrypick.app.entity.Auction;
import com.cherrypick.app.entity.Bid;
import com.cherrypick.app.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 입찰 Repository
 */
@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    /**
     * 경매별 입찰 내역 조회 (최신순)
     */
    Page<Bid> findByAuctionOrderByBidTimeDesc(Auction auction, Pageable pageable);

    /**
     * 경매별 입찰 내역 조회 (가격순)
     */
    List<Bid> findByAuctionOrderByBidAmountDesc(Auction auction);

    /**
     * 경매의 최고 입찰 조회
     */
    Optional<Bid> findTopByAuctionOrderByBidAmountDesc(Auction auction);

    /**
     * 경매의 최신 입찰 조회
     */
    Optional<Bid> findTopByAuctionOrderByBidTimeDesc(Auction auction);

    /**
     * 사용자별 입찰 내역 조회
     */
    Page<Bid> findByBidderOrderByBidTimeDesc(User bidder, Pageable pageable);

    /**
     * 사용자의 특정 경매 입찰 내역 조회
     */
    List<Bid> findByBidderAndAuctionOrderByBidTimeDesc(User bidder, Auction auction);

    /**
     * 사용자의 최고 입찰가 조회 (경매별)
     */
    @Query("SELECT b FROM Bid b WHERE b.bidder = :bidder AND b.auction = :auction " +
           "ORDER BY b.bidAmount DESC")
    Optional<Bid> findTopBidByBidderAndAuction(@Param("bidder") User bidder, 
                                               @Param("auction") Auction auction);

    /**
     * 특정 가격 이상의 입찰 수 조회
     */
    @Query("SELECT COUNT(b) FROM Bid b WHERE b.auction = :auction AND b.bidAmount >= :amount")
    Long countBidsAboveAmount(@Param("auction") Auction auction, @Param("amount") BigDecimal amount);

    /**
     * 경매별 고유 입찰자 수 조회
     */
    @Query("SELECT COUNT(DISTINCT b.bidder) FROM Bid b WHERE b.auction = :auction")
    Long countUniqueBidders(@Param("auction") Auction auction);

    /**
     * 자동 입찰 설정된 입찰 조회
     */
    List<Bid> findByAuctionAndIsAutoBidTrueOrderByMaxAutoBidAmountDesc(Auction auction);

    /**
     * 사용자의 자동 입찰 설정 조회
     */
    @Query("SELECT b FROM Bid b WHERE b.bidder = :bidder AND b.isAutoBid = true " +
           "AND b.auction.status = 'ACTIVE' " +
           "ORDER BY b.bidTime DESC")
    List<Bid> findActiveAutoBidsByBidder(@Param("bidder") User bidder);

    /**
     * 낙찰 입찰 조회
     */
    List<Bid> findByIsWinningTrue();

    /**
     * 사용자의 낙찰 입찰 조회
     */
    Page<Bid> findByBidderAndIsWinningTrueOrderByBidTimeDesc(User bidder, Pageable pageable);

    /**
     * 경매의 입찰 통계 조회
     */
    @Query("SELECT MIN(b.bidAmount), MAX(b.bidAmount), AVG(b.bidAmount), COUNT(b) " +
           "FROM Bid b WHERE b.auction = :auction")
    Object[] getAuctionBidStatistics(@Param("auction") Auction auction);

    /**
     * 시간대별 입찰 현황 조회 (마지막 24시간)
     */
    @Query("SELECT HOUR(b.bidTime), COUNT(b) FROM Bid b " +
           "WHERE b.auction = :auction AND b.bidTime >= :since " +
           "GROUP BY HOUR(b.bidTime) ORDER BY HOUR(b.bidTime)")
    List<Object[]> getBidCountByHour(@Param("auction") Auction auction, 
                                     @Param("since") java.time.LocalDateTime since);

    /**
     * 스나이핑 입찰 조회 (마지막 10분 이내)
     */
    @Query("SELECT b FROM Bid b WHERE b.auction = :auction " +
           "AND b.bidTime >= :tenMinutesBeforeEnd " +
           "ORDER BY b.bidTime DESC")
    List<Bid> findSnipingBids(@Param("auction") Auction auction, 
                              @Param("tenMinutesBeforeEnd") java.time.LocalDateTime tenMinutesBeforeEnd);

    /**
     * 사용자의 평균 입찰 금액 조회
     */
    @Query("SELECT AVG(b.bidAmount) FROM Bid b WHERE b.bidder = :bidder")
    Optional<BigDecimal> getAverageBidAmountByBidder(@Param("bidder") User bidder);

    /**
     * 경매에서 특정 사용자보다 높은 입찰 조회
     */
    @Query("SELECT b FROM Bid b WHERE b.auction = :auction AND b.bidAmount > :amount " +
           "ORDER BY b.bidAmount ASC")
    List<Bid> findBidsAboveAmount(@Param("auction") Auction auction, @Param("amount") BigDecimal amount);
}