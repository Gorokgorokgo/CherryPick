package com.cherrypick.app.domain.chat.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.chat.dto.request.SendMessageRequest;
import com.cherrypick.app.domain.chat.dto.response.ChatMessageResponse;
import com.cherrypick.app.domain.chat.dto.response.ChatRoomListResponse;
import com.cherrypick.app.domain.chat.dto.response.ChatRoomResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * 채팅 서비스 테스트
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("채팅 서비스 테스트")
class ChatServiceTest {

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

    private User seller;
    private User buyer;
    private Auction auction;
    private ConnectionService connectionService;
    private ChatRoom chatRoom;
    private ChatMessage message;
    
    @BeforeEach
    void setUp() {
        // 테스트 데이터 설정
        seller = User.builder()
                .id(1L)
                .nickname("판매자")
                .email("seller@test.com")
                .build();
                
        buyer = User.builder()
                .id(2L)
                .nickname("구매자")
                .email("buyer@test.com")
                .build();
                
        auction = mock(Auction.class);
        given(auction.getId()).willReturn(1L);
        given(auction.getTitle()).willReturn("테스트 상품");
        given(auction.getDescription()).willReturn("테스트 설명");
        given(auction.getSeller()).willReturn(seller);
                
        connectionService = ConnectionService.builder()
                .id(1L)
                .auction(auction)
                .seller(seller)
                .buyer(buyer)
                .finalPrice(BigDecimal.valueOf(15000L))
                .status(ConnectionStatus.ACTIVE)
                .build();
                
        chatRoom = ChatRoom.builder()
                .id(1L)
                .auction(auction)
                .seller(seller)
                .buyer(buyer)
                .connectionService(connectionService)
                .status(ChatRoomStatus.ACTIVE)
                .build();
                
        message = ChatMessage.builder()
                .id(1L)
                .chatRoom(chatRoom)
                .sender(buyer)
                .content("안녕하세요!")
                .messageType(MessageType.TEXT)
                .isRead(false)
                .build();
    }

    @Test
    @DisplayName("채팅방 생성 - 성공")
    void createChatRoom_Success() {
        // given
        given(chatRoomRepository.findByConnectionServiceId(connectionService.getId()))
                .willReturn(Optional.empty());
        given(chatRoomRepository.save(any(ChatRoom.class)))
                .willReturn(chatRoom);
        
        // when
        ChatRoom result = chatService.createChatRoom(connectionService);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.getAuction()).isEqualTo(auction);
        assertThat(result.getSeller()).isEqualTo(seller);
        assertThat(result.getBuyer()).isEqualTo(buyer);
        
        verify(chatRoomRepository).findByConnectionServiceId(connectionService.getId());
        verify(chatRoomRepository).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("채팅방 생성 - 이미 존재하는 경우")
    void createChatRoom_AlreadyExists() {
        // given
        given(chatRoomRepository.findByConnectionServiceId(connectionService.getId()))
                .willReturn(Optional.of(chatRoom));
        
        // when
        ChatRoom result = chatService.createChatRoom(connectionService);
        
        // then
        assertThat(result).isEqualTo(chatRoom);
        
        verify(chatRoomRepository).findByConnectionServiceId(connectionService.getId());
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("내 채팅방 목록 조회 - 성공")
    void getMyChatRooms_Success() {
        // given
        List<ChatRoom> chatRooms = Arrays.asList(chatRoom);
        given(chatRoomRepository.findByUserId(buyer.getId()))
                .willReturn(chatRooms);
        given(chatMessageRepository.findLatestMessageByChatRoomId(chatRoom.getId()))
                .willReturn(Optional.of(message));
        given(chatMessageRepository.countUnreadMessagesByChatRoomIdAndUserId(chatRoom.getId(), buyer.getId()))
                .willReturn(3);
        
        // when
        List<ChatRoomListResponse> result = chatService.getMyChatRooms(buyer.getId(), null);
        
        // then
        assertThat(result).hasSize(1);
        
        ChatRoomListResponse response = result.get(0);
        assertThat(response.getId()).isEqualTo(chatRoom.getId());
        assertThat(response.getPartnerName()).isEqualTo("판매자");
        assertThat(response.getPartnerType()).isEqualTo("seller");
        assertThat(response.getLastMessage()).isEqualTo("안녕하세요!");
        assertThat(response.getUnreadCount()).isEqualTo(3);
        
        verify(chatRoomRepository).findByUserId(buyer.getId());
        verify(chatMessageRepository).findLatestMessageByChatRoomId(chatRoom.getId());
        verify(chatMessageRepository).countUnreadMessagesByChatRoomIdAndUserId(chatRoom.getId(), buyer.getId());
    }

    @Test
    @DisplayName("채팅 메시지 전송 - 성공")
    void sendMessage_Success() {
        // given
        SendMessageRequest request = new SendMessageRequest("새로운 메시지", MessageType.TEXT);
        
        given(chatRoomRepository.findById(chatRoom.getId()))
                .willReturn(Optional.of(chatRoom));
        given(userRepository.findById(buyer.getId()))
                .willReturn(Optional.of(buyer));
        given(chatMessageRepository.save(any(ChatMessage.class)))
                .willReturn(message);
        given(chatRoomRepository.save(any(ChatRoom.class)))
                .willReturn(chatRoom);
        
        // when
        ChatMessageResponse result = chatService.sendMessage(chatRoom.getId(), buyer.getId(), request);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.getSenderId()).isEqualTo(buyer.getId());
        assertThat(result.getSenderName()).isEqualTo(buyer.getNickname());
        assertThat(result.getContent()).isEqualTo(message.getContent());
        
        verify(chatRoomRepository).findById(chatRoom.getId());
        verify(userRepository).findById(buyer.getId());
        verify(chatMessageRepository).save(any(ChatMessage.class));
        verify(webSocketMessagingService).sendChatMessage(eq(chatRoom.getId()), any(ChatMessageResponse.class));
    }

