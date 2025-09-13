package com.cherrypick.app.domain.bid.repository;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.bid.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    
    // 경매의 최고 입찰 조회
    Optional<Bid> findTopByAuctionOrderByBidAmountDesc(Auction auction);
    
    // 경매 ID로 최고 입찰 조회
    Optional<Bid> findTopByAuctionIdOrderByBidAmountDesc(Long auctionId);
    
    // 경매 ID로 최근 입찰 조회
    Optional<Bid> findTopByAuctionIdOrderByBidTimeDesc(Long auctionId);
    
    // 경매의 모든 입찰 조회 (금액 순)
    List<Bid> findByAuctionOrderByBidAmountDesc(Auction auction);
    
    // 경매 ID로 입찰 내역 조회 (금액 순, 페이지네이션) - 자동입찰 설정(금액 0) 제외
    @Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId AND b.bidAmount > 0 ORDER BY b.bidAmount DESC")
    Page<Bid> findByAuctionIdOrderByBidAmountDesc(@Param("auctionId") Long auctionId, Pageable pageable);
    
    // 사용자의 입찰 내역 조회
    Page<Bid> findByBidderOrderByCreatedAtDesc(User bidder, Pageable pageable);
    
    // 사용자 ID로 입찰 내역 조회 (시간 순, 페이지네이션)
    Page<Bid> findByBidderIdOrderByBidTimeDesc(Long bidderId, Pageable pageable);
    
    // 사용자의 특정 경매 입찰 조회
    Optional<Bid> findByAuctionAndBidder(Auction auction, User bidder);
    
    // 경매별 입찰 수 카운트
    @Query("SELECT COUNT(b) FROM Bid b WHERE b.auction = :auction")
    Integer countByAuction(@Param("auction") Auction auction);
    
    // 경매의 최고 입찰 금액 조회
    @Query("SELECT MAX(b.bidAmount) FROM Bid b WHERE b.auction.id = :auctionId")
    BigDecimal findHighestBidAmountByAuctionId(@Param("auctionId") Long auctionId);
    
    // 사용자가 현재 최고가로 입찰 중인 경매들
    @Query("SELECT b FROM Bid b WHERE b.bidder = :bidder AND b.bidAmount = " +
           "(SELECT MAX(b2.bidAmount) FROM Bid b2 WHERE b2.auction = b.auction)")
    List<Bid> findWinningBidsByBidder(@Param("bidder") User bidder);
    
    // 자동입찰 관련 메서드들
    
    // 특정 경매의 활성 자동입찰 조회 (각 사용자별 최신 자동입찰 설정)
    @Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId AND b.isAutoBid = true AND b.status = 'ACTIVE' " +
           "AND b.id = (SELECT MAX(b2.id) FROM Bid b2 WHERE b2.auction.id = :auctionId AND b2.bidder.id = b.bidder.id AND b2.isAutoBid = true AND b2.status = 'ACTIVE') " +
           "ORDER BY b.maxAutoBidAmount DESC")
    List<Bid> findActiveAutoBidsByAuctionId(@Param("auctionId") Long auctionId);
    
    // 사용자의 활성 자동입찰 조회
    @Query("SELECT b FROM Bid b WHERE b.bidder.id = :bidderId AND b.isAutoBid = true AND b.status = 'ACTIVE'")
    List<Bid> findActiveAutoBidsByBidderId(@Param("bidderId") Long bidderId);
    
    // 특정 경매에서 특정 입찰자의 현재 자동입찰 조회
    @Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId AND b.bidder.id = :bidderId " +
           "AND b.isAutoBid = true AND b.status = 'ACTIVE'")
    Optional<Bid> findActiveAutoBidByAuctionIdAndBidderId(@Param("auctionId") Long auctionId, 
                                                          @Param("bidderId") Long bidderId);
    
    // 특정 경매에서 특정 입찰자의 최신 입찰 조회 (시간순) - Spring Data JPA naming convention 사용
    Optional<Bid> findFirstByAuctionIdAndBidderIdOrderByBidTimeDesc(Long auctionId, Long bidderId);
}