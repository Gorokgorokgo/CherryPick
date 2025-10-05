package com.cherrypick.app.domain.chat.controller;

import com.cherrypick.app.domain.chat.dto.request.CreateChatRoomRequest;
import com.cherrypick.app.domain.chat.dto.request.SendMessageRequest;
import com.cherrypick.app.domain.chat.dto.response.ChatMessageResponse;
import com.cherrypick.app.domain.chat.dto.response.ChatRoomListResponse;
import com.cherrypick.app.domain.chat.dto.response.ChatRoomResponse;
import com.cherrypick.app.domain.chat.service.ChatService;
import com.cherrypick.app.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 채팅 관련 REST API 컨트롤러
 * 
 * 주요 기능:
 * - 채팅방 목록 조회
 * - 채팅 메시지 조회/전송
 * - 메시지 읽음 처리
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "채팅 API")
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    /**
     * 채팅방 생성 (경매 낙찰 후)
     *
     * @param request 채팅방 생성 요청
     * @param userDetails 인증된 사용자 정보
     * @return 생성된 채팅방 정보
     */
    @PostMapping("/rooms")
    @Operation(summary = "채팅방 생성", description = "경매 낙찰 후 판매자와 낙찰자 간 채팅방을 생성합니다")
    public ResponseEntity<ChatRoomResponse> createChatRoom(
            @Valid @RequestBody CreateChatRoomRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = userService.getUserIdByEmail(userDetails.getUsername());

        log.info("채팅방 생성 요청: auctionId={}, sellerId={}, buyerId={}, requestUserId={}",
                request.getAuctionId(), request.getSellerId(), request.getBuyerId(), userId);

        ChatRoomResponse chatRoom = chatService.createAuctionChatRoomFromRequest(request, userId);

        return ResponseEntity.ok(chatRoom);
    }

    /**
     * 내 채팅방 목록 조회
     *
     * @param userDetails 인증된 사용자 정보
     * @param status 채팅방 상태 필터 (optional): active, inactive, closed
     * @return 채팅방 목록
     */
    @GetMapping("/rooms/my")
    @Operation(summary = "내 채팅방 목록 조회", description = "사용자의 채팅방 목록을 조회합니다")
    public ResponseEntity<List<ChatRoomListResponse>> getMyChatRooms(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String status) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        
        log.info("채팅방 목록 조회 요청: userId={}, status={}", userId, status);
        
        List<ChatRoomListResponse> chatRooms = chatService.getMyChatRooms(userId, status);
        
        return ResponseEntity.ok(chatRooms);
    }

    /**
     * 채팅방 상세 정보 조회
     * 
     * @param roomId 채팅방 ID
     * @param userDetails 인증된 사용자 정보
     * @return 채팅방 상세 정보
     */
    @GetMapping("/rooms/{roomId}")
    @Operation(summary = "채팅방 상세 조회", description = "특정 채팅방의 상세 정보를 조회합니다")
    public ResponseEntity<ChatRoomResponse> getChatRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        
        log.info("채팅방 상세 조회: roomId={}, userId={}", roomId, userId);
        
        ChatRoomResponse chatRoom = chatService.getChatRoomDetails(roomId, userId);
        
        return ResponseEntity.ok(chatRoom);
    }

    /**
     * 채팅 메시지 목록 조회 (페이지네이션)
     * 
     * @param roomId 채팅방 ID
     * @param userDetails 인증된 사용자 정보
     * @param pageable 페이지네이션 정보
     * @return 메시지 목록
     */
    @GetMapping("/rooms/{roomId}/messages")
    @Operation(summary = "채팅 메시지 조회", description = "채팅방의 메시지 목록을 조회합니다")
    public ResponseEntity<Page<ChatMessageResponse>> getChatMessages(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        
        log.info("채팅 메시지 조회: roomId={}, userId={}, page={}", 
                roomId, userId, pageable.getPageNumber());
        
        Page<ChatMessageResponse> messages = chatService.getChatMessages(roomId, userId, pageable);
        
        return ResponseEntity.ok(messages);
    }

    /**
     * 채팅 메시지 전송
     * 
     * @param roomId 채팅방 ID
     * @param request 메시지 전송 요청
     * @param userDetails 인증된 사용자 정보
     * @return 전송된 메시지 정보
     */
    @PostMapping("/rooms/{roomId}/messages")
    @Operation(summary = "채팅 메시지 전송", description = "채팅방에 메시지를 전송합니다")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable Long roomId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        
        log.info("메시지 전송: roomId={}, userId={}, content={}", 
                roomId, userId, request.getContent().substring(0, Math.min(50, request.getContent().length())));
        
        ChatMessageResponse message = chatService.sendMessage(roomId, userId, request);
        
        return ResponseEntity.ok(message);
    }

    /**
     * 메시지 읽음 처리
     * 
     * @param roomId 채팅방 ID
     * @param messageId 메시지 ID
     * @param userDetails 인증된 사용자 정보
     * @return 성공 응답
     */
    @PutMapping("/rooms/{roomId}/messages/{messageId}/read")
    @Operation(summary = "메시지 읽음 처리", description = "특정 메시지를 읽음으로 표시합니다")
    public ResponseEntity<Void> markMessageAsRead(
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        
        log.info("메시지 읽음 처리: roomId={}, messageId={}, userId={}", roomId, messageId, userId);
        
        chatService.markMessageAsRead(roomId, messageId, userId);
        
        return ResponseEntity.ok().build();
    }

    /**
     * 채팅방의 모든 메시지 읽음 처리
     * 
     * @param roomId 채팅방 ID
     * @param userDetails 인증된 사용자 정보
     * @return 성공 응답
     */
    @PutMapping("/rooms/{roomId}/messages/read-all")
    @Operation(summary = "채팅방 전체 메시지 읽음 처리", description = "채팅방의 모든 미읽은 메시지를 읽음으로 표시합니다")
    public ResponseEntity<Void> markAllMessagesAsRead(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        
        log.info("전체 메시지 읽음 처리: roomId={}, userId={}", roomId, userId);
        
        chatService.markAllMessagesAsRead(roomId, userId);
        
        return ResponseEntity.ok().build();
    }

    /**
     * 채팅방 나가기 (비활성화)
     * 
     * @param roomId 채팅방 ID
     * @param userDetails 인증된 사용자 정보
     * @return 성공 응답
     */
    @PostMapping("/rooms/{roomId}/leave")
    @Operation(summary = "채팅방 나가기", description = "채팅방을 나가고 비활성화합니다")
    public ResponseEntity<Void> leaveChatRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        
        log.info("채팅방 나가기: roomId={}, userId={}", roomId, userId);
        
        chatService.leaveChatRoom(roomId, userId);
        
        return ResponseEntity.ok().build();
    }

    /**
     * 읽지 않은 메시지 총 개수 조회
     * 
     * @param userDetails 인증된 사용자 정보
     * @return 읽지 않은 메시지 개수
     */
    @GetMapping("/unread-count")
    @Operation(summary = "읽지 않은 메시지 개수 조회", description = "사용자의 전체 읽지 않은 메시지 개수를 조회합니다")
    public ResponseEntity<Integer> getUnreadMessageCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        
        log.debug("읽지 않은 메시지 개수 조회: userId={}", userId);
        
        int unreadCount = chatService.getUnreadMessageCount(userId);
        
        return ResponseEntity.ok(unreadCount);
    }
}