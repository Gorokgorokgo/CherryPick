package com.cherrypick.app.domain.chat.repository;

import com.cherrypick.app.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 채팅 메시지 리포지토리
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 채팅방의 메시지 목록 조회 (페이지네이션)
     * 
     * @param chatRoomId 채팅방 ID
     * @param pageable 페이지네이션 정보
     * @return 메시지 목록
     */
    @Query("SELECT m FROM ChatMessage m " +
           "WHERE m.chatRoom.id = :chatRoomId " +
           "ORDER BY m.createdAt DESC")
    Page<ChatMessage> findByChatRoomId(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    /**
     * 채팅방의 최신 메시지 조회
     * 
     * @param chatRoomId 채팅방 ID
     * @return 최신 메시지
     */
    @Query("SELECT m FROM ChatMessage m " +
           "WHERE m.chatRoom.id = :chatRoomId " +
           "ORDER BY m.createdAt DESC " +
           "LIMIT 1")
    Optional<ChatMessage> findLatestMessageByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    /**
     * 사용자의 읽지 않은 메시지 개수 조회 (특정 채팅방)
     * 
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 읽지 않은 메시지 개수
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m " +
           "WHERE m.chatRoom.id = :chatRoomId " +
           "AND m.sender.id != :userId " +
           "AND m.isRead = false")
    int countUnreadMessagesByChatRoomIdAndUserId(
            @Param("chatRoomId") Long chatRoomId, 
            @Param("userId") Long userId);

    /**
     * 사용자의 전체 읽지 않은 메시지 개수 조회
     * 
     * @param userId 사용자 ID
     * @return 전체 읽지 않은 메시지 개수
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m " +
           "WHERE (m.chatRoom.seller.id = :userId OR m.chatRoom.buyer.id = :userId) " +
           "AND m.sender.id != :userId " +
           "AND m.isRead = false")
    int countUnreadMessagesByUserId(@Param("userId") Long userId);

    /**
     * 채팅방의 읽지 않은 메시지를 모두 읽음 처리
     * 
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID (읽음 처리하는 사용자)
     * @return 업데이트된 메시지 개수
     */
    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true, m.readAt = CURRENT_TIMESTAMP " +
           "WHERE m.chatRoom.id = :chatRoomId " +
           "AND m.sender.id != :userId " +
           "AND m.isRead = false")
    int markAllMessagesAsReadInChatRoom(
            @Param("chatRoomId") Long chatRoomId, 
            @Param("userId") Long userId);

    /**
     * 특정 메시지를 읽음 처리
     * 
     * @param messageId 메시지 ID
     * @param userId 사용자 ID (읽음 처리하는 사용자)
     * @return 업데이트된 메시지 개수
     */
    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true, m.readAt = CURRENT_TIMESTAMP " +
           "WHERE m.id = :messageId " +
           "AND m.sender.id != :userId " +
           "AND m.isRead = false")
    int markMessageAsRead(@Param("messageId") Long messageId, @Param("userId") Long userId);

    /**
     * 특정 사용자가 참여하는 채팅방들의 최신 메시지 조회
     * 
     * @param userId 사용자 ID
     * @return 채팅방별 최신 메시지 목록
     */
    @Query("SELECT m FROM ChatMessage m " +
           "WHERE m.id IN (" +
           "    SELECT MAX(m2.id) FROM ChatMessage m2 " +
           "    WHERE (m2.chatRoom.seller.id = :userId OR m2.chatRoom.buyer.id = :userId) " +
           "    GROUP BY m2.chatRoom.id" +
           ") " +
           "ORDER BY m.createdAt DESC")
    List<ChatMessage> findLatestMessagesByUserId(@Param("userId") Long userId);

    /**
     * 채팅방의 메시지 개수 조회
     * 
     * @param chatRoomId 채팅방 ID
     * @return 메시지 개수
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.chatRoom.id = :chatRoomId")
    long countByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    /**
     * 채팅방의 시스템 메시지 조회
     * 
     * @param chatRoomId 채팅방 ID
     * @return 시스템 메시지 목록
     */
    @Query("SELECT m FROM ChatMessage m " +
           "WHERE m.chatRoom.id = :chatRoomId " +
           "AND m.messageType = 'SYSTEM' " +
           "ORDER BY m.createdAt DESC")
    List<ChatMessage> findSystemMessagesByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    /**
     * 특정 기간 동안의 메시지 조회
     * 
     * @param chatRoomId 채팅방 ID
     * @param startDate 시작 날짜 (ISO 문자열)
     * @param endDate 종료 날짜 (ISO 문자열)
     * @return 해당 기간의 메시지 목록
     */
    @Query("SELECT m FROM ChatMessage m " +
           "WHERE m.chatRoom.id = :chatRoomId " +
           "AND m.createdAt >= CAST(:startDate AS timestamp) " +
           "AND m.createdAt <= CAST(:endDate AS timestamp) " +
           "ORDER BY m.createdAt ASC")
    List<ChatMessage> findMessagesByDateRange(
            @Param("chatRoomId") Long chatRoomId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /**
     * 채팅방의 미읽은 메시지 조회 (특정 사용자가 보낸 메시지 제외)
     * 메시지 전송 상태 업데이트를 위한 쿼리
     * 
     * @param chatRoomId 채팅방 ID
     * @param userId 제외할 사용자 ID (발신자)
     * @return 미읽은 메시지 목록
     */
    @Query("SELECT m FROM ChatMessage m " +
           "WHERE m.chatRoom.id = :chatRoomId " +
           "AND m.sender.id != :userId " +
           "AND m.isRead = false " +
           "ORDER BY m.createdAt ASC")
    List<ChatMessage> findUnreadMessagesByChatRoomIdAndNotSender(
            @Param("chatRoomId") Long chatRoomId,
            @Param("userId") Long userId);
}