package com.cherrypick.app.domain.bid.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.bid.dto.response.BidResponse;
import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.bid.enums.BidStatus;
import com.cherrypick.app.domain.bid.repository.BidRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AutoBidService {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final BidValidationService validationService;

    /**
     * 자동 입찰 설정
     */
    @Transactional
    public BidResponse setupAutoBid(Long auctionId, Long bidderId, BigDecimal maxAutoBidAmount) {
        log.info("자동 입찰 설정 시작: auctionId={}, bidderId={}, maxAmount={}",
                auctionId, bidderId, maxAutoBidAmount);

        // 1. 경매 조회
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다"));

        // 2. 입찰자 조회
        User bidder = userRepository.findById(bidderId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 3. 기본 검증 (경매 상태, 본인 경매 여부)
        validationService.validateAuctionStatus(auction);
        validationService.validateNotSelfBid(auction, bidder);

        // 4. 최대 자동입찰 금액 검증
        validationService.validate100Unit(maxAutoBidAmount);

        BigDecimal currentPrice = auction.getCurrentPrice();
        BigDecimal minBidAmount = currentPrice.add(validationService.calculateMinimumIncrement(currentPrice));

        if (maxAutoBidAmount.compareTo(minBidAmount) < 0) {
            throw new IllegalArgumentException(
                    String.format("최대 자동입찰 금액은 현재가(%s원)보다 높아야 합니다 (최소: %s원)",
                            currentPrice, minBidAmount));
        }

        // 5. 기존 자동입찰 설정 취소
        Optional<Bid> existingAutoBid = bidRepository.findByAuctionIdAndBidderIdAndIsAutoBidTrueAndStatus(
                auctionId, bidderId, BidStatus.ACTIVE);
        existingAutoBid.ifPresent(Bid::cancel);

        // 6. 자동입찰 설정 레코드 저장 (bidAmount = 0)
        Bid autoBidSetting = Bid.createAutoBidSetting(auction, bidder, maxAutoBidAmount);
        bidRepository.save(autoBidSetting);

        log.info("자동 입찰 설정 저장 완료");

        // 7. 즉시 입찰 실행
        executeAutoBidImmediately(auction, bidder, maxAutoBidAmount);

        return BidResponse.from(autoBidSetting);
    }

    /**
     * 자동 입찰 즉시 실행 (설정 시점)
     */
    private void executeAutoBidImmediately(Auction auction, User bidder, BigDecimal maxAutoBidAmount) {
        log.info("자동 입찰 즉시 실행: auctionId={}, bidderId={}", auction.getId(), bidder.getId());

        // 현재 최고 입찰 확인
        Optional<BigDecimal> maxBidAmount = bidRepository.findMaxBidAmountByAuctionId(auction.getId());

        if (maxBidAmount.isEmpty()) {
            // 첫 입찰 - 시작가로 입찰
            createAutoBidExecution(auction, bidder, auction.getStartPrice(), maxAutoBidAmount);
            log.info("첫 자동 입찰: 시작가 {}", auction.getStartPrice());
            return;
        }

        BigDecimal currentPrice = auction.getCurrentPrice();

        // 이미 최고 입찰자인지 확인
        boolean isHighestBidder = bidRepository.isHighestBidder(auction.getId(), bidder.getId());
        if (isHighestBidder) {
            log.info("이미 최고 입찰자이므로 추가 입찰하지 않음");
            return;
        }

        // 다른 활성 자동입찰 설정 조회
        List<Bid> otherAutoBids = bidRepository.findActiveAutoBidSettingsExcludingBidder(
                auction.getId(), bidder.getId(), BidStatus.ACTIVE);

        if (otherAutoBids.isEmpty()) {
            // 다른 자동입찰 없음 - 현재가 + 최소 증가폭으로 입찰
            BigDecimal nextBidAmount = currentPrice.add(validationService.calculateMinimumIncrement(currentPrice));
            if (nextBidAmount.compareTo(maxAutoBidAmount) <= 0) {
                createAutoBidExecution(auction, bidder, nextBidAmount, maxAutoBidAmount);
                log.info("자동 입찰 실행: {}", nextBidAmount);
            }
        } else {
            // 다른 자동입찰과 경쟁
            performAutoBidCompetition(auction, bidder, maxAutoBidAmount, otherAutoBids);
        }
    }

    /**
     * 자동 입찰 경쟁 시뮬레이션 (eBay 스타일)
     */
    private void performAutoBidCompetition(Auction auction, User newBidder,
                                           BigDecimal newMaxAmount, List<Bid> otherAutoBids) {
        log.info("자동 입찰 경쟁 시뮬레이션 시작");

        // 가장 높은 최대 금액을 가진 기존 자동입찰 찾기
        Bid highestOtherAutoBid = otherAutoBids.stream()
                .max((a, b) -> a.getMaxAutoBidAmount().compareTo(b.getMaxAutoBidAmount()))
                .orElse(null);

        if (highestOtherAutoBid == null) {
            return;
        }

        BigDecimal otherMaxAmount = highestOtherAutoBid.getMaxAutoBidAmount();
        BigDecimal currentPrice = auction.getCurrentPrice();
        BigDecimal increment = validationService.calculateMinimumIncrement(currentPrice);

        log.info("경쟁: 신규 {}원 vs 기존 {}원", newMaxAmount, otherMaxAmount);

        // 경쟁 결과 결정
        if (newMaxAmount.compareTo(otherMaxAmount) > 0) {
            // 신규 입찰자 승리
            BigDecimal loserFinalBid = otherMaxAmount;
            BigDecimal winnerFinalBid = otherMaxAmount.add(increment);

            // 승자의 최종 입찰가가 최대 금액을 초과하지 않도록
            if (winnerFinalBid.compareTo(newMaxAmount) > 0) {
                winnerFinalBid = newMaxAmount;
            }

            // 2건 저장: 패자 최대금액, 승자 최종금액
            createAutoBidExecution(auction, highestOtherAutoBid.getBidder(), loserFinalBid, otherMaxAmount);
            createAutoBidExecution(auction, newBidder, winnerFinalBid, newMaxAmount);

            log.info("신규 입찰자 승리: {} -> {}", loserFinalBid, winnerFinalBid);
        } else if (newMaxAmount.compareTo(otherMaxAmount) < 0) {
            // 기존 입찰자 승리
            BigDecimal loserFinalBid = newMaxAmount;
            BigDecimal winnerFinalBid = newMaxAmount.add(increment);

            // 승자의 최종 입찰가가 최대 금액을 초과하지 않도록
            if (winnerFinalBid.compareTo(otherMaxAmount) > 0) {
                winnerFinalBid = otherMaxAmount;
            }

            // 2건 저장: 패자 최대금액, 승자 최종금액
            createAutoBidExecution(auction, newBidder, loserFinalBid, newMaxAmount);
            createAutoBidExecution(auction, highestOtherAutoBid.getBidder(), winnerFinalBid, otherMaxAmount);

            log.info("기존 입찰자 승리: {} -> {}", loserFinalBid, winnerFinalBid);
        } else {
            // 동일 금액 - 먼저 설정한 사람 승리
            BigDecimal finalBid = otherMaxAmount;
            createAutoBidExecution(auction, highestOtherAutoBid.getBidder(), finalBid, otherMaxAmount);
            log.info("동일 금액: 기존 입찰자 승리 {}", finalBid);
        }
    }

    /**
     * 자동 입찰 실행 레코드 생성
     */
    private void createAutoBidExecution(Auction auction, User bidder,
                                       BigDecimal bidAmount, BigDecimal maxAutoBidAmount) {
        Bid bid = Bid.createAutoBidExecution(auction, bidder, bidAmount, maxAutoBidAmount);
        bidRepository.save(bid);

        // 경매 현재가 업데이트
        if (bidAmount.compareTo(auction.getCurrentPrice()) > 0) {
            auction.updateCurrentPrice(bidAmount);
            auction.increaseBidCount();
            auctionRepository.save(auction);
        }

        log.info("자동 입찰 실행 저장: bidAmount={}", bidAmount);
    }

    /**
     * 자동 입찰 취소
     */
    @Transactional
    public void cancelAutoBid(Long auctionId, Long bidderId) {
        log.info("자동 입찰 취소: auctionId={}, bidderId={}", auctionId, bidderId);

        Optional<Bid> autoBidSetting = bidRepository.findByAuctionIdAndBidderIdAndIsAutoBidTrueAndStatus(
                auctionId, bidderId, BidStatus.ACTIVE);

        if (autoBidSetting.isPresent()) {
            autoBidSetting.get().cancel();
            bidRepository.save(autoBidSetting.get());
            log.info("자동 입찰 취소 완료");
        } else {
            log.warn("취소할 자동 입찰 설정을 찾을 수 없음");
        }
    }

    /**
     * 내 자동 입찰 설정 조회
     */
    public Optional<BidResponse> getMyAutoBidSetting(Long auctionId, Long bidderId) {
        Optional<Bid> autoBidSetting = bidRepository.findByAuctionIdAndBidderIdAndIsAutoBidTrueAndStatus(
                auctionId, bidderId, BidStatus.ACTIVE);

        return autoBidSetting.map(BidResponse::from);
    }
}