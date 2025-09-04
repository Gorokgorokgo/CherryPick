package com.cherrypick.app.domain.bid.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.bid.enums.BidStatus;
import com.cherrypick.app.domain.bid.repository.BidRepository;
import com.cherrypick.app.domain.user.repository.UserRepository;
import com.cherrypick.app.domain.websocket.service.WebSocketMessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 자동입찰 서비스
 * 
 * 비즈니스 로직:
 * 1. 새 입찰 발생시 활성 자동입찰들을 1초 딜레이 후 실행
 * 2. 자동입찰 금액은 현재가 + 설정된 percentage로 계산
 * 3. 최대 금액 초과시 자동입찰 중단
 * 4. 동시 자동입찰시 최대금액이 높은 순으로 우선권 부여
 * 5. 100원 단위로 반올림
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AutoBidService {
    
    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final WebSocketMessagingService webSocketMessagingService;
    
    /**
     * 새 입찰에 대한 자동입찰 처리
     * 
     * @param auctionId 경매 ID
     * @param newBidAmount 새로운 입찰 금액
     * @return 비동기 처리 결과
     */
    @Async
    @Transactional
    public CompletableFuture<Void> processAutoBidsForAuction(Long auctionId, BigDecimal newBidAmount) {
        try {
            log.info("자동입찰 처리 시작 - 경매 ID: {}, 새 입찰가: {}", auctionId, newBidAmount);
            
            // 1초 딜레이 (비즈니스 요구사항)
            Thread.sleep(1000);
            
            // 경매 정보 조회
            Auction auction = auctionRepository.findById(auctionId)
                    .orElse(null);
            
            if (auction == null || !auction.isActive()) {
                log.warn("비활성 경매에 대한 자동입찰 시도 - 경매 ID: {}", auctionId);
                return CompletableFuture.completedFuture(null);
            }
            
            // 해당 경매의 활성 자동입찰자들 조회 (최대금액 높은 순)
            List<Bid> activeAutoBids = bidRepository.findActiveAutoBidsByAuctionId(auctionId);
            
            if (activeAutoBids.isEmpty()) {
                log.debug("활성 자동입찰자가 없음 - 경매 ID: {}", auctionId);
                return CompletableFuture.completedFuture(null);
            }
            
            // 자동입찰 실행 (최대금액 높은 순으로 처리)
            for (Bid autoBid : activeAutoBids) {
                if (processIndividualAutoBid(autoBid, newBidAmount, auction)) {
                    // 하나의 자동입찰이 성공하면 중단 (경쟁 방지)
                    break;
                }
            }
            
            return CompletableFuture.completedFuture(null);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("자동입찰 처리 중 인터럽트 발생", e);
            return CompletableFuture.failedFuture(e);
        } catch (Exception e) {
            log.error("자동입찰 처리 중 오류 발생 - 경매 ID: {}", auctionId, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 개별 자동입찰 처리
     * 
     * @param autoBid 자동입찰 정보
     * @param currentHighestBid 현재 최고 입찰가
     * @param auction 경매 정보
     * @return 자동입찰 실행 여부
     */
    private boolean processIndividualAutoBid(Bid autoBid, BigDecimal currentHighestBid, Auction auction) {
        try {
            // 자동입찰 실행 조건 확인
            if (!shouldTriggerAutoBid(autoBid, currentHighestBid, auction)) {
                return false;
            }
            
            // 다음 자동입찰 금액 계산 (사용자 설정 퍼센티지 사용)
            BigDecimal nextBidAmount = calculateNextAutoBidAmount(currentHighestBid, autoBid.getAutoBidPercentage());
            
            // 최대금액 초과 확인
            if (nextBidAmount.compareTo(autoBid.getMaxAutoBidAmount()) > 0) {
                log.info("최대금액 초과로 자동입찰 중단 - 입찰자: {}, 계산된 금액: {}, 최대금액: {}", 
                        autoBid.getBidder().getId(), nextBidAmount, autoBid.getMaxAutoBidAmount());
                return false;
            }
            
            // 자동입찰 실행 (직접 Bid 생성)
            Bid newAutoBid = Bid.builder()
                    .auction(auction)
                    .bidder(autoBid.getBidder())
                    .bidAmount(nextBidAmount)
                    .isAutoBid(true)
                    .maxAutoBidAmount(autoBid.getMaxAutoBidAmount())
                    .autoBidPercentage(autoBid.getAutoBidPercentage())
                    .status(BidStatus.ACTIVE)
                    .bidTime(LocalDateTime.now())
                    .build();
            
            bidRepository.save(newAutoBid);
            
            // 경매 현재가 및 입찰수 업데이트
            auction.updateCurrentPrice(nextBidAmount);
            auction.increaseBidCount();
            auctionRepository.save(auction);
            
            // 실시간 알림 전송
            String bidderName = autoBid.getBidder().getNickname() != null ? 
                    autoBid.getBidder().getNickname() : "익명" + autoBid.getBidder().getId();
            webSocketMessagingService.notifyNewBid(
                    auction.getId(),
                    nextBidAmount,
                    auction.getBidCount(),
                    bidderName + " (자동)"
            );
            
            log.info("자동입찰 실행 완료 - 입찰자: {}, 금액: {}", 
                    autoBid.getBidder().getId(), nextBidAmount);
            
            return true;
            
        } catch (Exception e) {
            log.error("개별 자동입찰 처리 중 오류 - 입찰자: {}", autoBid.getBidder().getId(), e);
            return false;
        }
    }
    
    /**
     * 자동입찰 실행 여부 판단
     * 
     * @param autoBid 자동입찰 정보
     * @param currentHighestBid 현재 최고 입찰가
     * @param auction 경매 정보
     * @return 실행 여부
     */
    private boolean shouldTriggerAutoBid(Bid autoBid, BigDecimal currentHighestBid, Auction auction) {
        // 1. 경매가 활성상태인지 확인
        if (!auction.isActive()) {
            return false;
        }
        
        // 2. 현재 최고 입찰가가 자신의 입찰가보다 높은지 확인
        if (currentHighestBid.compareTo(autoBid.getBidAmount()) <= 0) {
            return false;
        }
        
        // 3. 자신의 입찰이 아닌지 확인 (같은 입찰자의 연속 입찰 방지)
        // 이 로직은 실제로는 BidService에서 처리되므로 여기서는 기본적으로 허용
        
        return true;
    }
    
    /**
     * 다음 자동입찰 금액 계산
     * 
     * @param currentBid 현재 입찰가
     * @param percentage 증가율 (5-10%)
     * @return 계산된 입찰가 (100원 단위 반올림)
     */
    public BigDecimal calculateNextAutoBidAmount(BigDecimal currentBid, int percentage) {
        if (percentage < 5 || percentage > 10) {
            throw new IllegalArgumentException("증가율은 5-10% 범위여야 합니다.");
        }
        
        // percentage% 증가 계산
        BigDecimal multiplier = BigDecimal.ONE.add(BigDecimal.valueOf(percentage).divide(BigDecimal.valueOf(100)));
        BigDecimal calculatedAmount = currentBid.multiply(multiplier);
        
        // 100원 단위로 반올림
        BigDecimal roundedAmount = calculatedAmount.divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        
        return roundedAmount;
    }
    
    /**
     * 특정 경매의 활성 자동입찰 조회
     * 
     * @param auctionId 경매 ID
     * @return 활성 자동입찰 목록
     */
    public List<Bid> getActiveAutoBidsForAuction(Long auctionId) {
        return bidRepository.findActiveAutoBidsByAuctionId(auctionId);
    }
    
    /**
     * 사용자의 활성 자동입찰 조회
     * 
     * @param bidderId 입찰자 ID
     * @return 활성 자동입찰 목록
     */
    public List<Bid> getActiveAutoBidsForBidder(Long bidderId) {
        return bidRepository.findActiveAutoBidsByBidderId(bidderId);
    }
}