package com.cherrypick.app.domain.auction.service;

import com.cherrypick.app.config.BusinessConfig;
import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.bid.repository.BidRepository;
import com.cherrypick.app.domain.connection.dto.response.ConnectionResponse;
import com.cherrypick.app.domain.connection.service.ConnectionServiceImpl;
import com.cherrypick.app.domain.common.service.WebSocketMessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    
    /**
     * 경매 종료 처리 스케줄러
     * 매 1분마다 실행하여 종료된 경매를 자동 처리
     */
    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    @Transactional
    public void processEndedAuctions() {
        LocalDateTime now = LocalDateTime.now();
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
        
        log.info("경매 {} 낙찰 처리 완료 - 낙찰가: {}원, 낙찰자: {}", 
                auction.getId(), finalPrice.intValue(), winnerNickname);
    }
    
    /**
     * 유찰 처리 (입찰 없음 또는 Reserve Price 미달)
     */
    private void handleNoReserveAuction(Auction auction) {
        // 경매 상태를 Reserve Price 미달로 변경
        auction.endAuction(null, BigDecimal.ZERO);
        auctionRepository.save(auction);
        
        // 실시간 유찰 알림 전송
        webSocketMessagingService.notifyAuctionEnded(
            auction.getId(), 
            BigDecimal.ZERO, 
            "유찰"
        );
        
        log.info("경매 {} 유찰 처리 완료", auction.getId());
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