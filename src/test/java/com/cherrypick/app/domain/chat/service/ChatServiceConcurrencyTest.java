package com.cherrypick.app.domain.chat.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.chat.dto.request.SendMessageRequest;
import com.cherrypick.app.domain.chat.dto.response.ChatMessageResponse;
import com.cherrypick.app.domain.chat.entity.ChatMessage;
import com.cherrypick.app.domain.chat.entity.ChatRoom;
import com.cherrypick.app.domain.chat.enums.ChatRoomStatus;
import com.cherrypick.app.domain.chat.enums.MessageType;
import com.cherrypick.app.domain.chat.repository.ChatMessageRepository;
import com.cherrypick.app.domain.chat.repository.ChatRoomRepository;
import com.cherrypick.app.domain.websocket.service.WebSocketMessagingService;
import com.cherrypick.app.domain.connection.entity.ConnectionService;
import com.cherrypick.app.domain.connection.enums.ConnectionStatus;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * 채팅 서비스 동시성 테스트
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("채팅 서비스 동시성 테스트")
class ChatServiceConcurrencyTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;
    
    @Mock
    private ChatMessageRepository chatMessageRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private WebSocketMessagingService webSocketMessagingService;
    
    @InjectMocks
    private ChatService chatService;

    private User user1;
    private User user2;
    private Auction auction;
    private ConnectionService connectionService;
    private ChatRoom chatRoom;
    private ChatMessage message;
    
    @BeforeEach
    void setUp() {
        // 테스트 데이터 설정
        user1 = User.builder()
                .id(1L)
                .nickname("사용자1")
                .email("user1@test.com")
                .build();
                
        user2 = User.builder()
                .id(2L)
                .nickname("사용자2")
                .email("user2@test.com")
                .build();
                
        auction = mock(Auction.class);
        given(auction.getId()).willReturn(1L);
        given(auction.getTitle()).willReturn("테스트 상품");
        given(auction.getDescription()).willReturn("테스트 설명");
        given(auction.getSeller()).willReturn(user1);
                
        connectionService = ConnectionService.builder()
                .id(1L)
                .auction(auction)
                .seller(user1)
                .buyer(user2)
                .finalPrice(BigDecimal.valueOf(15000L))
                .status(ConnectionStatus.ACTIVE)
                .build();
                
        chatRoom = ChatRoom.builder()
                .id(1L)
                .auction(auction)
                .seller(user1)
                .buyer(user2)
                .connectionService(connectionService)
                .status(ChatRoomStatus.ACTIVE)
                .build();
                
        message = ChatMessage.builder()
                .id(1L)
                .chatRoom(chatRoom)
                .sender(user1)
                .content("동시성 테스트 메시지")
                .messageType(MessageType.TEXT)
                .isRead(false)
                .build();
    }

    @Test
    @DisplayName("동시 메시지 전송 - Race Condition 방지 테스트")
    void concurrentMessageSending_PreventRaceCondition() throws InterruptedException {
        // given
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        // Mock 설정
        given(chatRoomRepository.findById(1L)).willReturn(Optional.of(chatRoom));
        given(userRepository.findById(anyLong())).willReturn(Optional.of(user1));
        given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(message);
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(chatRoom);
        willDoNothing().given(webSocketMessagingService).sendChatMessage(anyLong(), any(ChatMessageResponse.class));
        
        // when - 동시에 여러 스레드에서 메시지 전송
        for (int i = 0; i < threadCount; i++) {
            final int messageIndex = i;
            executorService.submit(() -> {
                try {
                    SendMessageRequest request = new SendMessageRequest("메시지 " + messageIndex, MessageType.TEXT);
                    ChatMessageResponse response = chatService.sendMessage(1L, 1L, request);
                    
                    assertThat(response).isNotNull();
                    assertThat(response.getContent()).isEqualTo("동시성 테스트 메시지");
                    
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // then
        boolean finished = latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();
        
        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(errorCount.get()).isEqualTo(0);
        
        // 메시지 저장과 채팅방 업데이트가 정확히 threadCount번 호출되었는지 확인
        verify(chatMessageRepository, times(threadCount)).save(any(ChatMessage.class));
        verify(chatRoomRepository, times(threadCount)).save(any(ChatRoom.class));
        verify(webSocketMessagingService, times(threadCount)).sendChatMessage(eq(1L), any(ChatMessageResponse.class));
    }

    @Test
    @DisplayName("동시 메시지 전송 - 채팅방별 격리 테스트")
    void concurrentMessageSending_IsolationBetweenChatRooms() throws InterruptedException {
        // given
        int threadCount = 20; // 각 채팅방당 10개씩
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // 두 번째 채팅방 설정
        ChatRoom chatRoom2 = ChatRoom.builder()
                .id(2L)
                .auction(auction)
                .seller(user1)
                .buyer(user2)
                .connectionService(connectionService)
                .status(ChatRoomStatus.ACTIVE)
                .build();
        
        // Mock 설정
        given(chatRoomRepository.findById(1L)).willReturn(Optional.of(chatRoom));
        given(chatRoomRepository.findById(2L)).willReturn(Optional.of(chatRoom2));
        given(userRepository.findById(anyLong())).willReturn(Optional.of(user1));
        given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(message);
        given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(invocation -> invocation.getArgument(0));
        willDoNothing().given(webSocketMessagingService).sendChatMessage(anyLong(), any(ChatMessageResponse.class));
        
        // when - 두 채팅방에 동시에 메시지 전송
        for (int i = 0; i < threadCount; i++) {
            final int messageIndex = i;
            final Long roomId = (i % 2 == 0) ? 1L : 2L; // 번갈아가며 다른 채팅방에 전송
            
            executorService.submit(() -> {
                try {
                    SendMessageRequest request = new SendMessageRequest("메시지 " + messageIndex, MessageType.TEXT);
                    ChatMessageResponse response = chatService.sendMessage(roomId, 1L, request);
                    
                    assertThat(response).isNotNull();
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // then
        boolean finished = latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();
        
        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(threadCount);
        
        // 각 채팅방별로 정확한 횟수만큼 호출되었는지 확인
        verify(chatMessageRepository, times(threadCount)).save(any(ChatMessage.class));
        verify(chatRoomRepository, times(threadCount)).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("WebSocket 전송 실패 시에도 메시지 저장은 성공")
    void messageStoredEvenWhenWebSocketFails() throws InterruptedException {
        // given
        int threadCount = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // Mock 설정 - WebSocket 전송 실패
        given(chatRoomRepository.findById(1L)).willReturn(Optional.of(chatRoom));
        given(userRepository.findById(anyLong())).willReturn(Optional.of(user1));
        given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(message);
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(chatRoom);
        willThrow(new RuntimeException("WebSocket 연결 실패"))
                .given(webSocketMessagingService).sendChatMessage(anyLong(), any(ChatMessageResponse.class));
        
        // when
        for (int i = 0; i < threadCount; i++) {
            final int messageIndex = i;
            executorService.submit(() -> {
                try {
                    SendMessageRequest request = new SendMessageRequest("메시지 " + messageIndex, MessageType.TEXT);
                    ChatMessageResponse response = chatService.sendMessage(1L, 1L, request);
                    
                    // WebSocket 전송이 실패해도 메시지 응답은 정상적으로 반환되어야 함
                    assertThat(response).isNotNull();
                    assertThat(response.getContent()).isEqualTo("동시성 테스트 메시지");
                    
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // then
        boolean finished = latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();
        
        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(threadCount);
        
        // 메시지 저장은 성공해야 함
        verify(chatMessageRepository, times(threadCount)).save(any(ChatMessage.class));
        verify(chatRoomRepository, times(threadCount)).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("Lock 객체 관리 - 메모리 효율성 테스트")
    void lockObjectManagement_MemoryEfficiency() {
        // given
        Long roomId1 = 1L;
        Long roomId2 = 2L;
        
        // when
        Object lock1a = chatService.getChatRoomLock(roomId1);
        Object lock1b = chatService.getChatRoomLock(roomId1); // 같은 채팅방
        Object lock2 = chatService.getChatRoomLock(roomId2);  // 다른 채팅방
        
        // then
        assertThat(lock1a).isSameAs(lock1b); // 같은 채팅방은 동일한 Lock 객체
        assertThat(lock1a).isNotSameAs(lock2); // 다른 채팅방은 다른 Lock 객체
        
        // Lock 객체 정리 테스트
        chatService.removeChatRoomLock(roomId1);
        Object newLock1 = chatService.getChatRoomLock(roomId1);
        assertThat(lock1a).isNotSameAs(newLock1); // 정리 후 새로운 Lock 객체 생성
    }
}