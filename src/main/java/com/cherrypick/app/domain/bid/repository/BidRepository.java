package com.cherrypick.app.domain.bid.repository;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.bid.enums.BidStatus;
import com.cherrypick.app.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {

    /**
     * 경매의 최고 입찰 조회
     */
    Optional<Bid> findTopByAuctionIdAndBidAmountGreaterThanOrderByBidAmountDesc(Long auctionId, BigDecimal minAmount);

    /**
     * 경매별 입찰 내역 조회 (금액 높은 순)
     */
    Page<Bid> findByAuctionIdAndBidAmountGreaterThanOrderByBidAmountDesc(Long auctionId, BigDecimal minAmount, Pageable pageable);

    /**
     * 사용자의 입찰 내역 조회
     */
    Page<Bid> findByBidderIdOrderByBidTimeDesc(Long bidderId, Pageable pageable);

    /**
     * 특정 경매에서 사용자의 활성 자동입찰 설정 조회
     */
    @Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId " +
           "AND b.bidder.id = :bidderId " +
           "AND b.isAutoBid = true " +
           "AND b.bidAmount = 0 " +
           "AND b.status = :status")
    Optional<Bid> findByAuctionIdAndBidderIdAndIsAutoBidTrueAndStatus(
            @Param("auctionId") Long auctionId,
            @Param("bidderId") Long bidderId,
            @Param("status") BidStatus status);

    /**
     * 특정 경매의 모든 활성 자동입찰 설정 조회 (본인 제외)
     */
    @Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId " +
           "AND b.isAutoBid = true " +
           "AND b.status = :status " +
           "AND b.bidAmount = 0 " +
           "AND b.bidder.id != :excludeBidderId")
    List<Bid> findActiveAutoBidSettingsExcludingBidder(
            @Param("auctionId") Long auctionId,
            @Param("excludeBidderId") Long excludeBidderId,
            @Param("status") BidStatus status
    );

    /**
     * 경매의 현재 최고 입찰가 조회
     */
    @Query("SELECT MAX(b.bidAmount) FROM Bid b WHERE b.auction.id = :auctionId AND b.bidAmount > 0")
    Optional<BigDecimal> findMaxBidAmountByAuctionId(@Param("auctionId") Long auctionId);

    /**
     * 특정 경매에서 특정 사용자가 최고 입찰자인지 확인
     * 동일 금액일 경우 가장 먼저 입찰한 사람이 최고 입찰자
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Bid b " +
           "WHERE b.auction.id = :auctionId " +
           "AND b.bidder.id = :bidderId " +
           "AND b.bidAmount > 0 " +
           "AND b.id = (SELECT b2.id FROM Bid b2 WHERE b2.auction.id = :auctionId AND b2.bidAmount > 0 " +
           "ORDER BY b2.bidAmount DESC, b2.bidTime ASC LIMIT 1)")
    boolean isHighestBidder(@Param("auctionId") Long auctionId, @Param("bidderId") Long bidderId);

    /**
     * 경매의 입찰 횟수 카운트
     */
    @Query("SELECT COUNT(b) FROM Bid b WHERE b.auction.id = :auctionId AND b.bidAmount > 0")
    long countByAuctionId(@Param("auctionId") Long auctionId);

    /**
     * 특정 경매의 모든 입찰 (디버깅용)
     */
    List<Bid> findByAuctionOrderByBidAmountDesc(Auction auction);

    /**
     * 최고 입찰 조회 (기존 호환성)
     */
    Optional<Bid> findTopByAuctionIdOrderByBidAmountDesc(Long auctionId);

    /**
     * 사용자의 활성 자동입찰 설정 목록 조회
     */
    @Query("SELECT b FROM Bid b WHERE b.bidder.id = :bidderId " +
           "AND b.isAutoBid = true " +
           "AND b.bidAmount = 0 " +
           "AND b.status = 'ACTIVE'")
    List<Bid> findActiveAutoBidsByBidderId(@Param("bidderId") Long bidderId);

    /**
     * 사용자의 모든 자동입찰 설정 목록 조회 (활성 + 종료)
     */
    @Query("SELECT b FROM Bid b WHERE b.bidder.id = :bidderId " +
           "AND b.isAutoBid = true " +
           "AND b.bidAmount = 0")
    List<Bid> findAllAutoBidsByBidderId(@Param("bidderId") Long bidderId);
}