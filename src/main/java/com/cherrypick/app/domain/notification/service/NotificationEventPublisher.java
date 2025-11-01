package com.cherrypick.app.domain.notification.service;

import com.cherrypick.app.domain.notification.event.*;
import com.cherrypick.app.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * 알림 이벤트 발행 서비스
 * 비즈니스 로직에서 알림 이벤트를 쉽게 발행할 수 있도록 하는 파사드 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 새로운 입찰 알림 이벤트 발행
     *
     * @param sellerId 판매자 ID
     * @param auctionId 경매 ID
     * @param auctionTitle 경매 제목
     * @param bidAmount 입찰 금액
     * @param bidderNickname 입찰자 닉네임
     */
    public void publishNewBidNotification(Long sellerId, Long auctionId, String auctionTitle,
                                        Long bidAmount, String bidderNickname) {
        try {
            NewBidNotificationEvent event = new NewBidNotificationEvent(
                    this, sellerId, auctionId, auctionTitle, bidAmount, bidderNickname);

            eventPublisher.publishEvent(event);

            log.info("새로운 입찰 알림 이벤트 발행. sellerId: {}, auctionId: {}, bidAmount: {}",
                    sellerId, auctionId, bidAmount);

        } catch (Exception e) {
            log.error("새로운 입찰 알림 이벤트 발행 실패. sellerId: {}, auctionId: {}, error: {}",
                    sellerId, auctionId, e.getMessage(), e);
        }
    }

    /**
     * 낙찰 알림 이벤트 발행
     *
     * @param buyerId 구매자 ID
     * @param auctionId 경매 ID
     * @param auctionTitle 경매 제목
     * @param finalPrice 최종 낙찰가
     */
    public void publishAuctionWonNotification(Long buyerId, Long auctionId, String auctionTitle,
                                            Long finalPrice) {
        try {
            AuctionWonNotificationEvent event = new AuctionWonNotificationEvent(
                    this, buyerId, auctionId, auctionTitle, finalPrice, null);

            eventPublisher.publishEvent(event);

            log.info("낙찰 알림 이벤트 발행. buyerId: {}, auctionId: {}, finalPrice: {}",
                    buyerId, auctionId, finalPrice);

        } catch (Exception e) {
            log.error("낙찰 알림 이벤트 발행 실패. buyerId: {}, auctionId: {}, error: {}",
                    buyerId, auctionId, e.getMessage(), e);
        }
    }

    /**
     * 연결 서비스 결제 요청 알림 이벤트 발행
     *
     * @param sellerId 판매자 ID
     * @param connectionId 연결 서비스 ID
     * @param auctionTitle 경매 제목
     */
    public void publishConnectionPaymentRequestNotification(Long sellerId, Long connectionId,
                                                          String auctionTitle) {
        try {
            ConnectionPaymentRequestNotificationEvent event = new ConnectionPaymentRequestNotificationEvent(
                    this, sellerId, connectionId, auctionTitle);

            eventPublisher.publishEvent(event);

            log.info("연결 서비스 결제 요청 알림 이벤트 발행. sellerId: {}, connectionId: {}",
                    sellerId, connectionId);

        } catch (Exception e) {
            log.error("연결 서비스 결제 요청 알림 이벤트 발행 실패. sellerId: {}, connectionId: {}, error: {}",
                    sellerId, connectionId, e.getMessage(), e);
        }
    }

    /**
     * 채팅 활성화 알림 이벤트 발행
     *
     * @param buyerId 구매자 ID
     * @param chatRoomId 채팅방 ID
     * @param auctionTitle 경매 제목
     */
    public void publishChatActivatedNotification(Long buyerId, Long chatRoomId, String auctionTitle) {
        try {
            ChatActivatedNotificationEvent event = new ChatActivatedNotificationEvent(
                    this, buyerId, chatRoomId, auctionTitle);

            eventPublisher.publishEvent(event);

            log.info("채팅 활성화 알림 이벤트 발행. buyerId: {}, chatRoomId: {}",
                    buyerId, chatRoomId);

        } catch (Exception e) {
            log.error("채팅 활성화 알림 이벤트 발행 실패. buyerId: {}, chatRoomId: {}, error: {}",
                    buyerId, chatRoomId, e.getMessage(), e);
        }
    }

    /**
     * 거래 완료 알림 이벤트 발행
     *
     * @param userId 사용자 ID
     * @param connectionId 연결 서비스 ID
     * @param auctionTitle 경매 제목
     * @param isSeller 판매자 여부
     */
    public void publishTransactionCompletedNotification(Long userId, Long connectionId,
                                                      String auctionTitle, boolean isSeller) {
        try {
            TransactionCompletedNotificationEvent event = new TransactionCompletedNotificationEvent(
                    this, userId, connectionId, auctionTitle, isSeller);

            eventPublisher.publishEvent(event);

            log.info("거래 완료 알림 이벤트 발행. userId: {}, connectionId: {}, isSeller: {}",
                    userId, connectionId, isSeller);

        } catch (Exception e) {
            log.error("거래 완료 알림 이벤트 발행 실패. userId: {}, connectionId: {}, error: {}",
                    userId, connectionId, e.getMessage(), e);
        }
    }

    /**
     * 거래 확인 알림 이벤트 발행 (상대방에게)
     *
     * @param userId 사용자 ID (알림 받을 사람)
     * @param auctionTitle 경매 제목
     * @param confirmerRole 확인한 사람의 역할 (판매자/구매자)
     */
    public void publishTransactionConfirmedNotification(Long userId, String auctionTitle, String confirmerRole) {
        try {
            // 기존 TransactionCompletedNotificationEvent를 재사용하거나 새로운 이벤트 생성 가능
            // 여기서는 간단하게 일반 알림으로 처리
            log.info("거래 확인 알림 발행. userId: {}, auctionTitle: {}, confirmer: {}",
                    userId, auctionTitle, confirmerRole);

            // TODO: 필요시 별도 TransactionConfirmedNotificationEvent 생성
            // 현재는 로깅만 수행

        } catch (Exception e) {
            log.error("거래 확인 알림 이벤트 발행 실패. userId: {}, error: {}",
                    userId, e.getMessage(), e);
        }
    }

    /**
     * 후기 수신 알림 발행
     *
     * @param reviewee 후기 대상자
     * @param reviewerNickname 후기 작성자 닉네임
     */
    public void sendReviewReceivedNotification(User reviewee, String reviewerNickname) {
        try {
            log.info("후기 수신 알림 발행. revieweeId: {}, reviewer: {}",
                    reviewee.getId(), reviewerNickname);

            // TODO: 필요시 ReviewReceivedNotificationEvent 생성
            // 현재는 로깅만 수행

        } catch (Exception e) {
            log.error("후기 수신 알림 발행 실패. revieweeId: {}, error: {}",
                    reviewee.getId(), e.getMessage(), e);
        }
    }

    /**
     * 다중 사용자에게 동일한 알림 이벤트 발행
     * 예: 관심 상품에 새로운 입찰이 있을 때 여러 관심 등록자에게 알림
     *
     * @param userIds 대상 사용자 ID 배열
     * @param auctionId 경매 ID
     * @param auctionTitle 경매 제목
     * @param bidAmount 입찰 금액
     * @param bidderNickname 입찰자 닉네임
     */
    public void publishNewBidNotificationToMultiple(Long[] userIds, Long auctionId, String auctionTitle,
                                                  Long bidAmount, String bidderNickname) {
        for (Long userId : userIds) {
            publishNewBidNotification(userId, auctionId, auctionTitle, bidAmount, bidderNickname);
        }

        log.info("다중 사용자 새로운 입찰 알림 이벤트 발행 완료. userCount: {}, auctionId: {}",
                userIds.length, auctionId);
    }
}