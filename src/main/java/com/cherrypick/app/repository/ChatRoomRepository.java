package com.cherrypick.app.repository;

import com.cherrypick.app.entity.Auction;
import com.cherrypick.app.entity.ChatRoom;
import com.cherrypick.app.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 채팅방 Repository
 */
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 사용자의 채팅방 목록 조회 (활성 채팅방만)
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.isActive = true " +
           "AND (cr.buyer = :user OR cr.seller = :user) " +
           "ORDER BY cr.lastMessageAt DESC NULLS LAST, cr.createdAt DESC")
    Page<ChatRoom> findByUserAndIsActiveTrue(@Param("user") User user, Pageable pageable);

    /**
     * 사용자의 모든 채팅방 조회 (비활성 포함)
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.buyer = :user OR cr.seller = :user " +
           "ORDER BY cr.lastMessageAt DESC NULLS LAST, cr.createdAt DESC")
    Page<ChatRoom> findByUser(@Param("user") User user, Pageable pageable);

    /**
     * 특정 경매의 채팅방 조회
     */
    List<ChatRoom> findByAuctionAndIsActiveTrueOrderByCreatedAtDesc(Auction auction);

    /**
     * 경매와 구매자, 판매자로 채팅방 조회
     */
    Optional<ChatRoom> findByAuctionAndBuyerAndSeller(Auction auction, User buyer, User seller);

    /**
     * 사용자의 읽지 않은 메시지가 있는 채팅방 개수
     */
    @Query("SELECT COUNT(cr) FROM ChatRoom cr WHERE cr.isActive = true " +
           "AND ((cr.buyer = :user AND cr.buyerUnreadCount > 0) " +
           "OR (cr.seller = :user AND cr.sellerUnreadCount > 0))")
    Long countUnreadChatRoomsByUser(@Param("user") User user);

    /**
     * 사용자의 총 읽지 않은 메시지 수
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN cr.buyer = :user THEN cr.buyerUnreadCount " +
           "WHEN cr.seller = :user THEN cr.sellerUnreadCount ELSE 0 END), 0) " +
           "FROM ChatRoom cr WHERE cr.isActive = true " +
           "AND (cr.buyer = :user OR cr.seller = :user)")
    Long getTotalUnreadMessagesByUser(@Param("user") User user);

    /**
     * 최근 활동이 있는 채팅방 조회
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.isActive = true " +
           "AND cr.lastMessageAt >= :since " +
           "ORDER BY cr.lastMessageAt DESC")
    List<ChatRoom> findRecentActiveChatRooms(@Param("since") java.time.LocalDateTime since);

    /**
     * 비활성 채팅방 정리 대상 조회 (30일 이상 메시지 없음)
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.isActive = true " +
           "AND (cr.lastMessageAt IS NULL OR cr.lastMessageAt < :cutoffDate)")
    List<ChatRoom> findInactiveChatRooms(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);

    /**
     * 경매별 채팅방 개수 조회
     */
    @Query("SELECT COUNT(cr) FROM ChatRoom cr WHERE cr.auction = :auction AND cr.isActive = true")
    Long countActiveChatRoomsByAuction(@Param("auction") Auction auction);

    /**
     * 사용자별 채팅방 통계
     */
    @Query("SELECT COUNT(cr), " +
           "COUNT(CASE WHEN cr.lastMessageAt >= :recentTime THEN 1 END), " +
           "SUM(CASE WHEN cr.buyer = :user THEN cr.buyerUnreadCount " +
           "       WHEN cr.seller = :user THEN cr.sellerUnreadCount ELSE 0 END) " +
           "FROM ChatRoom cr WHERE cr.isActive = true " +
           "AND (cr.buyer = :user OR cr.seller = :user)")
    Object[] getChatRoomStatisticsByUser(@Param("user") User user, 
                                         @Param("recentTime") java.time.LocalDateTime recentTime);

    /**
     * 구매자별 채팅방 조회
     */
    Page<ChatRoom> findByBuyerAndIsActiveTrueOrderByLastMessageAtDescCreatedAtDesc(User buyer, Pageable pageable);

    /**
     * 판매자별 채팅방 조회
     */
    Page<ChatRoom> findBySellerAndIsActiveTrueOrderByLastMessageAtDescCreatedAtDesc(User seller, Pageable pageable);
}