package com.cherrypick.app.domain.auction.service;

import com.cherrypick.app.config.BusinessConfig;
import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.bid.repository.BidRepository;
import com.cherrypick.app.domain.connection.dto.response.ConnectionResponse;
import com.cherrypick.app.domain.connection.service.ConnectionServiceImpl;
import com.cherrypick.app.domain.notification.event.AuctionNotSoldNotificationEvent;
import com.cherrypick.app.domain.notification.event.AuctionSoldNotificationEvent;
import com.cherrypick.app.domain.notification.event.AuctionWonNotificationEvent;
import com.cherrypick.app.domain.notification.event.AuctionEndedForParticipantEvent;
import com.cherrypick.app.domain.notification.event.AuctionNotSoldForHighestBidderEvent;
import com.cherrypick.app.domain.websocket.service.WebSocketMessagingService;
import com.cherrypick.app.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 경매 스케줄러 서비스
 * 경매 종료, 낙찰 처리, 연결 서비스 자동 생성 등을 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionSchedulerService {
    
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final ConnectionServiceImpl connectionService;
    private final WebSocketMessagingService webSocketMessagingService;
    private final BusinessConfig businessConfig;
    private final ApplicationEventPublisher applicationEventPublisher;
    
    /**
     * 경매 종료 처리 스케줄러
     * 매 1분마다 실행하여 종료된 경매를 자동 처리
     */
    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    @Transactional
    public void processEndedAuctions() {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        log.debug("경매 종료 처리 스케줄러 실행: {}", now);
        
        // 종료 시간이 지난 활성 경매들 조회
        List<Auction> endedAuctions = auctionRepository.findExpiredActiveAuctions(now);
        
        if (endedAuctions.isEmpty()) {
            log.debug("종료 처리할 경매가 없습니다.");
            return;
        }
        
        log.info("종료 처리할 경매 {}개 발견", endedAuctions.size());
        
        for (Auction auction : endedAuctions) {
            try {
                processAuctionEnd(auction);
                log.info("경매 {} 종료 처리 완료", auction.getId());
            } catch (Exception e) {
                log.error("경매 {} 종료 처리 중 오류 발생", auction.getId(), e);
            }
        }
    }
    
    /**
     * 개별 경매 종료 처리
     * 
     * @param auction 종료할 경매
     */
    private void processAuctionEnd(Auction auction) {
        // 최고 입찰 조회
        Optional<Bid> highestBidOpt = bidRepository.findTopByAuctionIdOrderByBidAmountDesc(auction.getId());
        
        if (highestBidOpt.isEmpty()) {
            // 입찰이 없는 경우 - 유찰 처리
            handleNoReserveAuction(auction);
            return;
        }
        
        Bid highestBid = highestBidOpt.get();
        BigDecimal finalPrice = highestBid.getBidAmount();
        
        // Reserve Price 확인
        if (!auction.isReservePriceMet(finalPrice)) {
            // Reserve Price 미달 - 유찰 처리
            handleNoReserveAuction(auction);
            return;
        }
        
        // 정상 낙찰 처리
        handleSuccessfulAuction(auction, highestBid, finalPrice);
    }
    
    /**
     * 정상 낙찰 처리
     */
    private void handleSuccessfulAuction(Auction auction, Bid winningBid, BigDecimal finalPrice) {
        // 1. 경매 상태를 종료로 변경하고 낙찰자 설정
        auction.endAuction(winningBid.getBidder(), finalPrice);
        auctionRepository.save(auction);

        // 2. 연결 서비스 자동 생성 (PENDING 상태)
        try {
            ConnectionResponse connectionResponse = connectionService.createConnection(
                auction.getId(), winningBid.getBidder().getId()
            );
            log.info("경매 {} 연결 서비스 생성 완료: {}", auction.getId(), connectionResponse.getId());
        } catch (Exception e) {
            log.error("경매 {} 연결 서비스 생성 실패", auction.getId(), e);
        }

        // 3. 실시간 낙찰 알림 전송
        String winnerNickname = winningBid.getBidder().getNickname() != null ?
            winningBid.getBidder().getNickname() :
            "익명" + winningBid.getBidder().getId();

        webSocketMessagingService.notifyAuctionEnded(
            auction.getId(),
            finalPrice,
            winnerNickname
        );

        // 4. 낙찰 알림 이벤트 발행 (구매자에게)
        applicationEventPublisher.publishEvent(new AuctionWonNotificationEvent(
            this,
            winningBid.getBidder().getId(),
            auction.getId(),
            auction.getTitle(),
            finalPrice.longValue()
        ));

        // 5. 판매자에게 낙찰 알림 이벤트 발행
        applicationEventPublisher.publishEvent(new AuctionSoldNotificationEvent(
            this,
            auction.getSeller().getId(),
            auction.getId(),
            auction.getTitle(),
            finalPrice.longValue(),
            winnerNickname
        ));

        // 6. 모든 입찰 참여자에게 경매 종료 알림 발행 (낙찰자 제외)
        notifyAllParticipants(auction, winningBid.getBidder().getId(), finalPrice.longValue(), true);

        log.info("경매 {} 낙찰 처리 완료 - 낙찰가: {}원, 낙찰자: {}, 참여자 {}명에게 알림 전송",
                auction.getId(), finalPrice.intValue(), winnerNickname,
                bidRepository.countDistinctBiddersByAuctionId(auction.getId()) - 1);
    }
    
    /**
     * 유찰 처리 (입찰 없음 또는 Reserve Price 미달)
     */
    private void handleNoReserveAuction(Auction auction) {
        // 경매 상태를 Reserve Price 미달로 변경
        auction.endAuction(null, BigDecimal.ZERO);
        auctionRepository.save(auction);

        // 최고 입찰 조회 (유찰이지만 입찰자가 있을 수 있음)
        Optional<Bid> highestBidOpt = bidRepository.findTopByAuctionIdOrderByBidAmountDesc(auction.getId());

        // 실시간 유찰 알림 전송
        webSocketMessagingService.notifyAuctionEnded(
            auction.getId(),
            BigDecimal.ZERO,
            "유찰"
        );

        // 유찰 알림 이벤트 발행 (판매자에게)
        applicationEventPublisher.publishEvent(new AuctionNotSoldNotificationEvent(
            this,
            auction.getSeller().getId(),
            auction.getId(),
            auction.getTitle(),
            highestBidOpt.orElse(null)
        ));

        // 최고 입찰자가 있는 경우 유찰 알림 발행
        if (highestBidOpt.isPresent()) {
            Bid highestBid = highestBidOpt.get();
            applicationEventPublisher.publishEvent(new AuctionNotSoldForHighestBidderEvent(
                this,
                highestBid.getBidder().getId(),
                auction.getId(),
                auction.getTitle(),
                highestBid.getBidAmount().longValue()
            ));

            // 다른 참여자들에게도 유찰 알림
            notifyAllParticipants(auction, highestBid.getBidder().getId(), 0L, false);
        }

        log.info("경매 {} 유찰 처리 완료 - 참여자 {}명에게 알림 전송",
                auction.getId(),
                highestBidOpt.isPresent() ? bidRepository.countDistinctBiddersByAuctionId(auction.getId()) : 0);
    }

    /**
     * 모든 입찰 참여자에게 경매 종료 알림 발행
     *
     * @param auction 종료된 경매
     * @param excludeUserId 제외할 사용자 ID (낙찰자 또는 최고 입찰자)
     * @param finalPrice 낙찰가 (낙찰 시) 또는 0 (유찰 시)
     * @param wasSuccessful 낙찰 성공 여부
     */
    private void notifyAllParticipants(Auction auction, Long excludeUserId, Long finalPrice, boolean wasSuccessful) {
        // 해당 경매의 모든 입찰자 조회 (중복 제거)
        List<Bid> allBids = bidRepository.findByAuctionIdOrderByBidAmountDesc(auction.getId());

        // 중복 제거 및 제외 대상 필터링
        Set<Long> notifiedUserIds = allBids.stream()
                .map(bid -> bid.getBidder().getId())
                .filter(userId -> !userId.equals(excludeUserId)) // 낙찰자/최고입찰자 제외
                .filter(userId -> !userId.equals(auction.getSeller().getId())) // 판매자 제외
                .collect(Collectors.toSet());

        // 각 참여자에게 알림 이벤트 발행
        for (Long participantId : notifiedUserIds) {
            applicationEventPublisher.publishEvent(new AuctionEndedForParticipantEvent(
                this,
                participantId,
                auction.getId(),
                auction.getTitle(),
                finalPrice,
                wasSuccessful
            ));
        }

        log.debug("경매 {} 참여자 {}명에게 종료 알림 발행 (낙찰: {})",
                auction.getId(), notifiedUserIds.size(), wasSuccessful);
    }
    
    /**
     * 정리 작업 스케줄러 (매일 새벽 3시 실행)
     * 완료된 경매, 만료된 세션 등 정리
     */
    @Scheduled(cron = "0 0 3 * * ?") // 매일 새벽 3시
    @Transactional
    public void dailyCleanup() {
        log.info("일일 정리 작업 시작");
        
        try {
            // TODO: 추후 구현
            // 1. 완료된 경매의 오래된 데이터 정리
            // 2. 만료된 세션 정리  
            // 3. 통계 데이터 정리/집계
            
            log.info("일일 정리 작업 완료");
        } catch (Exception e) {
            log.error("일일 정리 작업 중 오류 발생", e);
        }
    }
}