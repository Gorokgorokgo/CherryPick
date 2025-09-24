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
import java.util.*;
import java.util.stream.Collectors;
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
                log.warn("❌ 활성 자동입찰자가 없음 - 경매 ID: {}", auctionId);

                // 디버그: 해당 경매의 모든 Bid 레코드 조회
                List<Bid> allBidsForAuction = bidRepository.findAll().stream()
                        .filter(bid -> bid.getAuction().getId().equals(auctionId))
                        .toList();

                log.info("🔍 경매 {}의 전체 Bid 레코드 개수: {}", auctionId, allBidsForAuction.size());

                for (Bid bid : allBidsForAuction) {
                    log.info("📝 Bid 레코드 - ID: {}, 입찰자: {}, 금액: {}, 자동입찰: {}, 상태: {}, 최대자동금액: {}",
                            bid.getId(), bid.getBidder().getId(), bid.getBidAmount(),
                            bid.getIsAutoBid(), bid.getStatus(), bid.getMaxAutoBidAmount());
                }

                return CompletableFuture.completedFuture(null);
            }

            for (Bid autoBid : activeAutoBids) {
                log.info("🎯 자동입찰 설정 발견 - 입찰자: {}, 최대금액: {}, 상태: {}, 입찰금액: {}",
                        autoBid.getBidder().getId(), autoBid.getMaxAutoBidAmount(),
                        autoBid.getStatus(), autoBid.getBidAmount());
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
Bid currentHighestBid = bidRepository.findTopActualByAuctionIdOrderByBidAmountDesc(auction.getId()).orElse(null);
        Long currentHighestBidderId = (currentHighestBid != null) ? currentHighestBid.getBidder().getId() : null;
        
        // 스마트 자동입찰 실행
        BigDecimal finalBidAmount;
        Bid winner;
        
        if (highestBidder.getMaxAutoBidAmount().equals(secondBidder.getMaxAutoBidAmount())) {
            // 동일한 최대금액인 경우: 먼저 설정한 사람이 이김
            winner = highestBidder.getId() < secondBidder.getId() ? highestBidder : secondBidder;
            
            // 동일 최대금액이면 최종 입찰가는 공통 최대금액(추가 증가 없음)
            BigDecimal commonMaxAmount = highestBidder.getMaxAutoBidAmount();
            finalBidAmount = commonMaxAmount;
            
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
            // null 안전성 검증
            if (autoBidConfig == null) {
                log.error("❌ executeAutoBid: autoBidConfig가 null입니다");
                return;
            }
            if (autoBidConfig.getBidder() == null) {
                log.error("❌ executeAutoBid: 입찰자 정보가 null입니다 - 자동입찰 ID: {}", autoBidConfig.getId());
                return;
            }

            log.info("🚀 자동입찰 실행 시작 - 입찰자: {}, 금액: {}원",
                autoBidConfig.getBidder().getId(), bidAmount);

            // 현재 최고입찰자 확인 - 자기 자신이면 중복 자동입찰 방지, 혹은 제안 금액이 현재가 이하인 경우 생략
            Bid currentHighest = bidRepository.findTopActualByAuctionIdOrderByBidAmountDesc(auction.getId()).orElse(null);
            if (currentHighest != null && currentHighest.getBidder() != null) {
                if (currentHighest.getBidder().getId().equals(autoBidConfig.getBidder().getId())) {
                    log.info("💤 이미 최고입찰자이므로 자동입찰 생략 - 사용자: {}", autoBidConfig.getBidder().getId());
                    return;
                }
                if (bidAmount.compareTo(currentHighest.getBidAmount()) <= 0) {
                    log.info("💤 제안된 자동입찰 금액이 현재가 이하이므로 생략 - 제안: {}, 현재가: {}",
                            bidAmount, currentHighest.getBidAmount());
                    return;
                }
            }

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
        log.info("🔍 자동입찰 조건 검사 시작 - 입찰자: {}, 현재가: {}, 최대금액: {}",
                autoBid.getBidder().getId(), currentHighestBid, autoBid.getMaxAutoBidAmount());

        // 1. 경매가 활성상태인지 확인
        if (!auction.isActive()) {
            log.warn("❌ 비활성 경매로 자동입찰 건너뜀 - 경매 ID: {}, 상태: {}", auction.getId(), auction.getStatus());
            return false;
        }

        // 2. 현재가가 이미 내 최대금액을 초과했는지 확인
        if (currentHighestBid.compareTo(autoBid.getMaxAutoBidAmount()) >= 0) {
            log.warn("❌ 현재가가 이미 최대 자동입찰 금액을 초과하여 건너뜀 - 입찰자: {}, 현재가: {}, 최대금액: {}",
                    autoBid.getBidder().getId(), currentHighestBid, autoBid.getMaxAutoBidAmount());
            return false;
        }

        // 3. 내가 현재 최고입찰자인지 확인 (가장 중요한 체크)
Bid currentHighest = bidRepository.findTopActualByAuctionIdOrderByBidAmountDesc(auction.getId()).orElse(null);
        if (currentHighest != null) {
            log.info("🏆 현재 최고입찰자: {} (금액: {}원), 자동입찰자: {}",
                    currentHighest.getBidder().getId(), currentHighest.getBidAmount(), autoBid.getBidder().getId());

            if (currentHighest.getBidder().getId().equals(autoBid.getBidder().getId())) {
                log.warn("❌ 내가 이미 최고입찰자이므로 자동입찰 건너뜀 - 입찰자: {}", autoBid.getBidder().getId());
                return false;
            }
        } else {
            log.info("🆕 첫 입찰 상황 - 현재 최고입찰자 없음");
        }

        // 4. 자동입찰 설정은 bidAmount가 0이므로 이 조건 생략

        log.info("✅ 자동입찰 조건 통과 - 입찰자: {}, 현재가: {}, 최대금액: {}",
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

    /**
     * 자동입찰 설정 시 즉시 경쟁 실행
     * 새로운 자동입찰이 설정될 때 기존 자동입찰자들과 즉시 경쟁하여 최종 결과를 도출
     *
     * @param auctionId 경매 ID
     * @param newAutoBidderId 새로 자동입찰을 설정한 사용자 ID
     * @return 경쟁 결과 처리 여부
     */
    @Transactional
    public boolean processImmediateAutoBidCompetition(Long auctionId, Long newAutoBidderId) {
        try {
            log.info("🚀 자동입찰 설정 시 즉시 경쟁 시작 - 경매 ID: {}, 새 자동입찰자: {}", auctionId, newAutoBidderId);

            // 경매 정보 조회 (행 잠금)
            Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                    .orElse(null);

            if (auction == null || !auction.isActive()) {
                log.warn("비활성 경매에 대한 자동입찰 경쟁 시도 - 경매 ID: {}", auctionId);
                return false;
            }

            // 해당 경매의 모든 활성 자동입찰자들 조회 (단순화된 쿼리 결과)
            List<Bid> rawActiveAutoBids = bidRepository.findActiveAutoBidsByAuctionId(auctionId);
            log.info("📋 원시 자동입찰 설정 수: {} - 경매 ID: {}", rawActiveAutoBids.size(), auctionId);

            // 원시 데이터 상세 로그
            for (int i = 0; i < rawActiveAutoBids.size(); i++) {
                Bid rawBid = rawActiveAutoBids.get(i);
                if (rawBid.getBidder() != null) {
                    log.info("📄 원시 {}위: 사용자 {} (최대: {}원, ID: {}, 생성시간: {})",
                        i+1, rawBid.getBidder().getId(), rawBid.getMaxAutoBidAmount(), rawBid.getId(), rawBid.getBidTime());
                } else {
                    log.warn("⚠️ 원시 {}위: 입찰자 정보가 null - ID: {}", i+1, rawBid.getId());
                }
            }

            // 사용자별 최신 자동입찰만 필터링 (동일 사용자의 중복 설정 제거)
            Map<Long, Bid> latestByUser = new HashMap<>();
            for (Bid bid : rawActiveAutoBids) {
                if (bid.getBidder() != null) {
                    Long userId = bid.getBidder().getId();
                    Bid existing = latestByUser.get(userId);

                    if (existing == null) {
                        log.info("✅ 사용자 {} 첫 자동입찰 등록 - ID: {}, 최대: {}원", userId, bid.getId(), bid.getMaxAutoBidAmount());
                        latestByUser.put(userId, bid);
                    } else if (bid.getId() > existing.getId()) {
                        log.info("🔄 사용자 {} 자동입찰 업데이트 - 기존 ID: {} → 새로운 ID: {}, 최대: {}원 → {}원",
                            userId, existing.getId(), bid.getId(), existing.getMaxAutoBidAmount(), bid.getMaxAutoBidAmount());
                        latestByUser.put(userId, bid);
                    } else {
                        log.info("❌ 사용자 {} 구 자동입찰 제외 - ID: {}, 최대: {}원 (최신: ID {})",
                            userId, bid.getId(), bid.getMaxAutoBidAmount(), existing.getId());
                    }
                }
            }

            List<Bid> activeAutoBids = new ArrayList<>(latestByUser.values());
            // 최대금액 순으로 정렬 (높은 순)
            activeAutoBids.sort((a, b) -> b.getMaxAutoBidAmount().compareTo(a.getMaxAutoBidAmount()));

            log.info("📋 필터링된 활성 자동입찰자 수: {} - 경매 ID: {}", activeAutoBids.size(), auctionId);


            // 최종 자동입찰자 정보 로그
            for (int i = 0; i < activeAutoBids.size(); i++) {
                Bid autoBid = activeAutoBids.get(i);
                log.info("🎯 최종 {}위: 사용자 {} (최대: {}원, ID: {})",
                    i+1, autoBid.getBidder().getId(), autoBid.getMaxAutoBidAmount(), autoBid.getId());
            }

            if (activeAutoBids.size() < 2) {
                log.info("자동입찰자가 2명 미만이므로 경쟁 없음 - 현재 자동입찰자: {}", activeAutoBids.size());
                return false;
            }

            // 현재가 조회
            BigDecimal currentPrice = auction.getCurrentPrice();
            log.info("📊 현재가: {}원", currentPrice);

            // 스마트 자동입찰 경쟁 실행 (딜레이 없이 즉시)
            // 즉시 입찰이 실행되었다면 auction.getCurrentPrice()가 갱신되어 있으므로 최신 가격으로 전달
            processSmartAutoBiddingImmediate(activeAutoBids, auction.getCurrentPrice(), auction);

            return true;

        } catch (Exception e) {
            log.error("자동입찰 즉시 경쟁 처리 중 오류 발생 - 경매 ID: {}", auctionId, e);
            return false;
        }
    }

    /**
     * 즉시 실행되는 스마트 자동입찰 처리 (딜레이 없음)
     * 자동입찰 설정 시점에 바로 경쟁을 실행하여 최종 결과를 도출
     * 핵심: 현재 최고입찰자가 아닌 자동입찰자가 즉시 입찰해서 경쟁 트리거
     */
    private void processSmartAutoBiddingImmediate(List<Bid> activeAutoBids, BigDecimal currentPrice, Auction auction) {
        // 현재가보다 높은 최대금액을 가진 자동입찰자만 필터링
        final BigDecimal finalCurrentPrice = currentPrice;
        List<Bid> eligibleBids = activeAutoBids.stream()
                .filter(autoBid -> autoBid.getMaxAutoBidAmount().compareTo(finalCurrentPrice) > 0)
                .toList();

        if (eligibleBids.isEmpty()) {
            log.info("🚫 모든 자동입찰자의 최대금액이 현재가({})보다 낮아 경쟁 불가", currentPrice);
            return;
        }

        // 현재 최고입찰자 확인
Bid currentHighestBid = bidRepository.findTopActualByAuctionIdOrderByBidAmountDesc(auction.getId()).orElse(null);
        Long currentHighestBidderId = null;

        if (currentHighestBid != null) {
            if (currentHighestBid.getBidder() != null) {
                currentHighestBidderId = currentHighestBid.getBidder().getId();
                log.info("📊 현재 최고입찰자: {} (입찰ID: {}), 현재가: {}원",
                    currentHighestBidderId, currentHighestBid.getId(), currentPrice);
            } else {
                log.warn("⚠️ 현재 최고입찰의 입찰자 정보가 null - 입찰ID: {}", currentHighestBid.getId());
            }
        } else {
            log.info("📊 현재 최고입찰자: 없음, 현재가: {}원", currentPrice);
        }

        if (eligibleBids.size() == 1) {
            Bid singleBidder = eligibleBids.get(0);
            // 혼자라도 현재 최고입찰자가 아니면 입찰 실행
            if (currentHighestBidderId == null || !currentHighestBidderId.equals(singleBidder.getBidder().getId())) {
                BigDecimal nextBidAmount = calculateNextAutoBidAmount(currentPrice, 0);
                // 최대금액 초과 방지
                if (nextBidAmount.compareTo(singleBidder.getMaxAutoBidAmount()) > 0) {
                    nextBidAmount = singleBidder.getMaxAutoBidAmount();
                }
                log.info("🚀 단독 자동입찰자 {} 입찰 실행: {}원", singleBidder.getBidder().getId(), nextBidAmount);
                executeAutoBid(singleBidder, nextBidAmount, auction);
            } else {
                log.info("💤 단독 자동입찰자가 이미 최고입찰자이므로 입찰 생략");
            }
            return;
        }

        // 최고금액별로 정렬 (내림차순)
        eligibleBids.sort((a, b) -> b.getMaxAutoBidAmount().compareTo(a.getMaxAutoBidAmount()));

        log.info("🏁 즉시 자동입찰 경쟁 시작 - 경쟁자: {}명", eligibleBids.size());

        // 최종 결과만 저장하도록 내부 시뮬레이션 실행
        if (eligibleBids.size() >= 2) {
            Bid top1 = eligibleBids.get(0);
            Bid top2 = eligibleBids.get(1);

            BigDecimal secondMax = top2.getMaxAutoBidAmount();
            BigDecimal increment = calculateMinimumIncrement(secondMax);
            BigDecimal winnerFinal;
            if (top1.getMaxAutoBidAmount().compareTo(top2.getMaxAutoBidAmount()) == 0) {
                // 동일 최대금액: 먼저 설정한 사용자 승리, 추가 증가 없음
                winnerFinal = top1.getMaxAutoBidAmount();
            } else {
                winnerFinal = secondMax.add(increment);
                if (winnerFinal.compareTo(top1.getMaxAutoBidAmount()) > 0) {
                    winnerFinal = top1.getMaxAutoBidAmount();
                }
            }

            // 두 개의 기록만 저장: 패자 최대금액, 승자 최종금액
            persistFinalAutoBidOutcome(auction, currentPrice, top2, top1, secondMax, winnerFinal);
            return;
        }

        // 현재 최고입찰자가 아닌 자동입찰자부터 입찰 시작 (체이닝)
        boolean hasActivity = false;
        BigDecimal updatedCurrentPrice = currentPrice;
        Long updatedCurrentHighestBidderId = currentHighestBidderId;

        for (Bid autoBidder : eligibleBids) {
            // null 안전성 검증
            if (autoBidder == null || autoBidder.getBidder() == null) {
                log.warn("⚠️ 자동입찰자 정보가 null - 건너뛰기");
                continue;
            }

            if (updatedCurrentHighestBidderId == null || !updatedCurrentHighestBidderId.equals(autoBidder.getBidder().getId())) {
                // 이 자동입찰자는 현재 최고입찰자가 아니므로 입찰 가능
                BigDecimal targetBidAmount = calculateCompetitiveBidAmount(updatedCurrentPrice, autoBidder.getMaxAutoBidAmount(), eligibleBids);

                if (targetBidAmount.compareTo(updatedCurrentPrice) > 0 && targetBidAmount.compareTo(autoBidder.getMaxAutoBidAmount()) <= 0) {
                    log.info("⚡ 자동입찰자 {} 경쟁 입찰 실행: {}원", autoBidder.getBidder().getId(), targetBidAmount);
                    executeAutoBid(autoBidder, targetBidAmount, auction);
                    hasActivity = true;
                    // 입찰 후 현재가와 최고입찰자 업데이트
                    updatedCurrentPrice = targetBidAmount;
                    updatedCurrentHighestBidderId = autoBidder.getBidder().getId();
                    break; // 한 명씩 입찰하여 자연스러운 경쟁 유도
                }
            }
        }

        if (!hasActivity) {
            log.info("💤 모든 자동입찰자가 이미 경쟁 완료 상태이므로 추가 입찰 없음");
        }
    }

    /**
     * 경쟁적 입찰 금액 계산 - 다른 자동입찰자들을 고려하여 적절한 입찰가 결정
     */
    private BigDecimal calculateCompetitiveBidAmount(BigDecimal currentPrice, BigDecimal maxAmount, List<Bid> competitors) {
        // 현재가 + 최소증가분부터 시작
        BigDecimal baseBidAmount = calculateNextAutoBidAmount(currentPrice, 0);

        // 나보다 높은 최대금액을 가진 경쟁자가 있다면, 전략적 입찰
        boolean hasHigherCompetitor = competitors.stream()
                .anyMatch(competitor -> competitor.getMaxAutoBidAmount().compareTo(maxAmount) > 0);

        if (hasHigherCompetitor) {
            // 경쟁자가 있으면 좀 더 공격적으로 입찰 (최대금액까지)
            return maxAmount;
        } else {
            // 경쟁자가 없으면 최소한만 입찰
            return baseBidAmount.compareTo(maxAmount) > 0 ? maxAmount : baseBidAmount;
        }
    }
    /**
     * 최종 자동입찰 결과만 DB에 반영하는 메서드
     * - 중간 과정은 로그로만 남기고, DB에는 두 건만 저장
     */
    @Transactional
    protected void persistFinalAutoBidOutcome(
            Auction auction,
            BigDecimal currentPrice,
            Bid loserConfig,
            Bid winnerConfig,
            BigDecimal loserFinalAmount,
            BigDecimal winnerFinalAmount
    ) {
        try {
            // 1) 패자 최종 금액 기록 (자동입찰 기록)
            Bid loserFinal = Bid.builder()
                    .auction(auction)
                    .bidder(loserConfig.getBidder())
                    .bidAmount(loserFinalAmount)
                    .isAutoBid(true)
                    .maxAutoBidAmount(loserConfig.getMaxAutoBidAmount())
                    .autoBidPercentage(loserConfig.getAutoBidPercentage())
                    .status(BidStatus.ACTIVE)
                    .bidTime(LocalDateTime.now())
                    .build();
            bidRepository.save(loserFinal);

            // 2) 승자 최종 금액 기록
            Bid winnerFinal = Bid.builder()
                    .auction(auction)
                    .bidder(winnerConfig.getBidder())
                    .bidAmount(winnerFinalAmount)
                    .isAutoBid(true)
                    .maxAutoBidAmount(winnerConfig.getMaxAutoBidAmount())
                    .autoBidPercentage(winnerConfig.getAutoBidPercentage())
                    .status(BidStatus.ACTIVE)
                    .bidTime(LocalDateTime.now())
                    .build();
            bidRepository.save(winnerFinal);

            // 3) 경매 현재가 및 입찰수 업데이트 (2건 증가)
            auction.updateCurrentPrice(winnerFinalAmount);
            auction.increaseBidCount();
            auction.increaseBidCount();
            auctionRepository.save(auction);

            // 4) 실시간 알림: 경쟁 결과 및 NEW_BID 호환 알림
            String loserName = loserConfig.getBidder().getNickname() != null ?
                    loserConfig.getBidder().getNickname() : "익명" + loserConfig.getBidder().getId();
            String winnerName = winnerConfig.getBidder().getNickname() != null ?
                    winnerConfig.getBidder().getNickname() : "익명" + winnerConfig.getBidder().getId();

            // 호환성: 기존 클라이언트는 NEW_BID에 반응하므로 두 건 모두 전송
            webSocketMessagingService.notifyNewBid(
                    auction.getId(),
                    loserFinalAmount,
                    auction.getBidCount() - 1, // 첫 번째 저장 이후 카운트
                    loserName + " (자동)"
            );
            webSocketMessagingService.notifyNewBid(
                    auction.getId(),
                    winnerFinalAmount,
                    auction.getBidCount(),
                    winnerName + " (자동)"
            );

            // 새 클라이언트용 결과 알림
            webSocketMessagingService.notifyAutoBidResult(
                    auction.getId(), winnerFinalAmount, auction.getBidCount(), winnerName + " (자동)"
            );

            log.info("✅ 자동입찰 경쟁 최종 반영 - 경매 {}, 패자 {}:{}, 승자 {}:{}",
                    auction.getId(), loserConfig.getBidder().getId(), loserFinalAmount,
                    winnerConfig.getBidder().getId(), winnerFinalAmount);
        } catch (Exception e) {
            log.error("❌ 자동입찰 최종 결과 반영 실패 - auctionId={}, error={}", auction.getId(), e.getMessage(), e);
        }
    }
    /**
     * 자동입찰 설정 직후 즉시 최소입찰을 강제 실행 (설정자 기준)
     * - 현재 최고입찰자가 아니고
     * - 현재가 + 최소증가분 <= 나의 최대금액
     */
    @Transactional
    public boolean triggerImmediateBidOnSetup(Long auctionId, Long newAutoBidderId) {
        try {
            Auction auction = auctionRepository.findByIdForUpdate(auctionId).orElse(null);
            if (auction == null || !auction.isActive()) return false;

            // 방금 저장된 설정 포함 최신 자동입찰 설정 조회 (bidAmount=0)
            List<Bid> rawActive = bidRepository.findActiveAutoBidsByAuctionId(auctionId);
            Bid config = null;
            for (Bid b : rawActive) {
                if (b.getBidder() != null && b.getBidder().getId().equals(newAutoBidderId)) {
                    if (config == null || b.getId() > config.getId()) {
                        config = b;
                    }
                }
            }
            if (config == null) return false;

            // 현재 최고입찰자 확인 (자동입찰 설정 레코드: bidAmount=0 제외)
            Bid currentHighest = bidRepository.findTopActualByAuctionIdOrderByBidAmountDesc(auctionId).orElse(null);
            Long currentHighestId = currentHighest != null && currentHighest.getBidder() != null ? currentHighest.getBidder().getId() : null;
            if (currentHighestId != null && currentHighestId.equals(newAutoBidderId)) {
                // 이미 최고입찰자면 트리거 불필요
                return false;
            }

            BigDecimal currentPrice = auction.getCurrentPrice();
            BigDecimal startPrice = auction.getStartPrice();

            // 실제 입찰 테이블에서 실제 입찰 수 확인 (bidAmount > 0)
            long actualBidCount = bidRepository.countByAuctionIdAndBidAmountGreaterThan(auctionId, BigDecimal.ZERO);
            boolean isFirstBid = (actualBidCount == 0) && (currentHighestId == null);

            BigDecimal next = isFirstBid ? startPrice : calculateNextAutoBidAmount(currentPrice, 0);
            if (next.compareTo(config.getMaxAutoBidAmount()) > 0) {
                next = config.getMaxAutoBidAmount();
            }

            if (isFirstBid) {
                // 첫 입찰은 시작가로 즉시 입찰 (현재가와 동일하더라도 기록 생성)
                executeAutoBid(config, next, auction);
                return true;
            } else {
                // 첫 입찰이 아니라면 즉시 경쟁 시뮬레이션 실행 (두 건 반영: 패자/승자)
                boolean competed = processImmediateAutoBidCompetition(auctionId, newAutoBidderId);
                if (competed) {
                    return true;
                }

                // 방어적 폴백 1: 실시간 재계산으로 두 명 이상 활성 자동입찰자가 있으면 즉시 두 건 반영
                try {
                    List<Bid> rawActiveFallback = bidRepository.findActiveAutoBidsByAuctionId(auctionId);
                    Map<Long, Bid> latestByUser = new HashMap<>();
                    for (Bid b : rawActiveFallback) {
                        if (b.getBidder() == null) continue;
                        Long uid = b.getBidder().getId();
                        Bid prev = latestByUser.get(uid);
                        if (prev == null || b.getId() > prev.getId()) {
                            latestByUser.put(uid, b);
                        }
                    }
                    List<Bid> active = new ArrayList<>(latestByUser.values());
                    // 현재가보다 높은 최대금액 보유자만 경쟁 대상
                    BigDecimal cp = auction.getCurrentPrice();
                    List<Bid> eligible = active.stream()
                            .filter(b -> b.getMaxAutoBidAmount() != null && b.getMaxAutoBidAmount().compareTo(cp) > 0)
                            .sorted((a, b) -> {
                                int cmp = b.getMaxAutoBidAmount().compareTo(a.getMaxAutoBidAmount());
                                if (cmp == 0) return a.getId().compareTo(b.getId());
                                return cmp;
                            })
                            .toList();
                    if (eligible.size() >= 2) {
                        Bid top1 = eligible.get(0); // 최대금액 높은 사람 (예: B)
                        Bid top2 = eligible.get(1); // 두 번째 (예: A)
                        BigDecimal secondMax = top2.getMaxAutoBidAmount();
                        BigDecimal inc = calculateMinimumIncrement(secondMax);
                        BigDecimal winnerFinal = secondMax.add(inc);
                        if (winnerFinal.compareTo(top1.getMaxAutoBidAmount()) > 0) {
                            winnerFinal = top1.getMaxAutoBidAmount();
                        }
                        // 두 건만 저장: 패자(secondMax), 승자(winnerFinal)
                        persistFinalAutoBidOutcome(auction, cp, top2, top1, secondMax, winnerFinal);
                        return true;
                    }
                } catch (Exception ex) {
                    log.error("폴백 경쟁 처리 실패: auctionId={}, userId={}, error={}", auctionId, newAutoBidderId, ex.getMessage(), ex);
                }

                // 방어적 폴백 2: 경쟁이 불가하면 최소 증가분으로 단일 자동입찰 시도
                BigDecimal fallbackCurrent = auction.getCurrentPrice();
                BigDecimal fallbackNext = calculateNextAutoBidAmount(fallbackCurrent, 0);
                if (fallbackNext.compareTo(config.getMaxAutoBidAmount()) > 0) {
                    fallbackNext = config.getMaxAutoBidAmount();
                }
                if (fallbackNext.compareTo(fallbackCurrent) > 0) {
                    log.warn("⚠️ 경쟁 시뮬레이션 실패로 폴백 단일 자동입찰 실행 - {} → {}", fallbackCurrent, fallbackNext);
                    executeAutoBid(config, fallbackNext, auction);
                    return true;
                }
                return false;
            }
        } catch (Exception e) {
            log.error("triggerImmediateBidOnSetup error: auctionId={}, userId={}", auctionId, newAutoBidderId, e);
            return false;
        }
    }

    /**
     * 새 수동입찰 또는 임의의 입찰 직후 자동입찰 경쟁을 즉시 시뮬레이션하여 반영
     * - 최종 두 건만 DB 저장 (복수 자동입찰자)
     * - 단일 자동입찰자면 현재가 + 최소증가분으로 1건만 저장
     */
    @Transactional
    public boolean processCompetitionAfterNewBid(Long auctionId) {
        try {
            log.info("🚀 새 입찰 발생 - 자동입찰 즉시 경쟁 시작: auctionId={}", auctionId);

            Auction auction = auctionRepository.findByIdForUpdate(auctionId).orElse(null);
            if (auction == null || !auction.isActive()) {
                log.warn("❌ 비활성 경매 또는 없음: auctionId={}", auctionId);
                return false;
            }

            // 활성 자동입찰 설정 조회 후 사용자별 최신 설정으로 압축
            List<Bid> rawActiveAutoBids = bidRepository.findActiveAutoBidsByAuctionId(auctionId);
            Map<Long, Bid> latestByUser = new HashMap<>();
            for (Bid bid : rawActiveAutoBids) {
                if (bid.getBidder() == null) continue;
                Long uid = bid.getBidder().getId();
                Bid prev = latestByUser.get(uid);
                if (prev == null || bid.getId() > prev.getId()) {
                    latestByUser.put(uid, bid);
                }
            }
            List<Bid> activeAutoBids = new ArrayList<>(latestByUser.values());
            if (activeAutoBids.isEmpty()) {
                log.info("💤 활성 자동입찰 설정 없음: auctionId={}", auctionId);
                return false;
            }

            // 현재가 기준으로 즉시 경쟁 시뮬레이션 실행 (단, 단독 자격자면 명시적으로 한 번 올려준다)
            // cp를 Auction 엔티티가 아닌 실제 최고입찰에서 우선 산출하여 정합성 강화
            Bid highestActual = bidRepository.findTopActualByAuctionIdOrderByBidAmountDesc(auctionId).orElse(null);
            BigDecimal cp = highestActual != null ? highestActual.getBidAmount() : auction.getCurrentPrice();
            log.info("📊 경쟁 기준 현재가 결정 - actual={} / entity={}, 사용값={}",
                    highestActual != null ? highestActual.getBidAmount() : null,
                    auction.getCurrentPrice(), cp);

            List<Bid> eligible = activeAutoBids.stream()
                    .filter(b -> b.getMaxAutoBidAmount() != null && b.getMaxAutoBidAmount().compareTo(cp) > 0)
                    .toList();
            
            if (eligible.size() == 1) {
                Bid single = eligible.get(0);
                // 현재 최고입찰자 확인 (설정 레코드 제외)
                Bid currentHighest = highestActual; // 이미 실제 최고입찰 조회함
                Long currentHighestId = currentHighest != null && currentHighest.getBidder() != null ? currentHighest.getBidder().getId() : null;
                if (currentHighestId == null || !currentHighestId.equals(single.getBidder().getId())) {
                    BigDecimal next = calculateNextAutoBidAmount(cp, 0);
                    if (next.compareTo(single.getMaxAutoBidAmount()) > 0) {
                        next = single.getMaxAutoBidAmount();
                    }
                    if (next.compareTo(cp) > 0) {
                        log.info("⚡ 단독 자동입찰자 즉시 1회 상승: {} → {} (사용자: {})", cp, next, single.getBidder().getId());
                        executeAutoBid(single, next, auction);
                        return true;
                    }
                }
                log.info("💤 단독 자동입찰자이나 상승 불가 조건으로 처리 없음 (auctionId={})", auctionId);
                return false;
            }

            // 2명 이상이면 즉시 경쟁 시뮬레이션 실행
            processSmartAutoBiddingImmediate(activeAutoBids, cp, auction);
            return true;
        } catch (Exception e) {
            log.error("자동입찰 즉시 경쟁 처리 실패: auctionId={}, error={}", auctionId, e.getMessage(), e);
            return false;
        }
    }
}
