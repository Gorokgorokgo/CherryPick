package com.cherrypick.app.domain.bid.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.bid.dto.response.BidResponse;
import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.bid.repository.BidRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidService {

    private final BidRepository bidRepository;
    private final BidValidationService validationService;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    /**
     * 내 입찰 내역 조회
     */
    public Page<BidResponse> getMyBids(Long userId, Pageable pageable) {
        Page<Bid> bids = bidRepository.findByBidderIdOrderByBidTimeDesc(userId, pageable);
        return bids.map(BidResponse::from);
    }

    /**
     * 수동 입찰 처리
     */
    @Transactional
    public BidResponse placeBid(Long auctionId, Long bidderId, BigDecimal bidAmount) {
        log.info("수동 입찰 시작: auctionId={}, bidderId={}, bidAmount={}", auctionId, bidderId, bidAmount);

        // 1. 경매 조회
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다"));

        // 2. 입찰자 조회
        User bidder = userRepository.findById(bidderId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 3. 첫 입찰 여부 확인
        Optional<BigDecimal> maxBidAmount = bidRepository.findMaxBidAmountByAuctionId(auctionId);
        boolean isFirstBid = maxBidAmount.isEmpty();

        log.info("첫 입찰 여부: {}, 현재 최고가: {}", isFirstBid, maxBidAmount.orElse(null));

        // 4. 입찰 검증
        validationService.validateBid(auction, bidder, bidAmount, isFirstBid);

        // 5. 입찰 생성 및 저장
        Bid bid = Bid.createManualBid(auction, bidder, bidAmount);
        Bid savedBid = bidRepository.save(bid);

        log.info("입찰 저장 완료: bidId={}", savedBid.getId());

        // 6. 경매 현재가 및 입찰 횟수 업데이트
        auction.updateCurrentPrice(bidAmount);
        auction.increaseBidCount();
        auctionRepository.save(auction);

        log.info("경매 업데이트 완료: currentPrice={}, bidCount={}", auction.getCurrentPrice(), auction.getBidCount());

        // 7. 응답 생성
        return BidResponse.from(savedBid, true);
    }
}