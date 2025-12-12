package com.cherrypick.app.domain.bid.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.bid.dto.response.BidResponse;
import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.bid.repository.BidRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import com.cherrypick.app.domain.websocket.service.WebSocketMessagingService;
import com.cherrypick.app.domain.notification.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
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
    private final AutoBidService autoBidService;
    private final WebSocketMessagingService webSocketMessagingService;
    private final FcmService fcmService;

    /**
     * 내 입찰 내역 조회
     */
    public Page<BidResponse> getMyBids(Long userId, Pageable pageable) {
        Page<Bid> bids = bidRepository.findByBidderIdOrderByBidTimeDesc(userId, pageable);
        return bids.map(BidResponse::from);
    }

    /**
     * 특정 경매의 입찰 내역 조회 (금액 높은 순)
     */
    public Page<BidResponse> getAuctionBids(Long auctionId, Pageable pageable) {
        Page<Bid> bids = bidRepository.findByAuctionIdAndBidAmountGreaterThanOrderByBidAmountDesc(
            auctionId,
            BigDecimal.ZERO,
            pageable
        );
        return bids.map(bid -> BidResponse.from(bid, false));
    }

    /**
     * 수동 입찰 처리
     */
    @Transactional
    public BidResponse placeBid(Long auctionId, Long bidderId, BigDecimal bidAmount) {
        log.info("수동 입찰 시작: auctionId={}, bidderId={}, bidAmount={}", auctionId, bidderId, bidAmount);

        // 1. 경매 조회 (비관적 잠금으로 동시성 보장)
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
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

        // 7. 스나이핑 방지: 종료 3분 이내 입찰 시 시간 연장
        boolean extended = auction.extendEndTimeIfWithinSnipingWindow();
        if (extended) {
            log.info("스나이핑 방지: 경매 종료 시간 연장됨 - auctionId={}, newEndAt={}",
                    auction.getId(), auction.getEndAt());
        }

        auctionRepository.save(auction);

        // 스나이핑 방지 시간 연장 알림
        if (extended) {
            // WebSocket 실시간 알림 (경매 상세 화면에 표시)
            webSocketMessagingService.notifyAuctionExtended(
                    auction.getId(),
                    auction.getEndAt(),
                    auction.getCurrentPrice(),
                    auction.getBidCount()
            );

            // 입찰자들에게 푸시 알림 전송 (현재 입찰자 제외)
            List<User> bidders = bidRepository.findDistinctBiddersByAuctionId(auctionId);
            for (User bidderUser : bidders) {
                if (!bidderUser.getId().equals(bidderId)) {
                    try {
                        fcmService.sendAuctionExtendedNotification(bidderUser, auctionId, auction.getTitle());
                    } catch (Exception e) {
                        log.warn("스나이핑 연장 알림 전송 실패: userId={}, error={}", bidderUser.getId(), e.getMessage());
                    }
                }
            }
        }

        log.info("경매 업데이트 완료: currentPrice={}, bidCount={}", auction.getCurrentPrice(), auction.getBidCount());

        // 8. 자동 입찰 반응 처리
        try {
            autoBidService.reactToManualBid(auction, bidAmount);
        } catch (Exception e) {
            log.error("자동 입찰 반응 처리 중 오류 발생", e);
            // 수동 입찰은 성공했으므로 오류를 던지지 않고 로깅만 함
        }

        // 9. 응답 생성
        return BidResponse.from(savedBid, true);
    }
}