    @Test
    @DisplayName("채팅 메시지 조회 - 성공")
    void getChatMessages_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        List<ChatMessage> messages = Arrays.asList(message);
        Page<ChatMessage> messagePage = new PageImpl<>(messages, pageable, 1);
        
        given(chatRoomRepository.findById(chatRoom.getId()))
                .willReturn(Optional.of(chatRoom));
        given(chatMessageRepository.findByChatRoomId(chatRoom.getId(), pageable))
                .willReturn(messagePage);
        
        // when
        Page<ChatMessageResponse> result = chatService.getChatMessages(chatRoom.getId(), buyer.getId(), pageable);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        
        ChatMessageResponse response = result.getContent().get(0);
        assertThat(response.getId()).isEqualTo(message.getId());
        assertThat(response.getContent()).isEqualTo(message.getContent());
        assertThat(response.getSenderId()).isEqualTo(buyer.getId());
        
        verify(chatRoomRepository).findById(chatRoom.getId());
        verify(chatMessageRepository).findByChatRoomId(chatRoom.getId(), pageable);
    }

    @Test
    @DisplayName("메시지 읽음 처리 - 성공")
    void markMessageAsRead_Success() {
        // given
        given(chatRoomRepository.findById(chatRoom.getId()))
                .willReturn(Optional.of(chatRoom));
        given(chatMessageRepository.markMessageAsRead(message.getId(), seller.getId()))
                .willReturn(1);
        
        // when
        chatService.markMessageAsRead(chatRoom.getId(), message.getId(), seller.getId());
        
        // then
        verify(chatRoomRepository).findById(chatRoom.getId());
        verify(chatMessageRepository).markMessageAsRead(message.getId(), seller.getId());
    }

    @Test
    @DisplayName("읽지 않은 메시지 개수 조회 - 성공")
    void getUnreadMessageCount_Success() {
        // given
        given(chatMessageRepository.countUnreadMessagesByUserId(buyer.getId()))
                .willReturn(5);
        
        // when
        int result = chatService.getUnreadMessageCount(buyer.getId());
        
        // then
        assertThat(result).isEqualTo(5);
        
        verify(chatMessageRepository).countUnreadMessagesByUserId(buyer.getId());
    }

    @Test
    @DisplayName("채팅방 활성화 - 성공")
    void activateChatRoom_Success() {
        // given
        ChatRoom inactiveChatRoom = ChatRoom.builder()
                .id(1L)
                .auction(auction)
                .seller(seller)
                .buyer(buyer)
                .connectionService(connectionService)
                .status(ChatRoomStatus.INACTIVE)
                .build();
                
        ChatRoom activatedChatRoom = ChatRoom.builder()
                .id(1L)
                .auction(auction)
                .seller(seller)
                .buyer(buyer)
                .connectionService(connectionService)
                .status(ChatRoomStatus.ACTIVE)
                .build();
        
        given(chatRoomRepository.findByConnectionServiceId(connectionService.getId()))
                .willReturn(Optional.of(inactiveChatRoom));
        given(chatRoomRepository.save(any(ChatRoom.class)))
                .willReturn(activatedChatRoom);
        
        // when
        ChatRoom result = chatService.activateChatRoom(connectionService);
        
        // then
        assertThat(result.isActive()).isTrue();
        
        verify(chatRoomRepository).findByConnectionServiceId(connectionService.getId());
        verify(chatRoomRepository).save(any(ChatRoom.class));
    }
}