package com.cherrypick.app.domain.auction.repository;

import com.cherrypick.app.domain.auction.entity.AuctionBookmark;
import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionBookmarkRepository extends JpaRepository<AuctionBookmark, Long> {

    /**
     * 사용자가 특정 경매를 북마크했는지 확인
     */
    boolean existsByAuctionAndUser(Auction auction, User user);

    /**
     * 사용자의 특정 경매 북마크 조회
     */
    Optional<AuctionBookmark> findByAuctionAndUser(Auction auction, User user);

    /**
     * 특정 경매의 총 북마크 수 조회
     */
    long countByAuction(Auction auction);

    /**
     * 사용자의 모든 북마크된 경매 조회
     */
    @Query("SELECT ab.auction FROM AuctionBookmark ab WHERE ab.user = :user ORDER BY ab.createdAt DESC")
    List<Auction> findBookmarkedAuctionsByUser(@Param("user") User user);

    /**
     * 특정 경매를 북마크한 모든 사용자 조회
     */
    List<AuctionBookmark> findByAuctionOrderByCreatedAtDesc(Auction auction);

    /**
     * 사용자의 북마크 삭제
     */
    void deleteByAuctionAndUser(Auction auction, User user);
}