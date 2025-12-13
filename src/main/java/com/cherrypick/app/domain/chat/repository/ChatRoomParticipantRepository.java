package com.cherrypick.app.domain.chat.repository;

import com.cherrypick.app.domain.chat.entity.ChatRoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 채팅방 참여자 리포지토리
 */
@Repository
public interface ChatRoomParticipantRepository extends JpaRepository<ChatRoomParticipant, Long> {

    /**
     * 채팅방과 사용자로 참여자 정보 조회
     */
    Optional<ChatRoomParticipant> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    /**
     * 채팅방의 모든 참여자 조회
     */
    List<ChatRoomParticipant> findByChatRoomId(Long chatRoomId);

    /**
     * 사용자의 모든 참여 정보 조회 (나간 채팅방 포함)
     */
    List<ChatRoomParticipant> findByUserId(Long userId);

    /**
     * 사용자의 활성 참여 정보만 조회 (나가지 않은 채팅방)
     */
    @Query("SELECT p FROM ChatRoomParticipant p WHERE p.user.id = :userId AND p.isLeft = false")
    List<ChatRoomParticipant> findActiveByUserId(@Param("userId") Long userId);

    /**
     * 사용자가 나간 채팅방의 참여 정보 조회
     */
    @Query("SELECT p FROM ChatRoomParticipant p WHERE p.user.id = :userId AND p.isLeft = true")
    List<ChatRoomParticipant> findLeftByUserId(@Param("userId") Long userId);

    /**
     * 특정 채팅방에서 사용자가 나갔는지 확인
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM ChatRoomParticipant p " +
           "WHERE p.chatRoom.id = :chatRoomId AND p.user.id = :userId AND p.isLeft = true")
    boolean hasUserLeftChatRoom(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    /**
     * 특정 채팅방에서 사용자가 활성 상태인지 확인
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM ChatRoomParticipant p " +
           "WHERE p.chatRoom.id = :chatRoomId AND p.user.id = :userId AND p.isLeft = false")
    boolean isUserActiveInChatRoom(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    /**
     * 참여자 존재 여부 확인
     */
    boolean existsByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    /**
     * 사용자의 총 읽지 않은 메시지 개수 조회
     */
    @Query("SELECT COALESCE(SUM(p.unreadCount), 0) FROM ChatRoomParticipant p WHERE p.user.id = :userId AND p.isLeft = false")
    int sumUnreadCountByUserId(@Param("userId") Long userId);
}
