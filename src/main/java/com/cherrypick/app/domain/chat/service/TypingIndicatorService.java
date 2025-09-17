package com.cherrypick.app.domain.chat.service;

import com.cherrypick.app.domain.websocket.service.WebSocketMessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.cherrypick.app.domain.websocket.event.TypingEvent;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * 타이핑 상태 표시 서비스
 * 실시간 채팅에서 사용자의 타이핑 상태를 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TypingIndicatorService {

    private final WebSocketMessagingService webSocketMessagingService;
    
    // 채팅방별 타이핑 중인 사용자 정보 (chatRoomId -> Set<TypingUserInfo>)
    private final Map<Long, Set<TypingUserInfo>> chatRoomTypingUsers = new ConcurrentHashMap<>();
    
    // 타이핑 타임아웃 시간 (초)
    private static final int TYPING_TIMEOUT_SECONDS = 3;

    /**
     * 사용자가 타이핑을 시작함을 알림
     * 
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID
     * @param userNickname 사용자 닉네임
     */
    public void startTyping(Long chatRoomId, Long userId, String userNickname) {
        Set<TypingUserInfo> typingUsers = chatRoomTypingUsers.computeIfAbsent(
            chatRoomId, k -> new CopyOnWriteArraySet<>()
        );
        
        TypingUserInfo userInfo = new TypingUserInfo(userId, userNickname, LocalDateTime.now());
        
        // 이미 타이핑 중인 사용자인지 확인
        boolean wasAlreadyTyping = typingUsers.stream()
                .anyMatch(info -> info.userId.equals(userId));
        
        if (wasAlreadyTyping) {
            // 기존 정보 업데이트 (마지막 타이핑 시간 갱신)
            typingUsers.removeIf(info -> info.userId.equals(userId));
        }
        
        typingUsers.add(userInfo);
        
        // 새롭게 타이핑을 시작한 경우에만 알림 전송
        if (!wasAlreadyTyping) {
            webSocketMessagingService.notifyTypingStatus(chatRoomId, userId, userNickname, true);
            log.debug("타이핑 시작 알림: chatRoomId={}, userId={}, nickname={}", chatRoomId, userId, userNickname);
        }
    }

    /**
     * 사용자가 타이핑을 중단함을 알림
     * 
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID
     */
    public void stopTyping(Long chatRoomId, Long userId) {
        Set<TypingUserInfo> typingUsers = chatRoomTypingUsers.get(chatRoomId);
        if (typingUsers == null) {
            return;
        }
        
        // 타이핑 중이었던 사용자 찾기
        TypingUserInfo userInfo = typingUsers.stream()
                .filter(info -> info.userId.equals(userId))
                .findFirst()
                .orElse(null);
        
        if (userInfo != null) {
            typingUsers.remove(userInfo);
            
            // 채팅방에 타이핑 중인 사용자가 없으면 맵에서 제거
            if (typingUsers.isEmpty()) {
                chatRoomTypingUsers.remove(chatRoomId);
            }
            
            webSocketMessagingService.notifyTypingStatus(chatRoomId, userId, userInfo.userNickname, false);
            log.debug("타이핑 중단 알림: chatRoomId={}, userId={}, nickname={}", chatRoomId, userId, userInfo.userNickname);
        }
    }

    /**
     * 사용자가 타이핑 중인지 확인
     * 
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 타이핑 중인지 여부
     */
    public boolean isUserTyping(Long chatRoomId, Long userId) {
        Set<TypingUserInfo> typingUsers = chatRoomTypingUsers.get(chatRoomId);
        if (typingUsers == null) {
            return false;
        }
        
        return typingUsers.stream()
                .anyMatch(info -> info.userId.equals(userId));
    }

    /**
     * 채팅방에서 타이핑 중인 사용자 ID 목록 조회
     * 
     * @param chatRoomId 채팅방 ID
     * @return 타이핑 중인 사용자 ID 집합
     */
    public Set<Long> getTypingUsersInChatRoom(Long chatRoomId) {
        Set<TypingUserInfo> typingUsers = chatRoomTypingUsers.get(chatRoomId);
        if (typingUsers == null) {
            return Set.of();
        }
        
        return typingUsers.stream()
                .map(info -> info.userId)
                .collect(Collectors.toSet());
    }

    /**
     * 메시지 전송 시 해당 사용자의 타이핑 상태를 자동으로 중단
     * 
     * @param chatRoomId 채팅방 ID
     * @param userId 메시지를 전송한 사용자 ID
     */
    public void handleMessageSent(Long chatRoomId, Long userId) {
        if (isUserTyping(chatRoomId, userId)) {
            stopTyping(chatRoomId, userId);
            log.debug("메시지 전송으로 타이핑 상태 자동 중단: chatRoomId={}, userId={}", chatRoomId, userId);
        }
    }

    /**
     * 채팅방의 모든 타이핑 상태 초기화
     * 
     * @param chatRoomId 채팅방 ID
     */
    public void clearAllTypingInChatRoom(Long chatRoomId) {
        Set<TypingUserInfo> typingUsers = chatRoomTypingUsers.remove(chatRoomId);
        if (typingUsers != null && !typingUsers.isEmpty()) {
            // 모든 사용자에게 타이핑 중단 알림 전송
            for (TypingUserInfo userInfo : typingUsers) {
                webSocketMessagingService.notifyTypingStatus(
                    chatRoomId, userInfo.userId, userInfo.userNickname, false
                );
            }
            log.debug("채팅방 전체 타이핑 상태 초기화: chatRoomId={}, userCount={}", chatRoomId, typingUsers.size());
        }
    }

    /**
     * 만료된 타이핑 상태 정리 (3초 이상 업데이트되지 않은 상태)
     * 10초마다 실행되는 스케줄러
     */
    @Scheduled(fixedRate = 10000) // 10초마다 실행
    public void cleanupExpiredTypingStatus() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusSeconds(TYPING_TIMEOUT_SECONDS);
        
        chatRoomTypingUsers.entrySet().removeIf(entry -> {
            Long chatRoomId = entry.getKey();
            Set<TypingUserInfo> typingUsers = entry.getValue();
            
            // 만료된 사용자들 찾기 및 제거
            Set<TypingUserInfo> expiredUsers = typingUsers.stream()
                    .filter(info -> info.lastTypingTime.isBefore(cutoffTime))
                    .collect(Collectors.toSet());
            
            for (TypingUserInfo expiredUser : expiredUsers) {
                typingUsers.remove(expiredUser);
                webSocketMessagingService.notifyTypingStatus(
                    chatRoomId, expiredUser.userId, expiredUser.userNickname, false
                );
                log.debug("만료된 타이핑 상태 정리: chatRoomId={}, userId={}, nickname={}", 
                         chatRoomId, expiredUser.userId, expiredUser.userNickname);
            }
            
            // 타이핑 중인 사용자가 없으면 채팅방 항목 제거
            return typingUsers.isEmpty();
        });
    }

    /**
     * 타이핑 이벤트 처리
     */
    @EventListener
    public void handleTypingEvent(TypingEvent event) {
        switch (event.getEventType()) {
            case START:
                startTyping(event.getChatRoomId(), event.getUserId(), event.getUserNickname());
                break;
            case STOP:
                stopTyping(event.getChatRoomId(), event.getUserId());
                break;
            case MESSAGE_SENT:
                handleMessageSent(event.getChatRoomId(), event.getUserId());
                break;
        }
    }

    /**
     * 현재 전체 타이핑 중인 사용자 수 조회 (모니터링용)
     * 
     * @return 전체 타이핑 중인 사용자 수
     */
    public int getTotalTypingUserCount() {
        return chatRoomTypingUsers.values().stream()
                .mapToInt(Set::size)
                .sum();
    }

    /**
     * 타이핑 중인 사용자 정보를 담는 내부 클래스
     */
    private static class TypingUserInfo {
        final Long userId;
        final String userNickname;
        final LocalDateTime lastTypingTime;
        
        TypingUserInfo(Long userId, String userNickname, LocalDateTime lastTypingTime) {
            this.userId = userId;
            this.userNickname = userNickname;
            this.lastTypingTime = lastTypingTime;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof TypingUserInfo)) return false;
            TypingUserInfo that = (TypingUserInfo) obj;
            return userId.equals(that.userId);
        }
        
        @Override
        public int hashCode() {
            return userId.hashCode();
        }
    }
}