package com.cherrypick.app.domain.chat.controller;

import com.cherrypick.app.domain.chat.dto.request.SendMessageRequest;
import com.cherrypick.app.domain.chat.dto.response.ChatMessageResponse;
import com.cherrypick.app.domain.chat.dto.response.ChatRoomListResponse;
import com.cherrypick.app.domain.chat.dto.response.ChatRoomResponse;
import com.cherrypick.app.domain.chat.enums.ChatRoomStatus;
import com.cherrypick.app.domain.chat.enums.MessageType;
import com.cherrypick.app.domain.chat.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 채팅 컨트롤러 테스트
 */
@WebMvcTest(ChatController.class)
@DisplayName("채팅 컨트롤러 테스트")
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @Autowired
    private ObjectMapper objectMapper;

    private ChatRoomListResponse chatRoomListResponse;
    private ChatRoomResponse chatRoomResponse;
    private ChatMessageResponse chatMessageResponse;
    private SendMessageRequest sendMessageRequest;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        
        chatRoomListResponse = ChatRoomListResponse.builder()
                .id(1L)
                .auctionId(1L)
                .auctionTitle("테스트 상품")
                .category("전자제품")
                .finalPrice(100000L)
                .partnerId(2L)
                .partnerName("테스트사용자")
                .partnerType("seller")
                .isOnline(true)
                .status(ChatRoomStatus.ACTIVE)
                .lastMessage("안녕하세요!")
                .lastMessageAt(now)
                .unreadCount(3)
                .createdAt(now)
                .build();

        chatRoomResponse = ChatRoomResponse.builder()
                .id(1L)
                .auctionId(1L)
                .auctionTitle("테스트 상품")
                .auctionDescription("테스트 상품 설명")
                .category("전자제품")
                .finalPrice(100000L)
                .connectionServiceId(1L)
                .sellerId(2L)
                .sellerName("판매자")
                .buyerId(1L)
                .buyerName("구매자")
                .partnerId(2L)
                .partnerName("판매자")
                .partnerType("seller")
                .partnerOnline(true)
                .status(ChatRoomStatus.ACTIVE)
                .createdAt(now)
                .unreadCount(3)
                .build();

        chatMessageResponse = ChatMessageResponse.builder()
                .id(1L)
                .chatRoomId(1L)
                .senderId(1L)
                .senderName("구매자")
                .content("안녕하세요!")
                .messageType(MessageType.TEXT)
                .isRead(false)
                .createdAt(now)
                .build();

        sendMessageRequest = new SendMessageRequest("안녕하세요!", MessageType.TEXT);
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("내 채팅방 목록 조회 - 성공")
    void getMyChatRooms_Success() throws Exception {
        // given
        List<ChatRoomListResponse> chatRooms = Arrays.asList(chatRoomListResponse);
        given(chatService.getMyChatRooms(1L, null)).willReturn(chatRooms);

        // when & then
        mockMvc.perform(get("/api/chat/rooms/my")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].auctionTitle").value("테스트 상품"))
                .andExpect(jsonPath("$[0].partnerName").value("테스트사용자"))
                .andExpect(jsonPath("$[0].unreadCount").value(3))
                .andExpect(jsonPath("$[0].lastMessage").value("안녕하세요!"));

        verify(chatService).getMyChatRooms(1L, null);
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("내 채팅방 목록 조회 (상태 필터) - 성공")
    void getMyChatRooms_WithStatusFilter_Success() throws Exception {
        // given
        List<ChatRoomListResponse> activeChatRooms = Arrays.asList(chatRoomListResponse);
        given(chatService.getMyChatRooms(1L, "active")).willReturn(activeChatRooms);

        // when & then
        mockMvc.perform(get("/api/chat/rooms/my")
                        .param("status", "active")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        verify(chatService).getMyChatRooms(1L, "active");
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("채팅방 상세 조회 - 성공")
    void getChatRoom_Success() throws Exception {
        // given
        given(chatService.getChatRoomDetails(1L, 1L)).willReturn(chatRoomResponse);

        // when & then
        mockMvc.perform(get("/api/chat/rooms/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.auctionTitle").value("테스트 상품"))
                .andExpect(jsonPath("$.partnerName").value("판매자"))
                .andExpect(jsonPath("$.unreadCount").value(3));

        verify(chatService).getChatRoomDetails(1L, 1L);
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("채팅 메시지 목록 조회 - 성공")
    void getChatMessages_Success() throws Exception {
        // given
        Pageable pageable = PageRequest.of(0, 50);
        List<ChatMessageResponse> messages = Arrays.asList(chatMessageResponse);
        Page<ChatMessageResponse> messagePage = new PageImpl<>(messages, pageable, 1);
        
        given(chatService.getChatMessages(eq(1L), eq(1L), any(Pageable.class)))
                .willReturn(messagePage);

        // when & then
        mockMvc.perform(get("/api/chat/rooms/1/messages")
                        .param("page", "0")
                        .param("size", "50")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].content").value("안녕하세요!"))
                .andExpect(jsonPath("$.content[0].senderName").value("구매자"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(chatService).getChatMessages(eq(1L), eq(1L), any(Pageable.class));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("채팅 메시지 전송 - 성공")
    void sendMessage_Success() throws Exception {
        // given
        given(chatService.sendMessage(eq(1L), eq(1L), any(SendMessageRequest.class)))
                .willReturn(chatMessageResponse);

        // when & then
        mockMvc.perform(post("/api/chat/rooms/1/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendMessageRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.content").value("안녕하세요!"))
                .andExpect(jsonPath("$.senderName").value("구매자"))
                .andExpect(jsonPath("$.messageType").value("TEXT"));

        verify(chatService).sendMessage(eq(1L), eq(1L), any(SendMessageRequest.class));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("메시지 전송 - 유효성 검증 실패 (빈 내용)")
    void sendMessage_ValidationFail_EmptyContent() throws Exception {
        // given
        SendMessageRequest invalidRequest = new SendMessageRequest("", MessageType.TEXT);

        // when & then
        mockMvc.perform(post("/api/chat/rooms/1/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(chatService, never()).sendMessage(anyLong(), anyLong(), any(SendMessageRequest.class));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("메시지 전송 - 유효성 검증 실패 (내용 길이 초과)")
    void sendMessage_ValidationFail_ContentTooLong() throws Exception {
        // given
        String longContent = "a".repeat(1001); // 1000자 초과
        SendMessageRequest invalidRequest = new SendMessageRequest(longContent, MessageType.TEXT);

        // when & then
        mockMvc.perform(post("/api/chat/rooms/1/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(chatService, never()).sendMessage(anyLong(), anyLong(), any(SendMessageRequest.class));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("메시지 읽음 처리 - 성공")
    void markMessageAsRead_Success() throws Exception {
        // given
        willDoNothing().given(chatService).markMessageAsRead(1L, 1L, 1L);

        // when & then
        mockMvc.perform(put("/api/chat/rooms/1/messages/1/read")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(chatService).markMessageAsRead(1L, 1L, 1L);
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("전체 메시지 읽음 처리 - 성공")
    void markAllMessagesAsRead_Success() throws Exception {
        // given
        willDoNothing().given(chatService).markAllMessagesAsRead(1L, 1L);

        // when & then
        mockMvc.perform(put("/api/chat/rooms/1/messages/read-all")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(chatService).markAllMessagesAsRead(1L, 1L);
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("채팅방 나가기 - 성공")
    void leaveChatRoom_Success() throws Exception {
        // given
        willDoNothing().given(chatService).leaveChatRoom(1L, 1L);

        // when & then
        mockMvc.perform(post("/api/chat/rooms/1/leave")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(chatService).leaveChatRoom(1L, 1L);
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("읽지 않은 메시지 개수 조회 - 성공")
    void getUnreadMessageCount_Success() throws Exception {
        // given
        given(chatService.getUnreadMessageCount(1L)).willReturn(5);

        // when & then
        mockMvc.perform(get("/api/chat/unread-count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));

        verify(chatService).getUnreadMessageCount(1L);
    }

    @Test
    @DisplayName("인증되지 않은 사용자 접근 - 실패")
    void unauthenticatedAccess_Fail() throws Exception {
        // when & then
        mockMvc.perform(get("/api/chat/rooms/my")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(chatService, never()).getMyChatRooms(anyLong(), anyString());
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("존재하지 않는 채팅방 조회 - 실패")
    void getChatRoom_NotFound() throws Exception {
        // given
        given(chatService.getChatRoomDetails(999L, 1L))
                .willThrow(new RuntimeException("채팅방을 찾을 수 없습니다"));

        // when & then
        mockMvc.perform(get("/api/chat/rooms/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(chatService).getChatRoomDetails(999L, 1L);
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("잘못된 경로 변수 - 실패")
    void invalidPathVariable_Fail() throws Exception {
        // when & then
        mockMvc.perform(get("/api/chat/rooms/invalid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(chatService, never()).getChatRoomDetails(anyLong(), anyLong());
    }
}