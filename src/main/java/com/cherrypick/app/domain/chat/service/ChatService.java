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
import com.cherrypick.app.domain.chat.entity.ChatRoomParticipant;
import com.cherrypick.app.domain.chat.enums.ChatRoomStatus;
import com.cherrypick.app.domain.chat.enums.MessageType;
import com.cherrypick.app.domain.chat.repository.ChatMessageRepository;
import com.cherrypick.app.domain.chat.repository.ChatRoomParticipantRepository;
import com.cherrypick.app.domain.chat.repository.ChatRoomRepository;
import com.cherrypick.app.domain.websocket.service.WebSocketMessagingService;
import com.cherrypick.app.domain.connection.entity.ConnectionService;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.notification.service.FcmService;
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
import com.cherrypick.app.domain.transaction.entity.Transaction;
import com.cherrypick.app.domain.transaction.enums.TransactionStatus;
import com.cherrypick.app.domain.transaction.repository.TransactionRepository;

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
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final WebSocketMessagingService webSocketMessagingService;
    private final UserOnlineStatusService userOnlineStatusService;
    private final ApplicationEventPublisher eventPublisher;
    private final BidRepository bidRepository;
    private final FcmService fcmService;
    private final TransactionRepository transactionRepository;

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
     * - 나간 채팅방은 목록에서 제외됨
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
                // 나간 채팅방 필터링
                .filter(chatRoom -> !hasUserLeftChatRoom(chatRoom.getId(), userId))
                .map(chatRoom -> {
                    // 마지막 메시지 조회 (이미지 메시지인 경우 "사진"으로 표시)
                    String lastMessage = chatMessageRepository
                            .findLatestMessageByChatRoomId(chatRoom.getId())
                            .map(msg -> {
                                if (msg.getMessageType() == MessageType.IMAGE) {
                                    return "사진";
                                }
                                return msg.getContent();
                            })
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
     * 사용자가 채팅방을 나갔는지 확인
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 나간 경우 true
     */
    private boolean hasUserLeftChatRoom(Long chatRoomId, Long userId) {
        return chatRoomParticipantRepository.hasUserLeftChatRoom(chatRoomId, userId);
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

        // 거래 상태 조회
        String transactionStatus = getTransactionStatusByAuctionId(chatRoom.getAuction().getId());

        return ChatRoomResponse.from(chatRoom, userId, unreadCount, partnerOnline, transactionStatus);
    }

    /**
     * 경매 ID로 거래 상태 조회
     *
     * @param auctionId 경매 ID
     * @return 거래 상태 문자열 (없으면 null)
     */
    private String getTransactionStatusByAuctionId(Long auctionId) {
        return transactionRepository.findByAuctionId(auctionId)
                .map(transaction -> transaction.getStatus().name())
                .orElse(null);
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
     * - 나간 참여자에게 메시지가 전송되면 자동으로 재입장 처리
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

            // 발신자가 나간 상태였다면 자동 재입장
            rejoinIfLeft(chatRoom, sender);

            // 수신자(상대방)가 나간 상태였다면 자동 재입장
            Long receiverId = chatRoom.getSeller().getId().equals(userId)
                    ? chatRoom.getBuyer().getId()
                    : chatRoom.getSeller().getId();
            User receiver = userRepository.findById(receiverId)
                    .orElseThrow(EntityNotFoundException::user);
            rejoinIfLeft(chatRoom, receiver);

            // 메시지 생성 및 저장
            ChatMessage message = ChatMessage.createTextMessage(chatRoom, sender, request.getContent());
            ChatMessage savedMessage = chatMessageRepository.save(message);
            chatMessageRepository.flush(); // 즉시 DB에 반영

            log.info("💾 [DEBUG] Message saved to DB: messageId={}, roomId={}, senderId={}, content={}",
                    savedMessage.getId(), roomId, userId, request.getContent().substring(0, Math.min(20, request.getContent().length())));

            // 채팅방 마지막 메시지 시간 업데이트 (동시성 보장)
            ChatRoom updatedRoom = chatRoom.updateLastMessageTime();
            chatRoomRepository.save(updatedRoom);
            chatRoomRepository.flush(); // 즉시 DB에 반영

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

            // FCM 푸시 알림 발송 (수신자가 오프라인일 때만)
            try {
                if (!userOnlineStatusService.isUserOnline(receiverId)) {
                    fcmService.sendNewMessageNotification(
                            receiver,
                            roomId,
                            sender.getNickname(),
                            request.getContent()
                    );
                }
            } catch (Exception e) {
                log.warn("FCM 푸시 알림 발송 실패 (메시지는 저장됨): roomId={}, receiverId={}, error={}",
                        roomId, receiverId, e.getMessage());
            }

            return response;
        }
    }

    /**
     * 나간 참여자 자동 재입장 처리
     *
     * @param chatRoom 채팅방
     * @param user 사용자
     */
    private void rejoinIfLeft(ChatRoom chatRoom, User user) {
        chatRoomParticipantRepository.findByChatRoomIdAndUserId(chatRoom.getId(), user.getId())
                .ifPresent(participant -> {
                    if (participant.getIsLeft()) {
                        ChatRoomParticipant rejoinedParticipant = participant.rejoin();
                        chatRoomParticipantRepository.save(rejoinedParticipant);
                        log.info("채팅방 자동 재입장: roomId={}, userId={}", chatRoom.getId(), user.getId());
                    }
                });
    }

    /**
     * 배치 메시지 전송 (여러 이미지 동시 전송용)
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param requests 메시지 전송 요청 목록
     * @return 전송된 메시지 정보 목록
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<ChatMessageResponse> sendBatchMessages(Long roomId, Long userId, List<SendMessageRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

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

            // 모든 메시지 생성 및 저장 (메시지 타입 고려)
            List<ChatMessage> messages = requests.stream()
                    .map(request -> {
                        MessageType messageType = request.getMessageType() != null
                                ? request.getMessageType()
                                : MessageType.TEXT;
                        return ChatMessage.createMessage(chatRoom, sender, request.getContent(), messageType);
                    })
                    .collect(Collectors.toList());

            List<ChatMessage> savedMessages = chatMessageRepository.saveAll(messages);

            // 채팅방 마지막 메시지 시간 업데이트 (한 번만)
            ChatRoom updatedRoom = chatRoom.updateLastMessageTime();
            chatRoomRepository.save(updatedRoom);

            // 실시간 메시지 전송 (WebSocket) - 각 메시지마다
            List<ChatMessageResponse> responses = savedMessages.stream()
                    .map(ChatMessageResponse::from)
                    .collect(Collectors.toList());

            // WebSocket으로 모든 메시지 전송
            responses.forEach(response -> {
                try {
                    log.info("🔔 WebSocket 메시지 전송 시도: roomId={}, messageId={}, messageType={}",
                            roomId, response.getId(), response.getMessageType());
                    webSocketMessagingService.sendChatMessage(roomId, response);
                    log.info("✅ WebSocket 메시지 전송 성공: messageId={}", response.getId());
                } catch (Exception e) {
                    log.warn("❌ WebSocket 메시지 전송 실패 (메시지는 저장됨): roomId={}, messageId={}, error={}",
                            roomId, response.getId(), e.getMessage(), e);
                }
            });

            // 메시지 전송 시 타이핑 상태 자동 중단 이벤트 발행
            try {
                eventPublisher.publishEvent(new TypingEvent(
                        this, roomId, userId, null, TypingEvent.TypingEventType.MESSAGE_SENT
                ));
            } catch (Exception e) {
                log.warn("타이핑 상태 자동 중단 이벤트 발행 실패: roomId={}, userId={}, error={}",
                        roomId, userId, e.getMessage());
            }

            // FCM 푸시 알림 발송 (수신자가 오프라인일 때만, 첫 메시지만)
            try {
                Long receiverId = chatRoom.getSeller().getId().equals(userId)
                        ? chatRoom.getBuyer().getId()
                        : chatRoom.getSeller().getId();
                User receiver = userRepository.findById(receiverId)
                        .orElseThrow(EntityNotFoundException::user);

                if (!userOnlineStatusService.isUserOnline(receiverId) && !responses.isEmpty()) {
                    // 첫 번째 메시지만 푸시 알림으로 발송 (배치의 경우)
                    String previewContent;
                    if (responses.size() > 1) {
                        previewContent = String.format("사진 %d장", responses.size());
                    } else {
                        // 단일 메시지: 이미지인 경우 "사진"으로 표시
                        ChatMessageResponse firstResponse = responses.get(0);
                        if (firstResponse.getMessageType() == MessageType.IMAGE) {
                            previewContent = "사진";
                        } else {
                            previewContent = firstResponse.getContent();
                        }
                    }

                    fcmService.sendNewMessageNotification(
                            receiver,
                            roomId,
                            sender.getNickname(),
                            previewContent
                    );
                }
            } catch (Exception e) {
                log.warn("FCM 푸시 알림 발송 실패 (메시지는 저장됨): roomId={}, error={}",
                        roomId, e.getMessage());
            }

            log.info("배치 메시지 전송 완료: roomId={}, userId={}, messageCount={}",
                    roomId, userId, responses.size());

            return responses;
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
     * - 거래 진행 중(PENDING, SELLER_CONFIRMED, BUYER_CONFIRMED)이면 나가기 불가
     * - 거래 완료/취소 후에만 나가기 가능
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

        // 거래 상태 확인 - 진행 중이면 나가기 불가
        Optional<Transaction> transactionOpt = transactionRepository.findByAuctionId(chatRoom.getAuction().getId());
        if (transactionOpt.isPresent()) {
            TransactionStatus status = transactionOpt.get().getStatus();
            if (isTransactionInProgress(status)) {
                log.warn("거래 진행 중 채팅방 나가기 시도 차단: roomId={}, userId={}, transactionStatus={}",
                        roomId, userId, status);
                throw new BusinessException(ErrorCode.BAD_REQUEST);
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(EntityNotFoundException::user);

        // 마지막 메시지 ID 조회
        Long lastMessageId = chatMessageRepository.findLatestMessageByChatRoomId(roomId)
                .map(ChatMessage::getId)
                .orElse(null);

        // 참여자 정보 조회 또는 생성
        ChatRoomParticipant participant = chatRoomParticipantRepository
                .findByChatRoomIdAndUserId(roomId, userId)
                .orElseGet(() -> {
                    // 기존 채팅방에 참여자 정보가 없으면 새로 생성
                    ChatRoomParticipant newParticipant = ChatRoomParticipant.createParticipant(chatRoom, user);
                    return chatRoomParticipantRepository.save(newParticipant);
                });

        // 이미 나간 상태인지 확인
        if (participant.getIsLeft()) {
            log.info("사용자가 이미 채팅방을 나간 상태입니다. roomId={}, userId={}", roomId, userId);
            return;
        }

        // 참여자 상태를 '나감'으로 변경
        ChatRoomParticipant leftParticipant = participant.leave(lastMessageId);
        chatRoomParticipantRepository.save(leftParticipant);

        log.info("채팅방 나가기 완료: roomId={}, userId={}, lastMessageId={}", roomId, userId, lastMessageId);
    }

    /**
     * 거래가 진행 중인지 확인
     * PENDING, SELLER_CONFIRMED, BUYER_CONFIRMED 상태면 진행 중
     *
     * @param status 거래 상태
     * @return 진행 중이면 true
     */
    private boolean isTransactionInProgress(TransactionStatus status) {
        return status == TransactionStatus.PENDING
                || status == TransactionStatus.SELLER_CONFIRMED
                || status == TransactionStatus.BUYER_CONFIRMED;
    }

    /**
     * 거래가 완료/취소 상태인지 확인
     *
     * @param status 거래 상태
     * @return 완료/취소 상태면 true
     */
    private boolean isTransactionFinished(TransactionStatus status) {
        return status == TransactionStatus.COMPLETED
                || status == TransactionStatus.CANCELLED;
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

    /**
     * 채팅방 재입장 (수동)
     * - 거래 완료/취소 후에는 재입장 불가
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 채팅방 상세 정보
     */
    @Transactional
    public ChatRoomResponse rejoinChatRoom(Long roomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(EntityNotFoundException::chatRoom);

        // 사용자가 채팅방 참여자인지 확인
        if (!chatRoom.isParticipant(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 참여자 정보 조회
        ChatRoomParticipant participant = chatRoomParticipantRepository
                .findByChatRoomIdAndUserId(roomId, userId)
                .orElse(null);

        // 나간 상태였다면 재입장 처리
        if (participant != null && participant.getIsLeft()) {
            // 거래 완료/취소 후에는 재입장 불가
            Optional<Transaction> transactionOpt = transactionRepository.findByAuctionId(chatRoom.getAuction().getId());
            if (transactionOpt.isPresent()) {
                TransactionStatus status = transactionOpt.get().getStatus();
                if (isTransactionFinished(status)) {
                    log.warn("거래 완료/취소 후 채팅방 재입장 시도 차단: roomId={}, userId={}, transactionStatus={}",
                            roomId, userId, status);
                    throw new BusinessException(ErrorCode.BAD_REQUEST);
                }
            }

            ChatRoomParticipant rejoinedParticipant = participant.rejoin();
            chatRoomParticipantRepository.save(rejoinedParticipant);
            log.info("채팅방 수동 재입장 완료: roomId={}, userId={}", roomId, userId);
        }

        // 읽지 않은 메시지 개수
        int unreadCount = chatMessageRepository
                .countUnreadMessagesByChatRoomIdAndUserId(roomId, userId);

        // 상대방 온라인 상태
        Long partnerId = chatRoom.getSeller().getId().equals(userId) ?
                chatRoom.getBuyer().getId() : chatRoom.getSeller().getId();
        boolean partnerOnline = userOnlineStatusService.isUserOnline(partnerId);

        // 거래 상태 조회
        String transactionStatus = getTransactionStatusByAuctionId(chatRoom.getAuction().getId());

        return ChatRoomResponse.from(chatRoom, userId, unreadCount, partnerOnline, transactionStatus);
    }

    /**
     * 경매 ID로 채팅방 조회 및 자동 재입장
     *
     * @param auctionId 경매 ID
     * @param userId 사용자 ID
     * @return 채팅방 상세 정보
     */
    @Transactional
    public ChatRoomResponse getChatRoomByAuctionIdAndRejoin(Long auctionId, Long userId) {
        // 사용자가 참여한 채팅방 조회 (판매자 또는 구매자)
        List<ChatRoom> chatRooms = chatRoomRepository.findByUserId(userId);

        ChatRoom chatRoom = chatRooms.stream()
                .filter(cr -> cr.getAuction().getId().equals(auctionId))
                .findFirst()
                .orElseThrow(EntityNotFoundException::chatRoom);

        // 재입장 처리 후 상세 정보 반환
        return rejoinChatRoom(chatRoom.getId(), userId);
    }
}