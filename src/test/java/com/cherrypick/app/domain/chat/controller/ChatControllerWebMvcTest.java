package com.cherrypick.app.domain.chat.controller;

import com.cherrypick.app.domain.chat.dto.request.SendMessageRequest;
import com.cherrypick.app.domain.chat.dto.response.ChatMessageResponse;
import com.cherrypick.app.domain.chat.dto.response.ChatRoomListResponse;
import com.cherrypick.app.domain.chat.dto.response.ChatRoomResponse;
import com.cherrypick.app.domain.chat.enums.ChatRoomStatus;
import com.cherrypick.app.domain.chat.enums.MessageType;
import com.cherrypick.app.domain.chat.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
 * 채팅 컨트롤러 간단 테스트 (WebMvcTest)
 */
@WebMvcTest(ChatController.class)
@DisplayName("채팅 컨트롤러 WebMvc 테스트")
class ChatControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "1")
    @DisplayName("내 채팅방 목록 조회 - Mock 테스트")
    void getMyChatRooms_MockTest() throws Exception {
        // given
        ChatRoomListResponse response = ChatRoomListResponse.builder()
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
                .lastMessageAt(LocalDateTime.now())
                .unreadCount(3)
                .createdAt(LocalDateTime.now())
                .build();
        
        List<ChatRoomListResponse> chatRooms = Arrays.asList(response);
        given(chatService.getMyChatRooms(1L, null)).willReturn(chatRooms);

        // when & then
        mockMvc.perform(get("/api/chat/rooms/my")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].auctionTitle").value("테스트 상품"));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("메시지 전송 - Mock 테스트")
    void sendMessage_MockTest() throws Exception {
        // given
        ChatMessageResponse response = ChatMessageResponse.builder()
                .id(1L)
                .chatRoomId(1L)
                .senderId(1L)
                .senderName("구매자")
                .content("안녕하세요!")
                .messageType(MessageType.TEXT)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        SendMessageRequest request = new SendMessageRequest("안녕하세요!", MessageType.TEXT);
        given(chatService.sendMessage(eq(1L), eq(1L), any(SendMessageRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/chat/rooms/1/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.content").value("안녕하세요!"));
    }
}