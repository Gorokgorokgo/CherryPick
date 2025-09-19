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
        Bid currentHighest = bidRepository.findTopByAuctionIdOrderByBidAmountDesc(auction.getId()).orElse(null);
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

            // 경매 정보 조회
            Auction auction = auctionRepository.findById(auctionId)
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
            processSmartAutoBiddingImmediate(activeAutoBids, currentPrice, auction);

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
        Bid currentHighestBid = bidRepository.findTopByAuctionIdOrderByBidAmountDesc(auction.getId()).orElse(null);
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
        for (int i = 0; i < eligibleBids.size(); i++) {
            Bid bidder = eligibleBids.get(i);
            if (bidder != null && bidder.getBidder() != null) {
                log.info("🔥 {}위: 입찰자 {} (최대: {}원)", i+1, bidder.getBidder().getId(), bidder.getMaxAutoBidAmount());
            } else {
                log.warn("⚠️ {}위: 입찰자 정보가 null - 자동입찰 ID: {}", i+1, bidder != null ? bidder.getId() : "null");
            }
        }

        // 현재 최고입찰자가 아닌 자동입찰자부터 입찰 시작 (체이닝)
        boolean hasActivity = false;
        BigDecimal updatedCurrentPrice = currentPrice;
        Long updatedCurrentHighestBidderId = currentHighestBidderId;

        // 현재 최고입찰자가 자동입찰자가 아닌 경우, 최고 자동입찰자부터 입찰
        // 현재 최고입찰자가 자동입찰자인 경우, 그 다음 자동입찰자부터 입찰
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
}