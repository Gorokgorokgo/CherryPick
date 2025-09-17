package com.cherrypick.app.domain.chat.repository;

import com.cherrypick.app.domain.chat.entity.ChatRoom;
import com.cherrypick.app.domain.chat.enums.ChatRoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 채팅방 리포지토리
 */
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 연결 서비스 ID로 채팅방 조회
     */
    Optional<ChatRoom> findByConnectionServiceId(Long connectionServiceId);
    
    /**
     * 사용자별 채팅방 목록 조회 (판매자 또는 구매자) - List 버전
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.seller.id = :userId OR cr.buyer.id = :userId ORDER BY cr.lastMessageAt DESC, cr.createdAt DESC")
    List<ChatRoom> findByUserId(@Param("userId") Long userId);
    
    /**
     * 사용자별 채팅방 목록 조회 (판매자 또는 구매자) - Pageable 버전
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.seller.id = :userId OR cr.buyer.id = :userId ORDER BY cr.lastMessageAt DESC, cr.createdAt DESC")
    Page<ChatRoom> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 사용자별 특정 상태의 채팅방 목록 조회
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE (cr.seller.id = :userId OR cr.buyer.id = :userId) AND cr.status = :status ORDER BY cr.lastMessageAt DESC, cr.createdAt DESC")
    List<ChatRoom> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") ChatRoomStatus status);
    
    /**
     * 판매자의 채팅방 목록 조회
     */
    Page<ChatRoom> findBySellerIdOrderByLastMessageAtDescCreatedAtDesc(Long sellerId, Pageable pageable);
    
    /**
     * 구매자의 채팅방 목록 조회
     */
    Page<ChatRoom> findByBuyerIdOrderByLastMessageAtDescCreatedAtDesc(Long buyerId, Pageable pageable);
    
    /**
     * 활성화된 채팅방 개수 조회 (사용자별)
     */
    @Query("SELECT COUNT(cr) FROM ChatRoom cr WHERE (cr.seller.id = :userId OR cr.buyer.id = :userId) AND cr.status = 'ACTIVE'")
    long countActiveRoomsByUserId(@Param("userId") Long userId);
    
    /**
     * 경매 기반 채팅방 조회 (경매 ID, 판매자 ID, 구매자 ID로)
     */
    Optional<ChatRoom> findByAuctionIdAndSellerIdAndBuyerId(
            @Param("auctionId") Long auctionId,
            @Param("sellerId") Long sellerId,
            @Param("buyerId") Long buyerId);
}