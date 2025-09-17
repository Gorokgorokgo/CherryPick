package com.cherrypick.app.domain.chat.service;

import com.cherrypick.app.domain.user.enums.OnlineStatus;
import com.cherrypick.app.domain.websocket.service.WebSocketMessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.cherrypick.app.domain.websocket.event.UserConnectionEvent;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 사용자 온라인 상태 추적 서비스
 * 실시간 채팅에서 사용자의 온라인/오프라인 상태를 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserOnlineStatusService {

    private final WebSocketMessagingService webSocketMessagingService;
    
    // 사용자별 활성 세션 ID들 (userId -> Set<sessionId>)
    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();
    
    // 사용자별 마지막 활동 시간 (userId -> lastSeenTime)
    private final Map<Long, LocalDateTime> userLastActivity = new ConcurrentHashMap<>();
    
    // 사용자 비활성화 임계값 (분)
    private static final int INACTIVITY_TIMEOUT_MINUTES = 5;

    /**
     * 사용자를 온라인 상태로 설정
     * 
     * @param userId 사용자 ID
     * @param sessionId 세션 ID
     */
    public void setUserOnline(Long userId, String sessionId) {
        boolean wasOffline = !isUserOnline(userId);
        
        // 사용자 세션 추가
        userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(sessionId);
        
        // 마지막 활동 시간 업데이트
        userLastActivity.put(userId, LocalDateTime.now());
        
        // 오프라인에서 온라인으로 상태가 변경된 경우에만 알림 전송
        if (wasOffline) {
            webSocketMessagingService.notifyUserOnlineStatus(userId, true);
            log.debug("사용자 온라인 상태 변경: userId={}, sessionId={}", userId, sessionId);
        }
    }

    /**
     * 사용자를 오프라인 상태로 설정
     * 
     * @param userId 사용자 ID
     * @param sessionId 세션 ID
     */
    public void setUserOffline(Long userId, String sessionId) {
        Set<String> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            
            // 모든 세션이 종료된 경우에만 오프라인 처리
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
                webSocketMessagingService.notifyUserOnlineStatus(userId, false);
                log.debug("사용자 오프라인 상태 변경: userId={}, sessionId={}", userId, sessionId);
            }
        }
    }

    /**
     * 사용자의 활동 시간 업데이트
     * 
     * @param userId 사용자 ID
     * @param sessionId 세션 ID
     */
    public void updateUserActivity(Long userId, String sessionId) {
        Set<String> sessions = userSessions.get(userId);
        if (sessions != null && sessions.contains(sessionId)) {
            userLastActivity.put(userId, LocalDateTime.now());
            log.trace("사용자 활동 시간 업데이트: userId={}, sessionId={}", userId, sessionId);
        }
    }

    /**
     * 사용자가 온라인 상태인지 확인
     * 
     * @param userId 사용자 ID
     * @return 온라인 상태 여부
     */
    public boolean isUserOnline(Long userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * 사용자의 온라인 상태 조회
     * 
     * @param userId 사용자 ID
     * @return 온라인 상태
     */
    public OnlineStatus getUserOnlineStatus(Long userId) {
        if (!isUserOnline(userId)) {
            return OnlineStatus.OFFLINE;
        }
        
        LocalDateTime lastActivity = userLastActivity.get(userId);
        if (lastActivity != null) {
            Duration inactiveDuration = Duration.between(lastActivity, LocalDateTime.now());
            
            // 2분 이상 비활성이면 자리비움 상태
            if (inactiveDuration.toMinutes() >= 2) {
                return OnlineStatus.AWAY;
            }
        }
        
        return OnlineStatus.ONLINE;
    }

    /**
     * 사용자의 마지막 활동 시간 조회
     * 
     * @param userId 사용자 ID
     * @return 마지막 활동 시간
     */
    public LocalDateTime getUserLastSeen(Long userId) {
        return userLastActivity.get(userId);
    }

    /**
     * 채팅방 참여자들의 온라인 상태 조회
     * 
     * @param chatRoomId 채팅방 ID
     * @param participantIds 참여자 ID 목록
     * @return 사용자 ID별 온라인 상태 맵
     */
    public Map<Long, Boolean> getChatRoomParticipantsOnlineStatus(Long chatRoomId, Set<Long> participantIds) {
        Map<Long, Boolean> result = new ConcurrentHashMap<>();
        
        for (Long userId : participantIds) {
            result.put(userId, isUserOnline(userId));
        }
        
        log.debug("채팅방 참여자 온라인 상태 조회: chatRoomId={}, participants={}", chatRoomId, result);
        return result;
    }

    /**
     * 비활성 사용자들을 자동으로 오프라인 처리
     * 5분마다 실행되는 스케줄러
     */
    @Scheduled(fixedRate = 300000) // 5분마다 실행
    public void cleanupInactiveUsers() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(INACTIVITY_TIMEOUT_MINUTES);
        
        userLastActivity.entrySet().removeIf(entry -> {
            Long userId = entry.getKey();
            LocalDateTime lastActivity = entry.getValue();
            
            if (lastActivity.isBefore(cutoffTime)) {
                // 비활성 사용자를 오프라인 처리
                Set<String> sessions = userSessions.remove(userId);
                if (sessions != null && !sessions.isEmpty()) {
                    webSocketMessagingService.notifyUserOnlineStatus(userId, false);
                    log.info("비활성 사용자 자동 오프라인 처리: userId={}, lastActivity={}", userId, lastActivity);
                }
                return true; // 맵에서 제거
            }
            return false;
        });
    }

    /**
     * 현재 온라인 사용자 수 조회 (모니터링용)
     * 
     * @return 온라인 사용자 수
     */
    public int getOnlineUserCount() {
        return userSessions.size();
    }

    /**
     * 특정 사용자의 활성 세션 수 조회 (모니터링용)
     * 
     * @param userId 사용자 ID
     * @return 활성 세션 수
     */
    public int getUserSessionCount(Long userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null ? sessions.size() : 0;
    }

    /**
     * WebSocket 연결 이벤트 처리
     */
    @EventListener
    public void handleUserConnectionEvent(UserConnectionEvent event) {
        switch (event.getEventType()) {
            case CONNECTED:
                setUserOnline(event.getUserId(), event.getSessionId());
                break;
            case DISCONNECTED:
                setUserOffline(event.getUserId(), event.getSessionId());
                break;
            case ACTIVITY_UPDATE:
                updateUserActivity(event.getUserId(), event.getSessionId());
                break;
        }
    }
    
    /**
     * 세션 ID로 사용자를 오프라인 처리 (WebSocket 연결 해제 시 사용)
     * 
     * @param sessionId 세션 ID
     */
    public void handleSessionDisconnect(String sessionId) {
        userSessions.entrySet().removeIf(entry -> {
            Long userId = entry.getKey();
            Set<String> sessions = entry.getValue();
            
            boolean removed = sessions.remove(sessionId);
            
            if (removed && sessions.isEmpty()) {
                webSocketMessagingService.notifyUserOnlineStatus(userId, false);
                log.debug("세션 연결 해제로 사용자 오프라인 처리: userId={}, sessionId={}", userId, sessionId);
                return true; // 맵에서 제거
            }
            
            return false;
        });
    }
}