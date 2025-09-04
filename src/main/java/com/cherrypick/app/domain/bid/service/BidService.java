package com.cherrypick.app.domain.bid.service;

import com.cherrypick.app.common.exception.BusinessException;
import com.cherrypick.app.common.exception.EntityNotFoundException;
import com.cherrypick.app.common.exception.ErrorCode;
import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.bid.dto.request.PlaceBidRequest;
import com.cherrypick.app.domain.bid.dto.response.BidResponse;
import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.bid.enums.BidStatus;
import com.cherrypick.app.domain.bid.repository.BidRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import com.cherrypick.app.domain.websocket.service.WebSocketMessagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidService {
    
    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final WebSocketMessagingService webSocketMessagingService;
    private final AutoBidService autoBidService;
    
    /**
     * 입찰하기 (개정된 비즈니스 모델)
     * 
     * 비즈니스 로직:
     * 1. 경매 유효성 검증 (진행중, 종료시간 등)
     * 2. 입찰자 정보 확인
     * 3. 입찰 금액 유효성 검증 (최소 증가폭, 현재가 등)
     * 4. 새 입찰 등록
     * 5. 경매 현재가 업데이트
     * 
     * 개정된 정책:
     * - 포인트 예치(Lock) 시스템 완전 제거 (법적 리스크 해결)
     * - 무료 입찰로 참여 장벽 낮춤
     * - 레벨 시스템으로 신뢰도 관리
     * 
     * @param userId 입찰자 사용자 ID
     * @param request 입찰 요청 정보
     * @return 입찰 결과
     */
    @Transactional
    public BidResponse placeBid(Long userId, PlaceBidRequest request) {
        // 요청 데이터 유효성 검증
        request.validate();
        
        // 경매 정보 조회 및 유효성 검증
        Auction auction = auctionRepository.findById(request.getAuctionId())
                .orElseThrow(EntityNotFoundException::auction);
        
        validateAuctionForBidding(auction);
        
        // 입찰자 정보 확인
        User bidder = userRepository.findById(userId)
                .orElseThrow(EntityNotFoundException::user);
        
        // 자신의 경매에는 입찰할 수 없음
        if (auction.getSeller().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.SELF_BID_NOT_ALLOWED);
        }
        
        // 입찰 금액 유효성 검증
        validateBidAmount(auction, request.getBidAmount());
        
        // 입찰 생성
        Bid bid = Bid.builder()
                .auction(auction)
                .bidder(bidder)
                .bidAmount(request.getBidAmount())
                .isAutoBid(request.getIsAutoBid())
                .maxAutoBidAmount(request.getMaxAutoBidAmount())
                .autoBidPercentage(request.getAutoBidPercentage())
                .status(BidStatus.ACTIVE)
                .bidTime(LocalDateTime.now())
                .build();
        
        Bid savedBid = bidRepository.save(bid);
        
        // 경매 현재가 및 입찰수 업데이트
        auction.updateCurrentPrice(request.getBidAmount());
        auction.increaseBidCount();
        auctionRepository.save(auction);
        
        // 실시간 입찰 알림 전송 (WebSocket)
        webSocketMessagingService.notifyNewBid(
            auction.getId(),
            request.getBidAmount(),
            auction.getBidCount(),
            bidder.getNickname() != null ? bidder.getNickname() : "익명" + bidder.getId()
        );
        
        // 자동입찰 트리거 (수동 입찰에만 적용)
        if (!Boolean.TRUE.equals(request.getIsAutoBid())) {
            autoBidService.processAutoBidsForAuction(auction.getId(), request.getBidAmount());
        }
        
        return BidResponse.from(savedBid, true);
    }
    
    /**
     * 경매별 입찰 내역 조회
     */
    public Page<BidResponse> getBidsByAuction(Long auctionId, Pageable pageable) {
        Page<Bid> bids = bidRepository.findByAuctionIdOrderByBidAmountDesc(auctionId, pageable);
        
        return bids.map(bid -> {
            // 최고가 입찰인지 확인
            BigDecimal highestAmount = bidRepository.findHighestBidAmountByAuctionId(auctionId);
            boolean isHighestBid = bid.getBidAmount().equals(highestAmount);
            
            return BidResponse.from(bid, isHighestBid);
        });
    }
    
    /**
     * 내 입찰 내역 조회
     */
    public Page<BidResponse> getMyBids(Long userId, Pageable pageable) {
        Page<Bid> bids = bidRepository.findByBidderIdOrderByBidTimeDesc(userId, pageable);
        
        return bids.map(bid -> {
            // 최고가 입찰인지 확인
            BigDecimal highestAmount = bidRepository.findHighestBidAmountByAuctionId(bid.getAuction().getId());
            boolean isHighestBid = bid.getBidAmount().equals(highestAmount);
            
            return BidResponse.from(bid, isHighestBid);
        });
    }
    
    /**
     * 특정 경매의 최고가 입찰 조회
     */
    public BidResponse getHighestBid(Long auctionId) {
        Bid highestBid = bidRepository.findTopByAuctionIdOrderByBidAmountDesc(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_BID_EXISTS));
        
        return BidResponse.from(highestBid, true);
    }
    
    /**
     * 경매 입찰 유효성 검증
     */
    private void validateAuctionForBidding(Auction auction) {
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.AUCTION_NOT_ACTIVE);
        }
        
        if (auction.getEndAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.AUCTION_ENDED);
        }
    }
    
    /**
     * 입찰 금액 유효성 검증 (5-10% 비율 기반)
     */
    private void validateBidAmount(Auction auction, BigDecimal bidAmount) {
        BigDecimal currentPrice = auction.getCurrentPrice();
        
        // 1. 5% 증가 체크 (최소 입찰 증가율)
        BigDecimal minimumBid = currentPrice.multiply(BigDecimal.valueOf(1.05));
        
        // 2. 10% 이상 증가는 제한 (너무 큰 증가폭 방지)
        BigDecimal maximumBid = currentPrice.multiply(BigDecimal.valueOf(1.10));
        
        if (bidAmount.compareTo(minimumBid) < 0) {
            throw new BusinessException(ErrorCode.INVALID_BID_AMOUNT, 
                "입찰가는 현재가의 5% 이상 높아야 합니다.");
        }
        
        if (bidAmount.compareTo(maximumBid) > 0) {
            throw new BusinessException(ErrorCode.INVALID_BID_AMOUNT,
                "한 번에 10% 이상 증가한 입찰은 제한됩니다.");
        }
        
        // 3. 100원 단위 체크
        if (bidAmount.remainder(BigDecimal.valueOf(100)).compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(ErrorCode.INVALID_BID_AMOUNT, "입찰가는 100원 단위로 입력해주세요.");
        }
    }
    
    // 포인트 예치(Lock) 시스템 제거 - 법적 리스크 해결
    // 기존의 releaseExistingBidLocks()와 lockBidAmount() 메서드 제거
}