package com.cherrypick.app.domain.auction.service;

import com.cherrypick.app.domain.auction.dto.AuctionResponse;
import com.cherrypick.app.domain.auction.dto.AuctionSearchRequest;
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
import com.cherrypick.app.common.exception.BusinessException;
import com.cherrypick.app.common.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionService {
    
    private final AuctionRepository auctionRepository;
    private final AuctionImageRepository auctionImageRepository;
    private final UserRepository userRepository;
    private final com.cherrypick.app.domain.chat.service.ChatService chatService;
    private final com.cherrypick.app.domain.bid.repository.BidRepository bidRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * 경매 등록 (개정된 비즈니스 모델)
     * 
     * 비즈니스 로직:
     * 1. 경매 정보 유효성 검증 (가격, 시간, 지역 등)
     * 2. 판매자 존재 여부 확인
     * 3. 경매 시작/종료 시간 자동 계산
     * 4. 경매 엔티티 생성 (초기 상태는 ACTIVE)
     * 5. 다중 이미지 저장 (순서 보장)
     * 
     * 개정된 정책:
     * - 보증금 시스템 완전 제거 (법적 리스크 해결)
     * - 무료 경매 등록으로 진입장벽 낮춤
     * - 레벨 시스템으로 신뢰도 관리
     * 
     * @param userId 판매자 사용자 ID
     * @param request 경매 등록 요청 정보
     * @return 등록된 경매 정보
     * @throws IllegalArgumentException 유효하지 않은 요청
     */
    @Transactional
    public AuctionResponse createAuction(Long userId, CreateAuctionRequest request) {
        // 경매 정보 유효성 검증 (가격 관계, 1000원 단위, 지역 정보 등)
        request.validate();
        
        // 판매자 존재 확인
        User seller = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        // 경매 엔티티 생성 (정적 팩토리 메서드에서 시간 자동 계산)
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
                request.getRegionName(),
                request.getProductCondition(),
                request.getPurchaseDate()
        );
        
        Auction savedAuction = auctionRepository.save(auction);
        
        // 상품 이미지 저장 (순서 보장)
        List<AuctionImage> images = saveAuctionImages(savedAuction, request.getImageUrls());
        
        return AuctionResponse.from(savedAuction, images);
    }
    
    public Page<AuctionResponse> getActiveAuctions(Pageable pageable) {
        Page<Auction> auctions = auctionRepository.findByStatusOrderByCreatedAtDesc(AuctionStatus.ACTIVE, pageable);

        return createAuctionResponsePage(auctions);
    }
    
    public Page<AuctionResponse> getAuctionsByCategory(Category category, Pageable pageable) {
        Page<Auction> auctions = auctionRepository.findByCategoryAndStatusOrderByCreatedAtDesc(category, AuctionStatus.ACTIVE, pageable);

        return createAuctionResponsePage(auctions);
    }
    
    public Page<AuctionResponse> getAuctionsByRegion(RegionScope regionScope, String regionCode, Pageable pageable) {
        Page<Auction> auctions;

        if (regionScope == RegionScope.NATIONWIDE) {
            auctions = auctionRepository.findByRegionScopeAndStatusOrderByCreatedAtDesc(regionScope, AuctionStatus.ACTIVE, pageable);
        } else {
            auctions = auctionRepository.findByRegionScopeAndRegionCodeAndStatusOrderByCreatedAtDesc(regionScope, regionCode, AuctionStatus.ACTIVE, pageable);
        }

        return createAuctionResponsePage(auctions);
    }
    
    public AuctionResponse getAuctionDetail(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다."));
        
        // 조회수 증가 로직을 제거 - 별도 API에서 처리하도록 변경
        
        List<AuctionImage> images = auctionImageRepository.findByAuctionIdOrderBySortOrder(auctionId);
        return AuctionResponse.from(auction, images);
    }
    
    /**
     * 조회수 증가 없이 경매 상세 정보만 조회
     */
    public AuctionResponse getAuctionDetailWithoutViewIncrement(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다."));
        
        List<AuctionImage> images = auctionImageRepository.findByAuctionIdOrderBySortOrder(auctionId);
        return AuctionResponse.from(auction, images);
    }
    
    /**
     * 조회수만 증가시키는 메서드
     */
    @Transactional
    public void increaseAuctionViewCount(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다."));
        
        auction.increaseViewCount();
        auctionRepository.save(auction);
    }
    
    public Page<AuctionResponse> getMyAuctions(Long userId, Pageable pageable) {
        Page<Auction> auctions = auctionRepository.findBySellerIdOrderByCreatedAtDesc(userId, pageable);

        return createAuctionResponsePage(auctions);
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
     * N+1 문제 해결을 위한 헬퍼 메서드 - 경매 목록과 이미지를 효율적으로 조합
     */
    private Page<AuctionResponse> createAuctionResponsePage(Page<Auction> auctions) {
        // 경매가 없으면 빈 페이지 반환
        if (auctions.isEmpty()) {
            return auctions.map(auction -> AuctionResponse.from(auction, List.of()));
        }

        // 모든 경매의 이미지를 한 번에 조회
        List<Long> auctionIds = auctions.getContent().stream()
                .map(Auction::getId)
                .toList();

        List<AuctionImage> allImages = auctionImageRepository.findByAuctionIdInOrderByAuctionIdAndSortOrder(auctionIds);

        // 경매 ID별로 이미지 그룹핑
        Map<Long, List<AuctionImage>> imageMap = allImages.stream()
                .collect(Collectors.groupingBy(image -> image.getAuction().getId()));

        return auctions.map(auction -> {
            List<AuctionImage> images = imageMap.getOrDefault(auction.getId(), List.of());
            return AuctionResponse.from(auction, images);
        });
    }
    
    /**
     * 경매 종료 처리 (개정된 비즈니스 모델)
     * 
     * 비즈니스 로직:
     * 1. 최고 입찰가와 Reserve Price 비교
     * 2. Reserve Price 미달시 유찰 처리
     * 3. 연결 서비스 생성 (정상 낙찰 시)
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
     * 유찰 처리 (개정된 비즈니스 모델)
     */
    private void processFailedAuction(Auction auction, User seller) {
        // 1. 경매 상태를 유찰로 변경
        auction.endAuction(null, BigDecimal.ZERO);
        
        // 2. 유찰 알림 발송 (별도 서비스에서 처리)
        // notificationService.sendAuctionFailedNotification(auction);

        log.info("경매 유찰 처리 완료: {} (Reserve Price 미달)", auction.getTitle());
    }
    
    /**
     * 정상 낙찰 처리 (개정된 비즈니스 모델)
     */
    private void processSuccessfulAuction(Auction auction, User seller, User winner, BigDecimal finalPrice) {
        // 1. 경매 상태를 낙찰 완료로 변경
        auction.endAuction(winner, finalPrice);
        
        // 2. 연결 서비스 생성 (수수료 결제 후 채팅 연결)
        // ConnectionService connectionService = ConnectionService.createConnection(auction, seller, winner, finalPrice);
        // connectionServiceRepository.save(connectionService);
        
        // 3. 낙찰 알림 발송 (별도 서비스에서 처리)
        // notificationService.sendAuctionWonNotification(auction, winner);

        log.info("경매 낙찰 처리 완료: {} -> {} ({}원), 연결 서비스 대기 중",
                auction.getTitle(), winner.getNickname(), finalPrice);
    }
    
    // === 고급 검색 및 필터링 기능 ===
    
    /**
     * 통합 검색 기능 - 키워드, 카테고리, 지역, 가격 범위 등 복합 조건 검색
     * 
     * @param searchRequest 검색 조건
     * @param pageable 페이지 정보
     * @return 검색 결과
     */
    public Page<AuctionResponse> searchAuctions(AuctionSearchRequest searchRequest, Pageable pageable) {
        // 검색 조건 검증
        searchRequest.validatePriceRange();
        
        // 정렬 조건에 따른 Pageable 생성
        Pageable sortedPageable = createSortedPageable(searchRequest.getSortBy(), pageable);
        
        // 마감 임박 시간 계산
        LocalDateTime endingSoonTime = null;
        if (searchRequest.getEndingSoonHours() != null) {
            endingSoonTime = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul")).plusHours(searchRequest.getEndingSoonHours());
        }
        
        Page<Auction> auctions;
        
        // 특별한 정렬 조건들 처리
        if (searchRequest.getSortBy() == AuctionSearchRequest.SortOption.ENDING_SOON) {
            // 마감 임박 순 정렬 - 별도 쿼리 사용
            auctions = auctionRepository.findActiveAuctionsOrderByEndingSoon(sortedPageable);
        } else {
            // 일반 복합 검색
            auctions = auctionRepository.searchAuctions(
                searchRequest.getStatus(),
                searchRequest.getKeyword(),
                searchRequest.getCategory(),
                searchRequest.getRegionScope(),
                searchRequest.getRegionCode(),
                searchRequest.getMinPrice(),
                searchRequest.getMaxPrice(),
                searchRequest.getMinBidCount(),
                endingSoonTime,
                sortedPageable
            );
        }
        
        return createAuctionResponsePage(auctions);
    }
    
    /**
     * 키워드 검색 (제목 + 설명)
     */
    public Page<AuctionResponse> searchByKeyword(String keyword, AuctionStatus status, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAuctionsByStatus(status, pageable);
        }
        
        Page<Auction> auctions = auctionRepository.searchByKeyword(keyword.trim(), status, pageable);
        
        return createAuctionResponsePage(auctions);
    }
    
    /**
     * 가격 범위로 검색
     */
    public Page<AuctionResponse> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, 
                                                   AuctionStatus status, Pageable pageable) {
        // 가격 범위 검증
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("최소 가격은 최대 가격보다 작아야 합니다.");
        }
        
        // 기본값 설정
        if (minPrice == null) minPrice = BigDecimal.ZERO;
        if (maxPrice == null) maxPrice = new BigDecimal("999999999"); // 매우 큰 값
        
        Page<Auction> auctions = auctionRepository.findByPriceRange(minPrice, maxPrice, status, pageable);
        
        return createAuctionResponsePage(auctions);
    }
    
    /**
     * 마감 임박 경매 조회 (N시간 이내)
     */
    public Page<AuctionResponse> getEndingSoonAuctions(int hours, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        LocalDateTime endTime = now.plusHours(hours);
        
        Page<Auction> auctions = auctionRepository.findEndingSoon(now, endTime, pageable);
        
        return createAuctionResponsePage(auctions);
    }
    
    /**
     * 인기 경매 조회 (입찰 수 기준)
     */
    public Page<AuctionResponse> getPopularAuctions(int minBidCount, AuctionStatus status, Pageable pageable) {
        Page<Auction> auctions = auctionRepository.findByMinBidCount(minBidCount, status, pageable);
        
        return createAuctionResponsePage(auctions);
    }
    
    /**
     * 상태별 경매 조회 (기존 getActiveAuctions의 일반화)
     */
    public Page<AuctionResponse> getAuctionsByStatus(AuctionStatus status, Pageable pageable) {
        Page<Auction> auctions = auctionRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        
        return createAuctionResponsePage(auctions);
    }
    
    /**
     * 정렬 조건에 따른 Pageable 생성
     */
    private Pageable createSortedPageable(AuctionSearchRequest.SortOption sortOption, Pageable pageable) {
        Sort sort = switch (sortOption) {
            case CREATED_ASC -> Sort.by(Sort.Direction.ASC, "createdAt");
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "currentPrice");
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "currentPrice");
            case VIEW_COUNT_DESC -> Sort.by(Sort.Direction.DESC, "viewCount");
            case BID_COUNT_DESC -> Sort.by(Sort.Direction.DESC, "bidCount");
            case ENDING_SOON -> Sort.by(Sort.Direction.ASC, "endAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt"); // CREATED_DESC (기본값)
        };
        
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
    
    /**
     * 경매 강제 종료
     */
    @Transactional
    public AuctionResponse forceEndAuction(Long auctionId) {
        log.info("경매 ID {} 강제 종료 요청됨", auctionId);
        
        // 경매 조회
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));
        
        // 이미 종료된 경매인지 확인
        if (auction.isEnded()) {
            log.warn("이미 종료된 경매입니다: ID={}", auctionId);
            List<AuctionImage> images = auctionImageRepository.findByAuctionIdOrderBySortOrder(auction.getId());
            return AuctionResponse.from(auction, images);
        }
        
        // 최고 입찰자를 낙찰자로 설정
        log.info("최고 입찰자 조회 시작: 경매ID={}", auctionId);
        Optional<com.cherrypick.app.domain.bid.entity.Bid> highestBid = bidRepository.findTopByAuctionIdOrderByBidAmountDesc(auctionId);
        
        if (highestBid.isPresent()) {
            log.info("최고 입찰 발견: 입찰자={}, 입찰액={}", 
                    highestBid.get().getBidder().getNickname(), highestBid.get().getBidAmount());
            auction.setWinner(highestBid.get().getBidder(), highestBid.get().getBidAmount());
            log.info("낙찰자 설정 완료: 경매ID={}, 낙찰자={}, 낙찰가={}", 
                    auctionId, highestBid.get().getBidder().getNickname(), highestBid.get().getBidAmount());
        } else {
            log.warn("최고 입찰을 찾을 수 없습니다: 경매ID={}", auctionId);
            
            // 추가 디버깅: 해당 경매의 모든 입찰 조회
            List<com.cherrypick.app.domain.bid.entity.Bid> allBids = bidRepository.findByAuctionOrderByBidAmountDesc(auction);
            log.info("해당 경매의 총 입찰 수: {}", allBids.size());
            for (com.cherrypick.app.domain.bid.entity.Bid bid : allBids) {
                log.info("입찰 내역: 입찰자={}, 금액={}, 시간={}", 
                        bid.getBidder().getNickname(), bid.getBidAmount(), bid.getBidTime());
            }
        }
        
        // 경매 강제 종료
        auction.forceEnd();
        Auction savedAuction = auctionRepository.save(auction);
        
        log.info("경매 강제 종료 완료: ID={}, 상태={}", auctionId, savedAuction.getStatus());
        
        // 낙찰자가 있으면 채팅방 자동 생성
        if (savedAuction.getWinner() != null) {
            try {
                chatService.createAuctionChatRoom(savedAuction, savedAuction.getSeller(), savedAuction.getWinner());
                log.info("경매 종료 후 채팅방 생성 완료: 경매ID={}, 판매자={}, 낙찰자={}", 
                        savedAuction.getId(), savedAuction.getSeller().getId(), savedAuction.getWinner().getId());
            } catch (Exception e) {
                log.error("채팅방 생성 실패: 경매ID={}, 오류={}", savedAuction.getId(), e.getMessage(), e);
            }
        } else {
            log.info("낙찰자가 없어 채팅방을 생성하지 않습니다: 경매ID={}", savedAuction.getId());
        }
        
        List<AuctionImage> images = auctionImageRepository.findByAuctionIdOrderBySortOrder(savedAuction.getId());
        return AuctionResponse.from(savedAuction, images);
    }
    
    /**
     * 경매 종료 후 처리 (단순 로깅)
     */
    @Transactional
    public void processAuctionEnd(Auction auction) {
        log.info("경매 종료됨: ID={}, 제목={}", auction.getId(), auction.getTitle());
        log.info("프론트엔드에서 타이머가 0초가 되면 자동으로 채팅방이 생성됩니다.");
    }
}