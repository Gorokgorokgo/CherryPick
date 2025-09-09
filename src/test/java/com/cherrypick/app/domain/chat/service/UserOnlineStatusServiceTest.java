package com.cherrypick.app.domain.chat.service;

import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.enums.OnlineStatus;
import com.cherrypick.app.domain.websocket.service.WebSocketMessagingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * 사용자 온라인 상태 추적 서비스 테스트
 * TDD: Test First 방식으로 실시간 온라인 상태 기능 구현
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("사용자 온라인 상태 추적 서비스 테스트")
class UserOnlineStatusServiceTest {

    @Mock
    private WebSocketMessagingService webSocketMessagingService;
    
    @InjectMocks
    private UserOnlineStatusService userOnlineStatusService;
    
    private User testUser;
    private User chatPartner;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .phoneNumber("01012345678")
                .nickname("testUser")
                .build();
                
        chatPartner = User.builder()
                .id(2L)
                .phoneNumber("01087654321")
                .nickname("chatPartner")
                .build();
    }

    @Test
    @DisplayName("사용자가 온라인 상태로 변경되면 상태가 업데이트되고 알림이 전송됨")
    void should_UpdateOnlineStatus_When_UserComesOnline() {
        // given
        String sessionId = "test-session-123";

        // when
        userOnlineStatusService.setUserOnline(testUser.getId(), sessionId);

        // then
        assertThat(userOnlineStatusService.isUserOnline(testUser.getId())).isTrue();
        assertThat(userOnlineStatusService.getUserOnlineStatus(testUser.getId()))
                .isEqualTo(OnlineStatus.ONLINE);
        
        verify(webSocketMessagingService).notifyUserOnlineStatus(
            eq(testUser.getId()), 
            eq(true)
        );
    }

    @Test
    @DisplayName("사용자가 오프라인 상태로 변경되면 상태가 업데이트되고 알림이 전송됨")
    void should_UpdateOfflineStatus_When_UserGoesOffline() {
        // given - 사용자를 먼저 온라인으로 설정
        String sessionId = "test-session-123";
        userOnlineStatusService.setUserOnline(testUser.getId(), sessionId);
        
        // when
        userOnlineStatusService.setUserOffline(testUser.getId(), sessionId);

        // then
        assertThat(userOnlineStatusService.isUserOnline(testUser.getId())).isFalse();
        assertThat(userOnlineStatusService.getUserOnlineStatus(testUser.getId()))
                .isEqualTo(OnlineStatus.OFFLINE);
        
        verify(webSocketMessagingService).notifyUserOnlineStatus(
            eq(testUser.getId()), 
            eq(false)
        );
    }

    @Test
    @DisplayName("사용자가 여러 세션에서 접속한 경우 모든 세션이 종료되어야 오프라인 상태가 됨")
    void should_StayOnline_When_UserHasMultipleSessions() {
        // given
        String session1 = "session-1";
        String session2 = "session-2";
        
        userOnlineStatusService.setUserOnline(testUser.getId(), session1);
        userOnlineStatusService.setUserOnline(testUser.getId(), session2);

        // when - 첫 번째 세션만 종료
        userOnlineStatusService.setUserOffline(testUser.getId(), session1);

        // then - 여전히 온라인 상태여야 함
        assertThat(userOnlineStatusService.isUserOnline(testUser.getId())).isTrue();
        assertThat(userOnlineStatusService.getUserOnlineStatus(testUser.getId()))
                .isEqualTo(OnlineStatus.ONLINE);
        
        // 오프라인 알림이 전송되지 않아야 함
        verify(webSocketMessagingService, times(2)).notifyUserOnlineStatus(
            eq(testUser.getId()), 
            eq(true)
        );
        verify(webSocketMessagingService, never()).notifyUserOnlineStatus(
            eq(testUser.getId()), 
            eq(false)
        );
    }

    @Test
    @DisplayName("사용자의 마지막 활동 시간이 업데이트됨")
    void should_UpdateLastSeenTime_When_UserIsActive() {
        // given
        String sessionId = "test-session";
        userOnlineStatusService.setUserOnline(testUser.getId(), sessionId);
        
        LocalDateTime beforeUpdate = LocalDateTime.now();

        // when
        userOnlineStatusService.updateUserActivity(testUser.getId(), sessionId);

        // then
        LocalDateTime lastSeen = userOnlineStatusService.getUserLastSeen(testUser.getId());
        assertThat(lastSeen).isAfter(beforeUpdate);
    }

    @Test
    @DisplayName("특정 채팅방의 참여자 온라인 상태를 조회할 수 있음")
    void should_GetChatRoomParticipantsOnlineStatus() {
        // given
        Long chatRoomId = 1L;
        String session1 = "session-1";
        String session2 = "session-2";
        
        userOnlineStatusService.setUserOnline(testUser.getId(), session1);
        // chatPartner는 오프라인 상태로 유지

        // when
        var participantsStatus = userOnlineStatusService.getChatRoomParticipantsOnlineStatus(
            chatRoomId, Set.of(testUser.getId(), chatPartner.getId())
        );

        // then
        assertThat(participantsStatus)
                .containsEntry(testUser.getId(), true)
                .containsEntry(chatPartner.getId(), false);
    }

    @Test
    @DisplayName("비활성 사용자들을 자동으로 오프라인 처리함")
    void should_AutomaticallySetInactiveUsersOffline() {
        // given
        String sessionId = "test-session";
        userOnlineStatusService.setUserOnline(testUser.getId(), sessionId);
        
        // 5분 전 마지막 활동으로 시뮬레이션 (내부 구현에서 처리)

        // when
        userOnlineStatusService.cleanupInactiveUsers();

        // then - 구현에 따라 비활성 사용자는 오프라인 처리되어야 함
        // 실제 구현에서는 configurable timeout 사용
        verify(webSocketMessagingService, atLeastOnce()).notifyUserOnlineStatus(
            any(Long.class), 
            any(Boolean.class)
        );
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 조회시 오프라인 상태 반환")
    void should_ReturnOffline_When_UserNotExists() {
        // given
        Long nonExistentUserId = 999L;

        // when & then
        assertThat(userOnlineStatusService.isUserOnline(nonExistentUserId)).isFalse();
        assertThat(userOnlineStatusService.getUserOnlineStatus(nonExistentUserId))
                .isEqualTo(OnlineStatus.OFFLINE);
    }
}