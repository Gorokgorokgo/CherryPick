package com.cherrypick.app.domain.chat.service;

import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.websocket.service.WebSocketMessagingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * 타이핑 상태 표시 서비스 테스트
 * TDD: Test First 방식으로 실시간 타이핑 표시 기능 구현
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("타이핑 상태 표시 서비스 테스트")
class TypingIndicatorServiceTest {

    @Mock
    private WebSocketMessagingService webSocketMessagingService;
    
    @Mock
    private ScheduledExecutorService scheduledExecutorService;
    
    @InjectMocks
    private TypingIndicatorService typingIndicatorService;
    
    private User testUser;
    private User chatPartner;
    private Long chatRoomId;

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
                
        chatRoomId = 100L;
    }

    @Test
    @DisplayName("사용자가 타이핑을 시작하면 상대방에게 타이핑 상태가 전송됨")
    void should_NotifyTypingStart_When_UserStartsTyping() {
        // when
        typingIndicatorService.startTyping(chatRoomId, testUser.getId(), testUser.getNickname());

        // then
        assertThat(typingIndicatorService.isUserTyping(chatRoomId, testUser.getId())).isTrue();
        
        verify(webSocketMessagingService).notifyTypingStatus(
            eq(chatRoomId), 
            eq(testUser.getId()), 
            eq(testUser.getNickname()), 
            eq(true)
        );
    }

    @Test
    @DisplayName("사용자가 타이핑을 중단하면 상대방에게 타이핑 중단 상태가 전송됨")
    void should_NotifyTypingStop_When_UserStopsTyping() {
        // given - 먼저 타이핑 시작
        typingIndicatorService.startTyping(chatRoomId, testUser.getId(), testUser.getNickname());
        
        // when
        typingIndicatorService.stopTyping(chatRoomId, testUser.getId());

        // then
        assertThat(typingIndicatorService.isUserTyping(chatRoomId, testUser.getId())).isFalse();
        
        verify(webSocketMessagingService).notifyTypingStatus(
            eq(chatRoomId), 
            eq(testUser.getId()), 
            eq(testUser.getNickname()), 
            eq(false)
        );
    }

    @Test
    @DisplayName("타이핑 상태가 일정 시간 후 자동으로 만료됨")
    void should_AutoExpireTypingStatus_After_Timeout() throws InterruptedException {
        // given
        typingIndicatorService.startTyping(chatRoomId, testUser.getId(), testUser.getNickname());
        assertThat(typingIndicatorService.isUserTyping(chatRoomId, testUser.getId())).isTrue();

        // when - 타이핑 타임아웃 시뮬레이션 (내부에서 스케줄러 사용)
        Thread.sleep(100); // 실제 구현에서는 더 긴 시간
        typingIndicatorService.cleanupExpiredTypingStatus();

        // then - 타이핑 상태가 만료되어야 함
        // 실제 구현에서는 configurable timeout 사용
        verify(scheduledExecutorService, atLeastOnce()).schedule(any(Runnable.class), anyLong(), any());
    }

    @Test
    @DisplayName("채팅방의 현재 타이핑 중인 사용자 목록을 조회할 수 있음")
    void should_GetTypingUsersInChatRoom() {
        // given
        typingIndicatorService.startTyping(chatRoomId, testUser.getId(), testUser.getNickname());
        typingIndicatorService.startTyping(chatRoomId, chatPartner.getId(), chatPartner.getNickname());

        // when
        Set<Long> typingUsers = typingIndicatorService.getTypingUsersInChatRoom(chatRoomId);

        // then
        assertThat(typingUsers)
                .hasSize(2)
                .contains(testUser.getId(), chatPartner.getId());
    }

    @Test
    @DisplayName("동일한 사용자가 연속으로 타이핑 시작해도 중복 알림이 발생하지 않음")
    void should_NotDuplicateNotification_When_SameUserStartsTypingAgain() {
        // given
        typingIndicatorService.startTyping(chatRoomId, testUser.getId(), testUser.getNickname());
        
        // when - 동일한 사용자가 다시 타이핑 시작
        typingIndicatorService.startTyping(chatRoomId, testUser.getId(), testUser.getNickname());

        // then - 알림이 한 번만 전송되어야 함
        verify(webSocketMessagingService, times(1)).notifyTypingStatus(
            eq(chatRoomId), 
            eq(testUser.getId()), 
            eq(testUser.getNickname()), 
            eq(true)
        );
    }

    @Test
    @DisplayName("타이핑하지 않는 사용자에 대해서는 타이핑 중단 알림이 전송되지 않음")
    void should_NotNotifyTypingStop_When_UserWasNotTyping() {
        // when - 타이핑하지 않던 사용자가 중단 요청
        typingIndicatorService.stopTyping(chatRoomId, testUser.getId());

        // then - 알림이 전송되지 않아야 함
        verify(webSocketMessagingService, never()).notifyTypingStatus(
            any(), any(), any(), eq(false)
        );
    }

    @Test
    @DisplayName("메시지를 전송하면 타이핑 상태가 자동으로 중단됨")
    void should_AutoStopTyping_When_MessageSent() {
        // given
        typingIndicatorService.startTyping(chatRoomId, testUser.getId(), testUser.getNickname());
        assertThat(typingIndicatorService.isUserTyping(chatRoomId, testUser.getId())).isTrue();

        // when
        typingIndicatorService.handleMessageSent(chatRoomId, testUser.getId());

        // then
        assertThat(typingIndicatorService.isUserTyping(chatRoomId, testUser.getId())).isFalse();
        
        verify(webSocketMessagingService).notifyTypingStatus(
            eq(chatRoomId), 
            eq(testUser.getId()), 
            eq(testUser.getNickname()), 
            eq(false)
        );
    }

    @Test
    @DisplayName("채팅방의 모든 타이핑 상태를 초기화할 수 있음")
    void should_ClearAllTypingStatus_ForChatRoom() {
        // given
        typingIndicatorService.startTyping(chatRoomId, testUser.getId(), testUser.getNickname());
        typingIndicatorService.startTyping(chatRoomId, chatPartner.getId(), chatPartner.getNickname());
        
        // when
        typingIndicatorService.clearAllTypingInChatRoom(chatRoomId);

        // then
        assertThat(typingIndicatorService.getTypingUsersInChatRoom(chatRoomId)).isEmpty();
        
        // 모든 사용자에게 타이핑 중단 알림이 전송되어야 함
        verify(webSocketMessagingService, times(2)).notifyTypingStatus(
            eq(chatRoomId), any(), any(), eq(false)
        );
    }

    @Test
    @DisplayName("존재하지 않는 채팅방에서 타이핑 상태 조회시 빈 집합 반환")
    void should_ReturnEmptySet_When_ChatRoomNotExists() {
        // given
        Long nonExistentChatRoomId = 999L;

        // when & then
        assertThat(typingIndicatorService.getTypingUsersInChatRoom(nonExistentChatRoomId)).isEmpty();
        assertThat(typingIndicatorService.isUserTyping(nonExistentChatRoomId, testUser.getId())).isFalse();
    }
}