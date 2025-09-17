package com.cherrypick.app.domain.chat.service;

import com.cherrypick.app.domain.chat.entity.ChatMessage;
import com.cherrypick.app.domain.chat.entity.ChatRoom;
import com.cherrypick.app.domain.chat.enums.MessageDeliveryStatus;
import com.cherrypick.app.domain.chat.repository.ChatMessageRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.websocket.service.WebSocketMessagingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * 채팅 메시지 전송 상태 추적 서비스 테스트
 * TDD: Test First 방식으로 메시지 전송 상태 기능 구현
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("채팅 메시지 전송 상태 추적 서비스 테스트")
class ChatMessageDeliveryServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;
    
    @Mock
    private WebSocketMessagingService webSocketMessagingService;
    
    @InjectMocks
    private ChatMessageDeliveryService chatMessageDeliveryService;
    
    private ChatMessage testMessage;
    private ChatRoom testChatRoom;
    private User sender;
    private User receiver;

    @BeforeEach
    void setUp() {
        sender = User.builder()
                .id(1L)
                .phoneNumber("01012345678")
                .nickname("sender")
                .build();
                
        receiver = User.builder()
                .id(2L)
                .phoneNumber("01087654321")
                .nickname("receiver")
                .build();

        testChatRoom = ChatRoom.builder()
                .id(1L)
                .seller(sender)
                .buyer(receiver)
                .build();

        testMessage = ChatMessage.builder()
                .id(1L)
                .chatRoom(testChatRoom)
                .sender(sender)
                .content("테스트 메시지")
                .deliveryStatus(MessageDeliveryStatus.SENT)
                .build();
    }

    @Test
    @DisplayName("메시지가 성공적으로 전송되면 SENT 상태로 업데이트")
    void should_UpdateMessageToSent_When_MessageSentSuccessfully() {
        // given
        given(chatMessageRepository.findById(1L)).willReturn(Optional.of(testMessage));
        given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(testMessage);

        // when
        chatMessageDeliveryService.markAsSent(1L);

        // then
        verify(chatMessageRepository).save(argThat(message -> 
            message.getDeliveryStatus() == MessageDeliveryStatus.SENT &&
            message.getSentAt() != null
        ));
    }

    @Test
    @DisplayName("메시지가 수신자에게 전달되면 DELIVERED 상태로 업데이트")
    void should_UpdateMessageToDelivered_When_MessageDeliveredToReceiver() {
        // given
        testMessage = testMessage.toBuilder()
                .deliveryStatus(MessageDeliveryStatus.SENT)
                .build();
        
        given(chatMessageRepository.findById(1L)).willReturn(Optional.of(testMessage));
        given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(testMessage);

        // when
        chatMessageDeliveryService.markAsDelivered(1L, receiver.getId());

        // then
        verify(chatMessageRepository).save(argThat(message -> 
            message.getDeliveryStatus() == MessageDeliveryStatus.DELIVERED &&
            message.getDeliveredAt() != null
        ));
        
        verify(webSocketMessagingService).notifyMessageDelivered(
            eq(testChatRoom.getId()), 
            eq(1L), 
            eq(receiver.getId())
        );
    }

    @Test
    @DisplayName("메시지가 수신자에 의해 읽혀지면 READ 상태로 업데이트")
    void should_UpdateMessageToRead_When_MessageReadByReceiver() {
        // given
        testMessage = testMessage.toBuilder()
                .deliveryStatus(MessageDeliveryStatus.DELIVERED)
                .build();
        
        given(chatMessageRepository.findById(1L)).willReturn(Optional.of(testMessage));
        given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(testMessage);

        // when
        chatMessageDeliveryService.markAsRead(1L, receiver.getId());

        // then
        verify(chatMessageRepository).save(argThat(message -> 
            message.getDeliveryStatus() == MessageDeliveryStatus.READ &&
            message.getReadAt() != null
        ));
        
        verify(webSocketMessagingService).notifyMessageRead(
            eq(testChatRoom.getId()), 
            eq(1L), 
            eq(receiver.getId())
        );
    }

    @Test
    @DisplayName("발신자는 자신의 메시지를 READ로 표시할 수 없음")
    void should_ThrowException_When_SenderTriesToMarkAsRead() {
        // given
        given(chatMessageRepository.findById(1L)).willReturn(Optional.of(testMessage));

        // when & then
        assertThatThrownBy(() -> 
            chatMessageDeliveryService.markAsRead(1L, sender.getId())
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessage("발신자는 자신의 메시지를 읽음 처리할 수 없습니다");
    }

    @Test
    @DisplayName("존재하지 않는 메시지 ID로 상태 업데이트 시도시 예외 발생")
    void should_ThrowException_When_MessageNotFound() {
        // given
        given(chatMessageRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> 
            chatMessageDeliveryService.markAsDelivered(999L, receiver.getId())
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessage("메시지를 찾을 수 없습니다: 999");
    }

    @Test
    @DisplayName("SENT -> DELIVERED -> READ 순서로만 상태 변경 가능")
    void should_FollowCorrectStatusTransition() {
        // given - READ 상태인 메시지를 DELIVERED로 되돌리려 시도
        testMessage = testMessage.toBuilder()
                .deliveryStatus(MessageDeliveryStatus.READ)
                .build();
        
        given(chatMessageRepository.findById(1L)).willReturn(Optional.of(testMessage));

        // when & then
        assertThatThrownBy(() -> 
            chatMessageDeliveryService.markAsDelivered(1L, receiver.getId())
        ).isInstanceOf(IllegalStateException.class)
         .hasMessage("메시지 상태를 되돌릴 수 없습니다: READ -> DELIVERED");
    }
}