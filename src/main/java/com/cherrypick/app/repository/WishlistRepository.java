package com.cherrypick.app.repository;

import com.cherrypick.app.entity.Auction;
import com.cherrypick.app.entity.User;
import com.cherrypick.app.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 찜 목록 Repository
 */
@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    /**
     * 사용자의 활성 찜 목록 조회
     */
    Page<Wishlist> findByUserAndIsActiveTrueOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * 사용자의 모든 찜 목록 조회 (비활성 포함)
     */
    Page<Wishlist> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * 특정 경매의 찜한 사용자들 조회
     */
    List<Wishlist> findByAuctionAndIsActiveTrueOrderByCreatedAtDesc(Auction auction);

    /**
     * 사용자가 특정 경매를 찜했는지 확인
     */
    Optional<Wishlist> findByUserAndAuction(User user, Auction auction);

    /**
     * 사용자가 특정 경매를 활성 찜했는지 확인
     */
    Optional<Wishlist> findByUserAndAuctionAndIsActiveTrue(User user, Auction auction);

    /**
     * 경매별 찜 개수 조회
     */
    @Query("SELECT COUNT(w) FROM Wishlist w WHERE w.auction = :auction AND w.isActive = true")
    Long countActiveWishlistsByAuction(@Param("auction") Auction auction);

    /**
     * 사용자별 활성 찜 개수 조회
     */
    @Query("SELECT COUNT(w) FROM Wishlist w WHERE w.user = :user AND w.isActive = true")
    Long countActiveWishlistsByUser(@Param("user") User user);

    /**
     * 인기 경매 조회 (찜 개수 기준)
     */
    @Query("SELECT w.auction, COUNT(w) as wishCount FROM Wishlist w " +
           "WHERE w.isActive = true AND w.auction.status = 'ACTIVE' " +
           "GROUP BY w.auction ORDER BY wishCount DESC")
    List<Object[]> findPopularAuctionsByWishlist(Pageable pageable);

    /**
     * 카테고리별 찜 통계
     */
    @Query("SELECT w.auction.category, COUNT(w) FROM Wishlist w " +
           "WHERE w.isActive = true " +
           "GROUP BY w.auction.category " +
           "ORDER BY COUNT(w) DESC")
    List<Object[]> getWishlistStatisticsByCategory();

    /**
     * 사용자의 찜한 경매 중 진행 중인 것들 조회
     */
    @Query("SELECT w FROM Wishlist w WHERE w.user = :user AND w.isActive = true " +
           "AND w.auction.status = 'ACTIVE' " +
           "ORDER BY w.auction.endAt ASC")
    List<Wishlist> findActiveAuctionWishlists(@Param("user") User user);

    /**
     * 사용자의 찜한 경매 중 종료 임박한 것들 조회
     */
    @Query("SELECT w FROM Wishlist w WHERE w.user = :user AND w.isActive = true " +
           "AND w.auction.status = 'ACTIVE' " +
           "AND w.auction.endAt BETWEEN :now AND :oneHourLater " +
           "ORDER BY w.auction.endAt ASC")
    List<Wishlist> findEndingSoonWishlists(@Param("user") User user,
                                           @Param("now") java.time.LocalDateTime now,
                                           @Param("oneHourLater") java.time.LocalDateTime oneHourLater);

    /**
     * 비활성화된 찜 목록 정리 (30일 이상 된 것들)
     */
    @Query("SELECT w FROM Wishlist w WHERE w.isActive = false " +
           "AND w.updatedAt < :cutoffDate")
    List<Wishlist> findOldInactiveWishlists(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);

    /**
     * 사용자의 찜 목록 통계 조회
     */
    @Query("SELECT COUNT(w), " +
           "COUNT(CASE WHEN w.auction.status = 'ACTIVE' THEN 1 END), " +
           "COUNT(CASE WHEN w.auction.status = 'COMPLETED' THEN 1 END) " +
           "FROM Wishlist w WHERE w.user = :user AND w.isActive = true")
    Object[] getWishlistStatisticsByUser(@Param("user") User user);
}