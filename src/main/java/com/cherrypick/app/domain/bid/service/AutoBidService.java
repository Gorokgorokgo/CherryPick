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
import java.util.Optional;
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
            log.info("📋 조회된 자동입찰 설정 개수: {} - 경매 ID: {}", activeAutoBids.size(), auctionId);
            
            if (activeAutoBids.isEmpty()) {
                log.info("❌ 활성 자동입찰자가 없음 - 경매 ID: {}", auctionId);
                return CompletableFuture.completedFuture(null);
            }
            
            for (Bid autoBid : activeAutoBids) {
                log.info("🎯 자동입찰 설정 발견 - 입찰자: {}, 최대금액: {}", 
                        autoBid.getBidder().getId(), autoBid.getMaxAutoBidAmount());
            }
            
            // 새로운 자동입찰 로직: 최고 입찰액 기반 경쟁
            // 중요: DB에서 최신 현재가를 다시 조회 (수동입찰이 이미 반영됨)
            BigDecimal currentPrice = auction.getCurrentPrice();
            log.info("📊 자동입찰 처리용 현재가: {}원 (전달받은 입찰가: {}원)", currentPrice, newBidAmount);
            processSmartAutoBidding(activeAutoBids, currentPrice, auction);
            
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
     * 스마트 자동입찰 처리 - 최고 입찰액 기반 경쟁
     * eBay 스타일의 진짜 자동입찰 시스템
     */
    private void processSmartAutoBidding(List<Bid> activeAutoBids, BigDecimal currentPrice, Auction auction) {
        // 모든 자동입찰자의 최대금액보다 현재가가 높은 경우 자동입찰 불가
        boolean anyCanAutoBid = activeAutoBids.stream()
                .anyMatch(autoBid -> autoBid.getMaxAutoBidAmount().compareTo(currentPrice) > 0);
        
        if (!anyCanAutoBid) {
            log.info("🚫 모든 자동입찰자의 최대금액이 현재가({})보다 낮아 자동입찰 불가", currentPrice);
            return;
        }
        
        if (activeAutoBids.size() < 2) {
            log.info("자동입찰자가 2명 미만이므로 일반 처리 - 현재 자동입찰자: {}", activeAutoBids.size());
            // 1명만 있으면 기존 방식으로 처리
            for (Bid autoBid : activeAutoBids) {
                if (processIndividualAutoBid(autoBid, currentPrice, auction)) {
                    break; // 한 명만 입찰하면 끝
                }
            }
            return;
        }
        
        // 최고금액별로 정렬 (내림차순)
        activeAutoBids.sort((a, b) -> b.getMaxAutoBidAmount().compareTo(a.getMaxAutoBidAmount()));
        
        Bid highestBidder = activeAutoBids.get(0);
        Bid secondBidder = activeAutoBids.get(1);
        
        log.info("🏁 스마트 자동입찰 시작 - 자동입찰자: {}명", activeAutoBids.size());
        log.info("🥇 1위: 입찰자 {} (최대: {}원)", highestBidder.getBidder().getId(), highestBidder.getMaxAutoBidAmount());
        log.info("🥈 2위: 입찰자 {} (최대: {}원)", secondBidder.getBidder().getId(), secondBidder.getMaxAutoBidAmount());
        
        // 현재 최고입찰자가 누구인지 확인
        Bid currentHighestBid = bidRepository.findTopByAuctionIdOrderByBidAmountDesc(auction.getId()).orElse(null);
        Long currentHighestBidderId = (currentHighestBid != null) ? currentHighestBid.getBidder().getId() : null;
        
        // 스마트 자동입찰 실행
        BigDecimal finalBidAmount;
        Bid winner;
        
        if (highestBidder.getMaxAutoBidAmount().equals(secondBidder.getMaxAutoBidAmount())) {
            // 동일한 최대금액인 경우: 먼저 설정한 사람이 이김
            winner = highestBidder.getId() < secondBidder.getId() ? highestBidder : secondBidder;
            
            // 동일 최대금액일 때는 공통 최대금액 + 최소증가분으로 입찰
            BigDecimal commonMaxAmount = highestBidder.getMaxAutoBidAmount();
            BigDecimal increment = calculateMinimumIncrement(commonMaxAmount);
            finalBidAmount = commonMaxAmount.add(increment);
            
            log.info("⚖️ 동일 최대금액({})원 - 먼저 설정한 입찰자 {} 승리, 최종 입찰가: {}원", 
                commonMaxAmount, winner.getBidder().getId(), finalBidAmount);
        } else {
            // 다른 최대금액인 경우: 높은 쪽이 이기고, 낮은 쪽 최대금액 + 최소증가분으로 결정
            winner = highestBidder;
            BigDecimal secondHighestMax = secondBidder.getMaxAutoBidAmount();
            BigDecimal increment = calculateMinimumIncrement(secondHighestMax);
            finalBidAmount = secondHighestMax.add(increment);
            
            // 승자의 최대금액을 초과하면 승자의 최대금액으로 제한
            if (finalBidAmount.compareTo(winner.getMaxAutoBidAmount()) > 0) {
                finalBidAmount = winner.getMaxAutoBidAmount();
            }
            
            log.info("🏆 최고입찰자 {} 승리 - 최종 입찰가: {}원 (2위 최대금액: {}원 + 증가분)", 
                    winner.getBidder().getId(), finalBidAmount, secondHighestMax);
        }
        
        // 승자가 이미 최고입찰자가 아닌 경우에만 입찰 실행
        if (currentHighestBidderId == null || !currentHighestBidderId.equals(winner.getBidder().getId())) {
            executeAutoBid(winner, finalBidAmount, auction);
        } else {
            log.info("🔄 승자가 이미 최고입찰자이므로 입찰 건너뜀");
        }
    }
    
    /**
     * 가격대별 최소 증가분 계산
     */
    private BigDecimal calculateMinimumIncrement(BigDecimal price) {
        if (price.compareTo(BigDecimal.valueOf(10000)) < 0) {
            return BigDecimal.valueOf(500);
        } else if (price.compareTo(BigDecimal.valueOf(1000000)) < 0) {
            return BigDecimal.valueOf(1000);
        } else if (price.compareTo(BigDecimal.valueOf(10000000)) < 0) {
            return BigDecimal.valueOf(5000);
        } else {
            return BigDecimal.valueOf(10000);
        }
    }
    
    /**
     * 자동입찰 실행
     */
    private void executeAutoBid(Bid autoBidConfig, BigDecimal bidAmount, Auction auction) {
        try {
            // 자동입찰 실행 (트리거된 입찰은 isAutoBid=false로 구분)
            Bid newAutoBid = Bid.builder()
                    .auction(auction)
                    .bidder(autoBidConfig.getBidder())
                    .bidAmount(bidAmount)
                    .isAutoBid(true)  // 자동입찰로 생성된 입찰
                    .maxAutoBidAmount(autoBidConfig.getMaxAutoBidAmount())  // 참조를 위해 유지
                    .autoBidPercentage(autoBidConfig.getAutoBidPercentage())
                    .status(BidStatus.ACTIVE)
                    .bidTime(LocalDateTime.now())
                    .build();
            
            bidRepository.save(newAutoBid);
            
            // 경매 현재가 및 입찰수 업데이트
            auction.updateCurrentPrice(bidAmount);
            auction.increaseBidCount();
            auctionRepository.save(auction);
            
            // 실시간 알림 전송
            String bidderName = autoBidConfig.getBidder().getNickname() != null ? 
                    autoBidConfig.getBidder().getNickname() : "익명" + autoBidConfig.getBidder().getId();
            webSocketMessagingService.notifyNewBid(
                    auction.getId(),
                    bidAmount,
                    auction.getBidCount(),
                    bidderName + " (자동)"
            );
            
            log.info("💰 스마트 자동입찰 실행 완료 - 입찰자: {}, 금액: {}", 
                    autoBidConfig.getBidder().getId(), bidAmount);
            
        } catch (Exception e) {
            log.error("스마트 자동입찰 실행 중 오류 - 입찰자: {}", autoBidConfig.getBidder().getId(), e);
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
            log.info("🔍 자동입찰 조건 체크 시작 - 입찰자: {}, 현재가: {}", autoBid.getBidder().getId(), currentHighestBid);
            if (!shouldTriggerAutoBid(autoBid, currentHighestBid, auction)) {
                log.info("❌ 자동입찰 조건 불만족 - 입찰자: {}", autoBid.getBidder().getId());
                return false;
            }
            log.info("✅ 자동입찰 조건 만족 - 입찰자: {}", autoBid.getBidder().getId());
            
            // 다음 자동입찰 금액 계산 (사용자 설정 퍼센티지 사용)
            BigDecimal nextBidAmount = calculateNextAutoBidAmount(currentHighestBid, autoBid.getAutoBidPercentage());
            
            // 최대금액 초과시 최대금액으로 제한
            if (nextBidAmount.compareTo(autoBid.getMaxAutoBidAmount()) > 0) {
                nextBidAmount = autoBid.getMaxAutoBidAmount();
                log.info("⚠️ 계산된 입찰가가 최대금액 초과 → 최대금액으로 제한 - 입찰자: {}, 최종금액: {}", 
                        autoBid.getBidder().getId(), nextBidAmount);
            }
            
            // 자동입찰 실행 (트리거된 입찰은 isAutoBid=false로 구분)
            Bid newAutoBid = Bid.builder()
                    .auction(auction)
                    .bidder(autoBid.getBidder())
                    .bidAmount(nextBidAmount)
                    .isAutoBid(true)  // 자동입찰로 생성된 입찰
                    .maxAutoBidAmount(autoBid.getMaxAutoBidAmount())  // 참조를 위해 유지
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
            log.error("개별 자동입찰 처리 중 상세 오류 - 입찰자: {}, 오류: {}", autoBid.getBidder().getId(), e.getMessage(), e);
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
            log.debug("비활성 경매로 자동입찰 건너뜀 - 경매 ID: {}", auction.getId());
            return false;
        }
        
        // 2. 현재가가 이미 내 최대금액을 초과했는지 확인
        if (currentHighestBid.compareTo(autoBid.getMaxAutoBidAmount()) >= 0) {
            log.debug("현재가가 이미 최대 자동입찰 금액을 초과하여 건너뜀 - 입찰자: {}, 현재가: {}, 최대금액: {}", 
                    autoBid.getBidder().getId(), currentHighestBid, autoBid.getMaxAutoBidAmount());
            return false;
        }
        
        // 3. 내가 현재 최고입찰자인지 확인 (가장 중요한 체크)
        Bid currentHighest = bidRepository.findTopByAuctionIdOrderByBidAmountDesc(auction.getId()).orElse(null);
        if (currentHighest != null && currentHighest.getBidder().getId().equals(autoBid.getBidder().getId())) {
            log.debug("내가 이미 최고입찰자이므로 자동입찰 건너뜀 - 입찰자: {}", autoBid.getBidder().getId());
            return false;
        }
        
        // 4. 자동입찰 설정은 bidAmount가 0이므로 이 조건 생략
        
        log.debug("자동입찰 조건 통과 - 입찰자: {}, 현재가: {}, 최대금액: {}", 
                autoBid.getBidder().getId(), currentHighestBid, autoBid.getMaxAutoBidAmount());
        return true;
    }
    
    /**
     * 다음 자동입찰 금액 계산 (최소 입찰 단위 사용)
     * 
     * @param currentBid 현재 입찰가
     * @param percentage 증가율 (현재는 사용하지 않음, 최소 단위 우선)
     * @return 계산된 입찰가 (최소 입찰 단위 적용)
     */
    public BigDecimal calculateNextAutoBidAmount(BigDecimal currentBid, int percentage) {
        // 가격대별 최소 입찰 단위 계산 (BidService 로직과 동일)
        BigDecimal minimumIncrement;
        
        if (currentBid.compareTo(BigDecimal.valueOf(10000)) < 0) {
            // 1만원 미만: 최소 500원 증가
            minimumIncrement = BigDecimal.valueOf(500);
        } else if (currentBid.compareTo(BigDecimal.valueOf(1000000)) < 0) {
            // 1만원~100만원: 최소 1,000원 증가
            minimumIncrement = BigDecimal.valueOf(1000);
        } else if (currentBid.compareTo(BigDecimal.valueOf(10000000)) < 0) {
            // 100만원~1,000만원: 최소 5,000원 증가
            minimumIncrement = BigDecimal.valueOf(5000);
        } else {
            // 1,000만원 이상: 최소 10,000원 증가
            minimumIncrement = BigDecimal.valueOf(10000);
        }
        
        return currentBid.add(minimumIncrement);
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