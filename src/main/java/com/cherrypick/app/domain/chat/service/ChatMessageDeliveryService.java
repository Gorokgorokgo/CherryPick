package com.cherrypick.app.domain.chat.service;

import com.cherrypick.app.domain.chat.entity.ChatMessage;
import com.cherrypick.app.domain.chat.enums.MessageDeliveryStatus;
import com.cherrypick.app.domain.chat.repository.ChatMessageRepository;
import com.cherrypick.app.domain.websocket.service.WebSocketMessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 전송 상태 추적 서비스
 * 실시간 채팅에서 메시지의 전송, 전달, 읽음 상태를 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageDeliveryService {

    private final ChatMessageRepository chatMessageRepository;
    private final WebSocketMessagingService webSocketMessagingService;

    /**
     * 메시지를 SENT 상태로 표시
     * 
     * @param messageId 메시지 ID
     */
    @Transactional
    public void markAsSent(Long messageId) {
        ChatMessage message = findMessageById(messageId);
        
        // 이미 SENT 상태면 sentAt 시간만 업데이트
        message.setSentAt(LocalDateTime.now());
        chatMessageRepository.save(message);
        
        log.debug("메시지 SENT 상태 업데이트: messageId={}", messageId);
    }

    /**
     * 메시지를 DELIVERED 상태로 표시
     * 
     * @param messageId 메시지 ID
     * @param receiverId 수신자 ID
     */
    @Transactional
    public void markAsDelivered(Long messageId, Long receiverId) {
        ChatMessage message = findMessageById(messageId);
        
        // 발신자 본인은 자신의 메시지를 DELIVERED로 표시할 수 없음
        if (message.getSender().getId().equals(receiverId)) {
            throw new IllegalArgumentException("발신자는 자신의 메시지를 전달됨 처리할 수 없습니다");
        }
        
        try {
            message.markAsDelivered();
            chatMessageRepository.save(message);
            
            // 실시간 전송 상태 알림
            webSocketMessagingService.notifyMessageDelivered(
                message.getChatRoom().getId(), 
                messageId, 
                receiverId
            );
            
            log.debug("메시지 DELIVERED 상태 업데이트: messageId={}, receiverId={}", messageId, receiverId);
            
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    /**
     * 메시지를 READ 상태로 표시
     * 
     * @param messageId 메시지 ID
     * @param readerId 읽은 사용자 ID
     */
    @Transactional
    public void markAsRead(Long messageId, Long readerId) {
        ChatMessage message = findMessageById(messageId);
        
        // 발신자 본인은 자신의 메시지를 READ로 표시할 수 없음
        if (message.getSender().getId().equals(readerId)) {
            throw new IllegalArgumentException("발신자는 자신의 메시지를 읽음 처리할 수 없습니다");
        }
        
        try {
            message.markAsRead();
            chatMessageRepository.save(message);
            
            // 실시간 읽음 상태 알림 (기존 WebSocketMessagingService 메서드 사용)
            webSocketMessagingService.notifyMessageRead(
                message.getChatRoom().getId(), 
                messageId, 
                readerId
            );
            
            log.debug("메시지 READ 상태 업데이트: messageId={}, readerId={}", messageId, readerId);
            
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    /**
     * 채팅방의 모든 미읽은 메시지를 READ 상태로 표시
     * 
     * @param chatRoomId 채팅방 ID
     * @param readerId 읽은 사용자 ID
     * @return 읽음 처리된 메시지 수
     */
    @Transactional
    public int markAllUnreadMessagesAsRead(Long chatRoomId, Long readerId) {
        // 해당 사용자가 보낸 메시지가 아닌 모든 미읽은 메시지 조회
        var unreadMessages = chatMessageRepository.findUnreadMessagesByChatRoomIdAndNotSender(
            chatRoomId, readerId
        );
        
        int updatedCount = 0;
        for (ChatMessage message : unreadMessages) {
            try {
                message.markAsRead();
                chatMessageRepository.save(message);
                updatedCount++;
                
                // 각 메시지에 대해 읽음 알림 전송
                webSocketMessagingService.notifyMessageRead(
                    chatRoomId, 
                    message.getId(), 
                    readerId
                );
                
            } catch (IllegalStateException e) {
                log.warn("메시지 READ 상태 변경 실패: messageId={}, error={}", 
                        message.getId(), e.getMessage());
            }
        }
        
        log.info("채팅방 전체 메시지 READ 상태 업데이트: chatRoomId={}, readerId={}, updatedCount={}", 
                chatRoomId, readerId, updatedCount);
        
        return updatedCount;
    }

    /**
     * 메시지 ID로 메시지 조회 (공통 메서드)
     * 
     * @param messageId 메시지 ID
     * @return 채팅 메시지
     * @throws IllegalArgumentException 메시지를 찾을 수 없는 경우
     */
    private ChatMessage findMessageById(Long messageId) {
        return chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다: " + messageId));
    }
}