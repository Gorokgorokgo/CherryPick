package com.cherrypick.app.domain.auction.service;

import com.cherrypick.app.domain.auction.dto.AuctionResponse;
import com.cherrypick.app.domain.auction.dto.CreateAuctionRequest;
import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.entity.AuctionImage;
import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.auction.repository.AuctionImageRepository;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionService {
    
    private final AuctionRepository auctionRepository;
    private final AuctionImageRepository auctionImageRepository;
    private final UserRepository userRepository;
    
    /**
     * 경매 등록
     * 
     * 비즈니스 로직:
     * 1. 경매 정보 유효성 검증 (가격, 시간, 지역 등)
     * 2. 판매자 존재 여부 확인
     * 3. 보증금 계산 및 잔액 확인 (희망가의 10%)
     * 4. 보증금 차감 (경매 등록 시 선차감)
     * 5. 경매 시작/종료 시간 자동 계산
     * 6. 경매 엔티티 생성 (초기 상태는 ACTIVE)
     * 7. 다중 이미지 저장 (순서 보장)
     * 
     * 보증금 정책:
     * - 희망가의 10% 선차감
     * - 경매 성공 시 반환, 실패 시 일부 차감 가능
     * - 허위 매물 등록 방지 목적
     * 
     * @param userId 판매자 사용자 ID
     * @param request 경매 등록 요청 정보
     * @return 등록된 경매 정보
     * @throws IllegalArgumentException 유효하지 않은 요청, 보증금 부족
     */
    @Transactional
    public AuctionResponse createAuction(Long userId, CreateAuctionRequest request) {
        // 경매 정보 유효성 검증 (가격 관계, 1000원 단위, 지역 정보 등)
        request.validate();
        
        // 판매자 존재 확인
        User seller = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        // 보증금 계산 (희망가의 10%)
        Long depositAmount = request.calculateDepositAmount().longValue();
        
        // 보증금 잔액 확인
        if (seller.getPointBalance() < depositAmount) {
            throw new IllegalArgumentException("보증금이 부족합니다. 필요 보증금: " + String.format("%,d", depositAmount) + "원");
        }
        
        // 보증금 선차감 (허위 매물 등록 방지)
        seller.setPointBalance(seller.getPointBalance() - depositAmount);
        userRepository.save(seller);
        
        // 경매 시작/종료 시간 자동 계산
        LocalDateTime startAt = LocalDateTime.now();
        LocalDateTime endAt = startAt.plusHours(request.getAuctionTimeHours());
        
        // 경매 엔티티 생성 (정적 팩토리 메서드 사용)
        Auction auction = Auction.createAuction(
                seller,
                request.getTitle(),
                request.getDescription(),
                request.getCategory(),
                request.getStartPrice(),
                request.getHopePrice(),
                request.getReservePrice(),
                request.getAuctionTimeHours(),
                request.getRegionScope(),
                request.getRegionCode(),
                request.getRegionName()
        );
        
        Auction savedAuction = auctionRepository.save(auction);
        
        // 상품 이미지 저장 (순서 보장)
        List<AuctionImage> images = saveAuctionImages(savedAuction, request.getImageUrls());
        
        return AuctionResponse.from(savedAuction, images);
    }
    
    public Page<AuctionResponse> getActiveAuctions(Pageable pageable) {
        Page<Auction> auctions = auctionRepository.findByStatusOrderByCreatedAtDesc(AuctionStatus.ACTIVE, pageable);
        
        return auctions.map(auction -> {
            List<AuctionImage> images = auctionImageRepository.findByAuctionIdOrderBySortOrder(auction.getId());
            return AuctionResponse.from(auction, images);
        });
    }
    
    public Page<AuctionResponse> getAuctionsByCategory(Category category, Pageable pageable) {
        Page<Auction> auctions = auctionRepository.findByCategoryAndStatusOrderByCreatedAtDesc(category, AuctionStatus.ACTIVE, pageable);
        
        return auctions.map(auction -> {
            List<AuctionImage> images = auctionImageRepository.findByAuctionIdOrderBySortOrder(auction.getId());
            return AuctionResponse.from(auction, images);
        });
    }
    
    public Page<AuctionResponse> getAuctionsByRegion(RegionScope regionScope, String regionCode, Pageable pageable) {
        Page<Auction> auctions;
        
        if (regionScope == RegionScope.NATIONWIDE) {
            auctions = auctionRepository.findByRegionScopeAndStatusOrderByCreatedAtDesc(regionScope, AuctionStatus.ACTIVE, pageable);
        } else {
            auctions = auctionRepository.findByRegionScopeAndRegionCodeAndStatusOrderByCreatedAtDesc(regionScope, regionCode, AuctionStatus.ACTIVE, pageable);
        }
        
        return auctions.map(auction -> {
            List<AuctionImage> images = auctionImageRepository.findByAuctionIdOrderBySortOrder(auction.getId());
            return AuctionResponse.from(auction, images);
        });
    }
    
    @Transactional
    public AuctionResponse getAuctionDetail(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다."));
        
        // 조회수 증가 (비즈니스 메서드 사용)
        auction.increaseViewCount();
        auctionRepository.save(auction);
        
        List<AuctionImage> images = auctionImageRepository.findByAuctionIdOrderBySortOrder(auctionId);
        return AuctionResponse.from(auction, images);
    }
    
    public Page<AuctionResponse> getMyAuctions(Long userId, Pageable pageable) {
        Page<Auction> auctions = auctionRepository.findBySellerIdOrderByCreatedAtDesc(userId, pageable);
        
        return auctions.map(auction -> {
            List<AuctionImage> images = auctionImageRepository.findByAuctionIdOrderBySortOrder(auction.getId());
            return AuctionResponse.from(auction, images);
        });
    }
    
    private List<AuctionImage> saveAuctionImages(Auction auction, List<String> imageUrls) {
        List<AuctionImage> images = imageUrls.stream()
                .map(url -> AuctionImage.builder()
                        .auction(auction)
                        .imageUrl(url)
                        .sortOrder(imageUrls.indexOf(url))
                        .build())
                .toList();
        
        return auctionImageRepository.saveAll(images);
    }
    
    /**
     * 경매 종료 처리 (Reserve Price 고려)
     * 
     * 비즈니스 로직:
     * 1. 최고 입찰가와 Reserve Price 비교
     * 2. Reserve Price 미달시 유찰 처리
     * 3. 보증금 및 예치 포인트 처리
     * 4. 알림 발송
     * 
     * @param auctionId 경매 ID
     * @param highestBidPrice 최고 입찰가
     * @param highestBidder 최고 입찰자
     */
    @Transactional
    public void processAuctionEnd(Long auctionId, BigDecimal highestBidPrice, User highestBidder) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다."));
        
        // 경매 상태 확인
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("이미 종료된 경매입니다.");
        }
        
        User seller = auction.getSeller();
        
        if (auction.hasReservePrice() && !auction.isReservePriceMet(highestBidPrice)) {
            // Reserve Price 미달 - 유찰 처리
            processFailedAuction(auction, seller);
        } else {
            // 정상 낙찰 처리
            processSuccessfulAuction(auction, seller, highestBidder, highestBidPrice);
        }
        
        auctionRepository.save(auction);
    }
    
    /**
     * 유찰 처리
     */
    private void processFailedAuction(Auction auction, User seller) {
        // 1. 경매 상태를 유찰로 변경
        auction.endAuction(null, BigDecimal.ZERO);
        
        // 2. 판매자 보증금 100% 반환 (판매자 귀책 아님)
        BigDecimal depositAmount = auction.getDepositAmount();
        seller.setPointBalance(seller.getPointBalance() + depositAmount.longValue());
        userRepository.save(seller);
        
        // 3. 모든 입찰자의 예치 포인트 해제 (별도 서비스에서 처리)
        // bidService.releaseAllBidderPoints(auction.getId());
        
        // 4. 유찰 알림 발송 (별도 서비스에서 처리)
        // notificationService.sendAuctionFailedNotification(auction);
        
        System.out.println("경매 유찰 처리 완료: " + auction.getTitle() + " (Reserve Price 미달)");
    }
    
    /**
     * 정상 낙찰 처리
     */
    private void processSuccessfulAuction(Auction auction, User seller, User winner, BigDecimal finalPrice) {
        // 1. 경매 상태를 낙찰 완료로 변경
        auction.endAuction(winner, finalPrice);
        
        // 2. 판매자 보증금 100% 반환
        BigDecimal depositAmount = auction.getDepositAmount();
        seller.setPointBalance(seller.getPointBalance() + depositAmount.longValue());
        userRepository.save(seller);
        
        // 3. 낙찰자를 제외한 나머지 입찰자들의 예치 포인트 해제 (별도 서비스에서 처리)
        // bidService.releaseNonWinnerPoints(auction.getId(), winner.getId());
        
        // 4. 낙찰 알림 발송 (별도 서비스에서 처리)
        // notificationService.sendAuctionWonNotification(auction, winner);
        
        System.out.println("경매 낙찰 처리 완료: " + auction.getTitle() + " -> " + winner.getNickname() + " (" + finalPrice + "원)");
    }
}