package com.cherrypick.app.domain.chat.service;

import com.cherrypick.app.common.exception.EntityNotFoundException;
import com.cherrypick.app.common.exception.BusinessException;
import com.cherrypick.app.common.exception.ErrorCode;
import com.cherrypick.app.domain.chat.dto.request.CreateChatRoomRequest;
import com.cherrypick.app.domain.chat.dto.request.SendMessageRequest;
import com.cherrypick.app.domain.chat.dto.response.ChatMessageResponse;
import com.cherrypick.app.domain.chat.dto.response.ChatRoomListResponse;
import com.cherrypick.app.domain.chat.dto.response.ChatRoomResponse;
import com.cherrypick.app.domain.chat.entity.ChatMessage;
import com.cherrypick.app.domain.chat.entity.ChatRoom;
import com.cherrypick.app.domain.chat.enums.ChatRoomStatus;
import com.cherrypick.app.domain.chat.repository.ChatMessageRepository;
import com.cherrypick.app.domain.chat.repository.ChatRoomRepository;
import com.cherrypick.app.domain.websocket.service.WebSocketMessagingService;
import com.cherrypick.app.domain.connection.entity.ConnectionService;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.cherrypick.app.domain.websocket.event.TypingEvent;
import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.bid.repository.BidRepository;
import com.cherrypick.app.domain.auction.entity.Auction;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 채팅 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final WebSocketMessagingService webSocketMessagingService;
    private final UserOnlineStatusService userOnlineStatusService;
    private final ApplicationEventPublisher eventPublisher;
    private final BidRepository bidRepository;

    // 채팅방별 동시성 제어를 위한 Lock 객체 캐시
    private final ConcurrentHashMap<Long, Object> chatRoomLocks = new ConcurrentHashMap<>();

    /**
     * 연결 서비스 기반 채팅방 생성
     * 
     * @param connectionService 연결 서비스
     * @return 생성된 채팅방
     */
    @Transactional
    public ChatRoom createChatRoom(ConnectionService connectionService) {
        // 기존 채팅방 확인
        Optional<ChatRoom> existingRoom = chatRoomRepository
                .findByConnectionServiceId(connectionService.getId());
        
        if (existingRoom.isPresent()) {
            // 채팅방 이미 존재
            return existingRoom.get();
        }
        
        // 새 채팅방 생성
        ChatRoom chatRoom = ChatRoom.createChatRoom(
                connectionService.getAuction(),
                connectionService.getSeller(),
                connectionService.getBuyer(),
                connectionService
        );
        
        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
        
        log.info("채팅방이 생성되었습니다. roomId: {}, connectionId: {}", 
                savedRoom.getId(), connectionService.getId());
        
        return savedRoom;
    }
    
    /**
     * 채팅방 활성화 (연결 서비스 결제 완료 시)
     * 
     * @param connectionService 연결 서비스
     * @return 활성화된 채팅방
     */
    @Transactional
    public ChatRoom activateChatRoom(ConnectionService connectionService) {
        // 채팅방 조회 또는 생성
        ChatRoom chatRoom = chatRoomRepository
                .findByConnectionServiceId(connectionService.getId())
                .orElseGet(() -> createChatRoom(connectionService));
        
        // 채팅방 활성화 (불변 객체 패턴)
        if (!chatRoom.isActive()) {
            ChatRoom activatedRoom = chatRoom.activateChatRoom();
            chatRoom = chatRoomRepository.save(activatedRoom);
            
            // 채팅방 활성화 완료
        }
        
        return chatRoom;
    }
    
    /**
     * 경매 낙찰 시 채팅방 생성
     *
     * @param auction 경매 정보
     * @param seller 판매자
     * @param winner 낙찰자
     * @return 생성된 채팅방
     */
    @Transactional
    public ChatRoom createAuctionChatRoom(Auction auction, User seller, User winner) {
        // 동일한 경매에 대한 채팅방이 이미 존재하는지 확인
        Optional<ChatRoom> existingRoom = chatRoomRepository
                .findByAuctionIdAndSellerIdAndBuyerId(auction.getId(), seller.getId(), winner.getId());

        if (existingRoom.isPresent()) {
            log.info("경매 채팅방이 이미 존재합니다. auctionId: {}, sellerId: {}, winnerId: {}",
                    auction.getId(), seller.getId(), winner.getId());
            return existingRoom.get();
        }

        // 새 채팅방 생성 (경매 기반)
        ChatRoom chatRoom = ChatRoom.createAuctionChatRoom(auction, seller, winner);

        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);

        log.info("경매 채팅방이 생성되었습니다. roomId: {}, auctionId: {}, sellerId: {}, winnerId: {}",
                savedRoom.getId(), auction.getId(), seller.getId(), winner.getId());

        return savedRoom;
    }

    /**
     * 유찰 경매용 채팅방 생성 (최고입찰자와 판매자)
     *
     * @param auctionId 경매 ID
     * @param sellerId 판매자 ID (요청자)
     * @return 채팅방 응답
     */
    @Transactional
    public ChatRoomResponse createFailedAuctionChatRoom(Long auctionId, Long sellerId) {
        // 경매 조회
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(EntityNotFoundException::auction);

        // 판매자 조회 및 권한 확인
        User seller = userRepository.findById(sellerId)
                .orElseThrow(EntityNotFoundException::user);

        if (!auction.getSeller().getId().equals(sellerId)) {
            log.error("유찰 경매 채팅방 생성 권한 없음: requestUserId={}, auctionSellerId={}",
                    sellerId, auction.getSeller().getId());
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 최고입찰자 조회
        Optional<Bid> topBidOpt =
                bidRepository.findTopByAuctionIdOrderByBidAmountDesc(auctionId);

        if (topBidOpt.isEmpty() || topBidOpt.get().getBidAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            log.error("최고입찰자를 찾을 수 없습니다: auctionId={}", auctionId);
            throw new BusinessException(ErrorCode.BID_NOT_FOUND);
        }

        User buyer = topBidOpt.get().getBidder();

        // 기존 채팅방 확인
        Optional<ChatRoom> existingRoom = chatRoomRepository.findByAuctionIdAndSellerIdAndBuyerId(
                auctionId, sellerId, buyer.getId());

        if (existingRoom.isPresent()) {
            ChatRoom chatRoom = existingRoom.get();
            int unreadCount = getUnreadCount(chatRoom.getId(), sellerId);
            boolean partnerOnline = userOnlineStatusService.isUserOnline(buyer.getId());
            return ChatRoomResponse.from(chatRoom, sellerId, unreadCount, partnerOnline);
        }

        // 새 채팅방 생성
        ChatRoom chatRoom = createAuctionChatRoom(auction, seller, buyer);

        log.info("유찰 경매 채팅방이 생성되었습니다. roomId: {}, auctionId: {}, sellerId: {}, buyerId: {}",
                chatRoom.getId(), auctionId, sellerId, buyer.getId());

        return ChatRoomResponse.from(chatRoom, sellerId, 0,
                userOnlineStatusService.isUserOnline(buyer.getId()));
    }

    /**
     * 채팅방의 읽지 않은 메시지 개수 조회
     */
    private int getUnreadCount(Long chatRoomId, Long userId) {
        return chatMessageRepository.countUnreadMessagesByChatRoomIdAndUserId(chatRoomId, userId);
    }

    /**
     * 경매 채팅방 생성 (REST API 요청용)
     *
     * @param request 채팅방 생성 요청
     * @param requestUserId 요청한 사용자 ID
     * @return 채팅방 응답
     */
    @Transactional
    public ChatRoomResponse createAuctionChatRoomFromRequest(CreateChatRoomRequest request, Long requestUserId) {
        // 경매 조회
        Auction auction = auctionRepository.findById(request.getAuctionId())
                .orElseThrow(EntityNotFoundException::auction);

        // 판매자 조회
        User seller = userRepository.findById(request.getSellerId())
                .orElseThrow(EntityNotFoundException::user);

        // 구매자 조회
        User buyer = userRepository.findById(request.getBuyerId())
                .orElseThrow(EntityNotFoundException::user);

        // 요청자가 판매자 또는 구매자인지 확인
        if (!requestUserId.equals(seller.getId()) && !requestUserId.equals(buyer.getId())) {
            log.error("채팅방 생성 권한 없음: requestUserId={}, sellerId={}, buyerId={}",
                    requestUserId, seller.getId(), buyer.getId());
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 채팅방 생성
        ChatRoom chatRoom = createAuctionChatRoom(auction, seller, buyer);

        // 응답 생성
        int unreadCount = 0; // 새로 생성된 채팅방이므로 0
        Long partnerId = requestUserId.equals(seller.getId()) ? buyer.getId() : seller.getId();
        boolean partnerOnline = userOnlineStatusService.isUserOnline(partnerId);

        return ChatRoomResponse.from(chatRoom, requestUserId, unreadCount, partnerOnline);
    }
    
    /**
     * 채팅방 조회 (연결 서비스 기반)
     * 
     * @param connectionServiceId 연결 서비스 ID
     * @return 채팅방 (옵셔널)
     */
    public Optional<ChatRoom> getChatRoomByConnectionId(Long connectionServiceId) {
        return chatRoomRepository.findByConnectionServiceId(connectionServiceId);
    }
    
    /**
     * 채팅방 종료 (거래 완료 시)
     * 
     * @param connectionService 연결 서비스
     */
    @Transactional
    public void closeChatRoom(ConnectionService connectionService) {
        chatRoomRepository.findByConnectionServiceId(connectionService.getId())
                .ifPresent(chatRoom -> {
                    ChatRoom closedRoom = chatRoom.closeChatRoom();
                    chatRoomRepository.save(closedRoom);
                    
                    log.info("채팅방이 종료되었습니다. roomId: {}, connectionId: {}", 
                            closedRoom.getId(), connectionService.getId());
                });
    }

    /**
     * 내 채팅방 목록 조회
     * 
     * @param userId 사용자 ID
     * @param status 채팅방 상태 필터 (optional)
     * @return 채팅방 목록
     */
    public List<ChatRoomListResponse> getMyChatRooms(Long userId, String status) {
        List<ChatRoom> chatRooms;
        
        if (status != null) {
            ChatRoomStatus roomStatus = ChatRoomStatus.valueOf(status.toUpperCase());
            chatRooms = chatRoomRepository.findByUserIdAndStatus(userId, roomStatus);
        } else {
            chatRooms = chatRoomRepository.findByUserId(userId);
        }
        
        return chatRooms.stream()
                .map(chatRoom -> {
                    // 마지막 메시지 조회
                    String lastMessage = chatMessageRepository
                            .findLatestMessageByChatRoomId(chatRoom.getId())
                            .map(ChatMessage::getContent)
                            .orElse("");
                    
                    // 읽지 않은 메시지 개수
                    int unreadCount = chatMessageRepository
                            .countUnreadMessagesByChatRoomIdAndUserId(chatRoom.getId(), userId);
                    
                    // 상대방 온라인 상태 (실시간 상태 추적)
                    Long partnerId = chatRoom.getSeller().getId().equals(userId) ? 
                            chatRoom.getBuyer().getId() : chatRoom.getSeller().getId();
                    boolean partnerOnline = userOnlineStatusService.isUserOnline(partnerId);
                    
                    return ChatRoomListResponse.from(chatRoom, userId, lastMessage, unreadCount, partnerOnline);
                })
                .collect(Collectors.toList());
    }

    /**
     * 채팅방 상세 정보 조회
     * 
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 채팅방 상세 정보
     */
    public ChatRoomResponse getChatRoomDetails(Long roomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(EntityNotFoundException::chatRoom);
        
        // 사용자가 채팅방 참여자인지 확인
        if (!chatRoom.isParticipant(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        
        // 읽지 않은 메시지 개수
        int unreadCount = chatMessageRepository
                .countUnreadMessagesByChatRoomIdAndUserId(roomId, userId);
        
        // 상대방 온라인 상태 (실시간 상태 추적)
        Long partnerId = chatRoom.getSeller().getId().equals(userId) ? 
                chatRoom.getBuyer().getId() : chatRoom.getSeller().getId();
        boolean partnerOnline = userOnlineStatusService.isUserOnline(partnerId);
        
        return ChatRoomResponse.from(chatRoom, userId, unreadCount, partnerOnline);
    }

    /**
     * 채팅 메시지 목록 조회
     * 
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param pageable 페이지네이션 정보
     * @return 메시지 목록
     */
    public Page<ChatMessageResponse> getChatMessages(Long roomId, Long userId, Pageable pageable) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(EntityNotFoundException::chatRoom);
        
        // 사용자가 채팅방 참여자인지 확인
        if (!chatRoom.isParticipant(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        
        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomId(roomId, pageable);
        
        return messages.map(ChatMessageResponse::from);
    }

    /**
     * 채팅 메시지 전송 (동시성 제어 적용)
     * 
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param request 메시지 전송 요청
     * @return 전송된 메시지 정보
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ChatMessageResponse sendMessage(Long roomId, Long userId, SendMessageRequest request) {
        // 채팅방별 동시성 제어
        Object lock = getChatRoomLock(roomId);
        
        synchronized (lock) {
            ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(EntityNotFoundException::chatRoom);
            
            // 사용자가 채팅방 참여자인지 확인
            if (!chatRoom.isParticipant(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            
            // 채팅방이 활성화되어 있는지 확인
            if (!chatRoom.isActive()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST);
            }
            
            User sender = userRepository.findById(userId)
                    .orElseThrow(EntityNotFoundException::user);
            
            // 메시지 생성 및 저장
            ChatMessage message = ChatMessage.createTextMessage(chatRoom, sender, request.getContent());
            ChatMessage savedMessage = chatMessageRepository.save(message);
            
            // 채팅방 마지막 메시지 시간 업데이트 (동시성 보장)
            ChatRoom updatedRoom = chatRoom.updateLastMessageTime();
            chatRoomRepository.save(updatedRoom);
            
            // 실시간 메시지 전송 (WebSocket)
            ChatMessageResponse response = ChatMessageResponse.from(savedMessage);
            
            // WebSocket 전송은 동기화 블록 밖에서 수행 (성능 최적화)
            try {
                webSocketMessagingService.sendChatMessage(roomId, response);
            } catch (Exception e) {
                log.warn("WebSocket 메시지 전송 실패 (메시지는 저장됨): roomId={}, messageId={}, error={}", 
                        roomId, savedMessage.getId(), e.getMessage());
            }
            
            // 메시지 전송 시 타이핑 상태 자동 중단 이벤트 발행
            try {
                eventPublisher.publishEvent(new TypingEvent(
                    this, roomId, userId, null, TypingEvent.TypingEventType.MESSAGE_SENT
                ));
            } catch (Exception e) {
                log.warn("타이핑 상태 자동 중단 이벤트 발행 실패: roomId={}, userId={}, error={}", 
                        roomId, userId, e.getMessage());
            }
            
            // 메시지 전송 완료
            
            return response;
        }
    }
    
    /**
     * 채팅방별 Lock 객체 획득 (메모리 효율적인 Lock 관리)
     * 
     * @param roomId 채팅방 ID
     * @return Lock 객체
     */
    public Object getChatRoomLock(Long roomId) {
        return chatRoomLocks.computeIfAbsent(roomId, k -> new Object());
    }
    
    /**
     * 사용하지 않는 Lock 객체 정리 (메모리 누수 방지)
     * 
     * @param roomId 채팅방 ID
     */
    public void removeChatRoomLock(Long roomId) {
        chatRoomLocks.remove(roomId);
        log.debug("채팅방 Lock 객체 정리: roomId={}", roomId);
    }

    /**
     * 메시지 읽음 처리
     * 
     * @param roomId 채팅방 ID
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     */
    @Transactional
    public void markMessageAsRead(Long roomId, Long messageId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(EntityNotFoundException::chatRoom);
        
        // 사용자가 채팅방 참여자인지 확인
        if (!chatRoom.isParticipant(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        
        int updatedCount = chatMessageRepository.markMessageAsRead(messageId, userId);
        
        if (updatedCount > 0) {
            log.debug("메시지 읽음 처리: roomId={}, messageId={}, userId={}", roomId, messageId, userId);
        }
    }

    /**
     * 채팅방의 모든 메시지 읽음 처리
     * 
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     */
    @Transactional
    public void markAllMessagesAsRead(Long roomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(EntityNotFoundException::chatRoom);
        
        // 사용자가 채팅방 참여자인지 확인
        if (!chatRoom.isParticipant(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        
        int updatedCount = chatMessageRepository.markAllMessagesAsReadInChatRoom(roomId, userId);
        
        // 전체 메시지 읽음 처리
    }

    /**
     * 채팅방 나가기
     * 
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     */
    @Transactional
    public void leaveChatRoom(Long roomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(EntityNotFoundException::chatRoom);
        
        // 사용자가 채팅방 참여자인지 확인
        if (!chatRoom.isParticipant(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        
        // 채팅방 비활성화 처리 (추후 구현 필요)
        // 현재는 로그만 남김
        // 채팅방 나가기
    }

    /**
     * 읽지 않은 메시지 총 개수 조회
     * 
     * @param userId 사용자 ID
     * @return 읽지 않은 메시지 개수
     */
    public int getUnreadMessageCount(Long userId) {
        return chatMessageRepository.countUnreadMessagesByUserId(userId);
    }
}