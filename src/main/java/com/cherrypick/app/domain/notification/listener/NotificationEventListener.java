package com.cherrypick.app.domain.notification.listener;

import com.cherrypick.app.domain.notification.entity.NotificationHistory;
import com.cherrypick.app.domain.notification.entity.NotificationSetting;
import com.cherrypick.app.domain.notification.event.*;
import com.cherrypick.app.domain.notification.enums.NotificationType;
import com.cherrypick.app.domain.notification.repository.NotificationHistoryRepository;
import com.cherrypick.app.domain.notification.repository.NotificationSettingRepository;
import com.cherrypick.app.domain.notification.service.NotificationThrottleService;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import com.cherrypick.app.domain.notification.service.FcmService;
import com.cherrypick.app.domain.websocket.service.WebSocketMessagingService;
import com.cherrypick.app.domain.websocket.dto.AuctionUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 알림 이벤트 리스너
 * 비즈니스 이벤트 발생 시 자동으로 알림 발송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationHistoryRepository notificationHistoryRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final UserRepository userRepository;
    private final WebSocketMessagingService webSocketMessagingService;
    private final NotificationThrottleService throttleService;
    private final FcmService fcmService;

    /**
     * 새로운 입찰 알림 이벤트 처리
     */
    @Async
    @EventListener
    @Transactional
    public void handleNewBidNotification(NewBidNotificationEvent event) {
        processNotificationEvent(event);
    }

    /**
     * 낙찰 알림 이벤트 처리 (구매자용)
     */
    @Async
    @EventListener
    @Transactional
    public void handleAuctionWonNotification(AuctionWonNotificationEvent event) {
        processNotificationEvent(event);
    }

    /**
     * 경매 낙찰 알림 이벤트 처리 (판매자용)
     */
    @Async
    @EventListener
    @Transactional
    public void handleAuctionSoldNotification(AuctionSoldNotificationEvent event) {
        processNotificationEvent(event);
    }

    /**
     * 경매 유찰 알림 이벤트 처리 (판매자용)
     */
    @Async
    @EventListener
    @Transactional
    public void handleAuctionNotSoldNotification(AuctionNotSoldNotificationEvent event) {
        processNotificationEvent(event);
    }

    /**
     * 경매 유찰 알림 이벤트 처리 (최고 입찰자용)
     */
    @Async
    @EventListener
    @Transactional
    public void handleAuctionNotSoldForHighestBidderNotification(AuctionNotSoldForHighestBidderEvent event) {
        processNotificationEvent(event);
    }

    /**
     * 경매 종료 알림 이벤트 처리 (일반 참여자용)
     */
    @Async
    @EventListener
    @Transactional
    public void handleAuctionEndedForParticipantNotification(AuctionEndedForParticipantEvent event) {
        processNotificationEvent(event);
    }

    /**
     * 연결 서비스 결제 요청 알림 이벤트 처리
     */
    @Async
    @EventListener
    @Transactional
    public void handleConnectionPaymentRequestNotification(ConnectionPaymentRequestNotificationEvent event) {
        processNotificationEvent(event);
    }

    /**
     * 채팅 활성화 알림 이벤트 처리
     */
    @Async
    @EventListener
    @Transactional
    public void handleChatActivatedNotification(ChatActivatedNotificationEvent event) {
        processNotificationEvent(event);
    }

    /**
     * 거래 완료 알림 이벤트 처리
     */
    @Async
    @EventListener
    @Transactional
    public void handleTransactionCompletedNotification(TransactionCompletedNotificationEvent event) {
        processNotificationEvent(event);
    }

    /**
     * 거래 확인 대기 알림 이벤트 처리
     */
    @Async
    @EventListener
    @Transactional
    public void handleTransactionPendingNotification(TransactionPendingNotificationEvent event) {
        log.info("🔔 [거래 확인 대기 알림] 사용자 ID: {}, 경매 ID: {}",
                event.getTargetUserId(), event.getResourceId());
        processNotificationEvent(event);
    }

    /**
     * 거래 취소 알림 이벤트 처리
     */
    @Async
    @EventListener
    @Transactional
    public void handleTransactionCancelledNotification(TransactionCancelledNotificationEvent event) {
        log.info("🔔 [거래 취소 알림] 사용자 ID: {}, 경매 ID: {}",
                event.getTargetUserId(), event.getResourceId());
        processNotificationEvent(event);
    }

    /**
     * 계정 복구 알림 이벤트 처리
     */
    @Async
    @EventListener
    @Transactional
    public void handleAccountRestoredNotification(AccountRestoredEvent event) {
        processNotificationEvent(event);
    }

    /**
     * Outbid 알림 이벤트 처리 (이전 최고 입찰자에게)
     * Throttling 적용: 1분 내 중복 발송 방지
     */
    @Async
    @EventListener
    @Transactional
    public void handleOutbidNotification(OutbidNotificationEvent event) {
        log.info("🔔 [Outbid 알림 처리] 사용자 ID: {}, 경매 ID: {}",
                event.getTargetUserId(), event.getAuctionId());

        // Throttle 확인
        if (!throttleService.canSendOutbidNotification(event.getTargetUserId(), event.getAuctionId())) {
            // Throttled - 카운트만 증가
            int count = throttleService.incrementOutbidCount(event.getTargetUserId(), event.getAuctionId());
            log.info("  - ⏳ [Throttled] 누적 입찰 수: {}", count);
            return;
        }

        // 누적된 카운트 확인
        int accumulatedCount = throttleService.getOutbidCount(event.getTargetUserId(), event.getAuctionId());
        if (accumulatedCount > 0) {
            // 그룹 알림 발송 후 카운트 리셋
            throttleService.resetOutbidCount(event.getTargetUserId(), event.getAuctionId());
        }

        processNotificationEvent(event);
    }

    /**
     * 경매 마감 임박 알림 이벤트 처리
     */
    @Async
    @EventListener
    @Transactional
    public void handleAuctionEndingSoonNotification(AuctionEndingSoonEvent event) {
        String type = event.getMinutesRemaining() == 15 ? "15m" : "5m";
        log.info("🔔 [마감 임박 알림 처리] 사용자 ID: {}, 경매 ID: {}, 남은 시간: {}분",
                event.getTargetUserId(), event.getAuctionId(), event.getMinutesRemaining());

        // Throttle 확인 (같은 경매에 대해 중복 알림 방지)
        if (!throttleService.canSendEndingSoonNotification(event.getTargetUserId(), event.getAuctionId(), type)) {
            log.info("  - ⏳ [Throttled] 이미 {}분 전 알림 발송됨", event.getMinutesRemaining());
            return;
        }

        processNotificationEvent(event);
    }

    /**
     * 키워드 알림 이벤트 처리
     */
    @Async
    @EventListener
    @Transactional
    public void handleKeywordAlertNotification(KeywordAlertEvent event) {
        log.info("🔔 [키워드 알림 처리] 사용자 ID: {}, 경매 ID: {}, 키워드: {}",
                event.getTargetUserId(), event.getAuctionId(), event.getMatchedKeyword());

        // Throttle 확인 (같은 경매에 대해 중복 알림 방지)
        if (!throttleService.canSendKeywordNotification(event.getTargetUserId(), event.getAuctionId())) {
            log.info("  - ⏳ [Throttled] 이미 해당 경매에 대한 키워드 알림 발송됨");
            return;
        }

        processNotificationEvent(event);
    }

    /**
     * 알림 이벤트 공통 처리 로직
     */
    private void processNotificationEvent(NotificationEvent event) {
        log.info("🔔 [알림 처리 시작] 이벤트: {}, 사용자 ID: {}", event.getClass().getSimpleName(), event.getTargetUserId());
        try {
            // 사용자 조회
            User user = userRepository.findById(event.getTargetUserId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "사용자를 찾을 수 없습니다. userId: " + event.getTargetUserId()));

            // 알림 설정 확인
            NotificationSetting setting = getOrCreateNotificationSetting(user);
            boolean isEnabled = isNotificationEnabled(setting, event.getNotificationType());

            // 계정 복구 알림은 설정과 무관하게 항상 발송 (중요 알림)
            if (event instanceof AccountRestoredEvent) {
                isEnabled = true;
                log.info("  - ⚠️ [강제 발송] 계정 복구 알림은 설정을 무시하고 발송합니다.");
            }

            log.info("  - 알림 타입: {}, 설정 활성화 여부: {}", event.getNotificationType(), isEnabled);

            if (!isEnabled) {
                log.info("  - ⏩ [알림 건너뜀] 사용자 설정이 비활성화되어 있습니다.");
                return;
            }

            // chatRoomId 추출 (경매 낙찰 알림인 경우)
            Long chatRoomId = null;
            if (event instanceof AuctionSoldNotificationEvent) {
                chatRoomId = ((AuctionSoldNotificationEvent) event).getChatRoomId();
            } else if (event instanceof AuctionWonNotificationEvent) {
                chatRoomId = ((AuctionWonNotificationEvent) event).getChatRoomId();
            } else if (event instanceof TransactionPendingNotificationEvent) {
                chatRoomId = ((TransactionPendingNotificationEvent) event).getChatRoomId();
            } else if (event instanceof TransactionCancelledNotificationEvent) {
                chatRoomId = ((TransactionCancelledNotificationEvent) event).getChatRoomId();
            } else if (event instanceof TransactionCompletedNotificationEvent) {
                chatRoomId = ((TransactionCompletedNotificationEvent) event).getChatRoomId();
            }

            // 알림 히스토리 저장 (chatRoomId 포함)
            NotificationHistory notification;
            if (chatRoomId != null) {
                notification = NotificationHistory.createNotificationWithChatRoom(
                        user, event.getNotificationType(), event.getTitle(),
                        event.getMessage(), event.getResourceId(), chatRoomId);
            } else {
                notification = NotificationHistory.createNotification(
                        user, event.getNotificationType(), event.getTitle(),
                        event.getMessage(), event.getResourceId());
            }
            notificationHistoryRepository.save(notification);
            log.info("  - 💾 [알림 저장 완료] ID: {}", notification.getId());

            // FCM 푸시 알림 발송 (Deep Link 포함)
            sendFcmNotification(setting.getFcmToken(), event.getTitle(), event.getMessage(),
                    notification, event.getNotificationType(), event.getResourceId());

            // WebSocket 실시간 알림 발송
            sendWebSocketNotification(user.getId(), event);
            log.info("  - 🚀 [알림 발송 완료] 사용자 ID: {}", user.getId());

        } catch (Exception e) {
            log.error("  - ❌ [알림 처리 오류] 이벤트: {}, 오류: {}",
                    event.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    /**
     * 알림 설정 확인
     */
    private boolean isNotificationEnabled(NotificationSetting setting, NotificationType type) {
        return switch (type) {
            case NEW_BID -> setting.getBidNotification();
            case AUCTION_WON -> setting.getWinningNotification(); // 구매자용 낙찰 알림
            case AUCTION_SOLD -> setting.getBidNotification(); // 판매자용 낙찰 알림 (입찰 관련 알림으로 처리)
            case AUCTION_NOT_SOLD -> setting.getBidNotification(); // 유찰 알림 (판매자용)
            case AUCTION_NOT_SOLD_HIGHEST_BIDDER -> setting.getWinningNotification(); // 유찰 알림 (최고 입찰자용)
            case AUCTION_ENDED -> setting.getBidNotification(); // 경매 종료 알림 (일반 참여자)
            case AUCTION_EXTENDED -> setting.getBidNotification(); // 스나이핑 방지 시간 연장 알림
            case CONNECTION_PAYMENT_REQUEST -> setting.getConnectionPaymentNotification();
            case CHAT_ACTIVATED -> setting.getChatActivationNotification();
            case NEW_MESSAGE -> setting.getMessageNotification();
            case TRANSACTION_COMPLETED, TRANSACTION_PENDING, TRANSACTION_CANCELLED -> setting.getTransactionCompletionNotification();
            case PROMOTION -> setting.getPromotionNotification();
            case OUTBID -> setting.getOutbidNotification(); // 더 높은 입찰 알림
            case AUCTION_ENDING_SOON_15M, AUCTION_ENDING_SOON_5M -> setting.getEndingSoonNotification(); // 마감 임박 알림
            case KEYWORD_ALERT -> setting.getKeywordNotification(); // 키워드 알림
        };
    }

    /**
     * FCM 푸시 알림 발송
     */
    private void sendFcmNotification(String fcmToken, String title, String message,
                                      NotificationHistory notification, NotificationType type, Long resourceId) {
        if (fcmToken == null || fcmToken.isEmpty()) {
            log.debug("  - ⚠️ [FCM 건너뜀] FCM 토큰이 없습니다.");
            return;
        }

        try {
            // Deep Link 라우트 결정
            String deepLinkRoute = determineDeepLinkRoute(type);

            // 실제 FCM 발송 (FcmService 사용)
            fcmService.sendFcmPushWithDeepLink(
                    fcmToken, title, message, notification,
                    resourceId, type.name(), deepLinkRoute);

            log.debug("  - 📱 [FCM 발송 완료] type: {}, route: {}", type, deepLinkRoute);

        } catch (Exception e) {
            log.warn("  - ⚠️ [FCM 발송 실패] error: {}", e.getMessage());
        }
    }

    /**
     * 알림 타입에 따른 Deep Link 라우트 결정
     */
    private String determineDeepLinkRoute(NotificationType type) {
        return switch (type) {
            case NEW_BID, AUCTION_WON, AUCTION_SOLD, AUCTION_NOT_SOLD,
                 AUCTION_NOT_SOLD_HIGHEST_BIDDER, AUCTION_ENDED, AUCTION_EXTENDED,
                 OUTBID, AUCTION_ENDING_SOON_15M, AUCTION_ENDING_SOON_5M, KEYWORD_ALERT
                 -> "/auction/detail";
            case CONNECTION_PAYMENT_REQUEST, CHAT_ACTIVATED, NEW_MESSAGE
                 -> "/chat";
            case TRANSACTION_COMPLETED, TRANSACTION_PENDING, TRANSACTION_CANCELLED -> "/chat";
            case PROMOTION -> "/promotion";
            default -> "/home";
        };
    }

    /**
     * WebSocket 실시간 알림 발송
     */
    private void sendWebSocketNotification(Long userId, NotificationEvent event) {
        try {
            // chatRoomId 추출 (경매 낙찰 또는 거래 관련 알림인 경우)
            Long chatRoomId = null;
            if (event instanceof AuctionSoldNotificationEvent) {
                chatRoomId = ((AuctionSoldNotificationEvent) event).getChatRoomId();
            } else if (event instanceof AuctionWonNotificationEvent) {
                chatRoomId = ((AuctionWonNotificationEvent) event).getChatRoomId();
            } else if (event instanceof TransactionPendingNotificationEvent) {
                chatRoomId = ((TransactionPendingNotificationEvent) event).getChatRoomId();
            } else if (event instanceof TransactionCancelledNotificationEvent) {
                chatRoomId = ((TransactionCancelledNotificationEvent) event).getChatRoomId();
            } else if (event instanceof TransactionCompletedNotificationEvent) {
                chatRoomId = ((TransactionCompletedNotificationEvent) event).getChatRoomId();
            }

            // 유찰 알림의 경우 추가 정보 포함
            Boolean hasHighestBidder = null;
            Long winnerId = null;
            String winnerNickname = null;

            if (event instanceof AuctionNotSoldNotificationEvent) {
                AuctionNotSoldNotificationEvent notSoldEvent = (AuctionNotSoldNotificationEvent) event;
                if (notSoldEvent.getHighestBid() != null) {
                    hasHighestBidder = true;
                    winnerId = notSoldEvent.getHighestBid().getBidder().getId();
                    winnerNickname = notSoldEvent.getHighestBid().getBidder().getNickname();
                } else {
                    hasHighestBidder = false;
                }
            }

            // 프론트엔드 NotificationMessage 형식에 맞춰 JSON 메시지 생성
            NotificationWebSocketMessage wsNotification = NotificationWebSocketMessage.builder()
                    .id(String.valueOf(System.currentTimeMillis())) // 임시 ID (실제로는 NotificationHistory의 ID 사용 가능)
                    .type(event.getNotificationType().name())
                    .title(event.getTitle())
                    .message(event.getMessage())
                    .timestamp(System.currentTimeMillis())
                    .isRead(false)
                    .resourceId(event.getResourceId())
                    .chatRoomId(chatRoomId)
                    .hasHighestBidder(hasHighestBidder)
                    .winnerId(winnerId)
                    .winnerNickname(winnerNickname)
                    .build();

            webSocketMessagingService.sendNotificationToUser(userId, wsNotification);

        } catch (Exception e) {
            // WebSocket 실시간 알림 발송 실패 무시
        }
    }

    /**
     * WebSocket 알림 메시지 DTO (프론트엔드 NotificationMessage와 동일 구조)
     */
    @lombok.Builder
    @lombok.Getter
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    private static class NotificationWebSocketMessage {
        private String id;
        private String type;
        private String title;
        private String message;
        private long timestamp;
        private boolean isRead;
        private Long resourceId;
        private Long chatRoomId;
        private Boolean hasHighestBidder;
        private Long winnerId;
        private String winnerNickname;
    }

    /**
     * 알림 설정 조회 또는 생성
     */
    private NotificationSetting getOrCreateNotificationSetting(User user) {
        return notificationSettingRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    NotificationSetting setting = NotificationSetting.createDefaultSetting(user);
                    return notificationSettingRepository.save(setting);
                });
    }
}