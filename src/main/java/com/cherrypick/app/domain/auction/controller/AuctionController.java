package com.cherrypick.app.domain.auction.controller;

import com.cherrypick.app.domain.auction.dto.AuctionResponse;
import com.cherrypick.app.domain.auction.dto.AuctionSearchRequest;
import com.cherrypick.app.domain.auction.dto.CreateAuctionRequest;
import com.cherrypick.app.domain.auction.service.AuctionService;
import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "6단계 - 경매 관리", description = "경매 등록, 조회, 검색 | 보증금 10% 자동 차감")
@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {
    
    private final AuctionService auctionService;
    private final UserService userService;
    
    @Operation(summary = "경매 등록", 
               description = """
                   새로운 경매를 등록합니다.
                   
                   **사용 예시:**
                   ```json
                   {
                     "title": "아이폰 14 Pro 256GB 스페이스 블랙",
                     "description": "작년 12월 구매, 케이스와 함께 깨끗하게 사용했습니다. 배터리 상태 95%",
                     "category": "ELECTRONICS",
                     "startingPrice": 800000,
                     "hopedPrice": 1200000,
                     "reservePrice": 1000000,
                     "regionScope": "NATIONAL",
                     "regionCode": "11",
                     "auctionHours": 24,
                     "imageUrls": [
                       "https://s3.aws.com/cherrypick/phone1.jpg",
                       "https://s3.aws.com/cherrypick/phone2.jpg"
                     ]
                   }
                   ```
                   
                   **중요 사항:**
                   - 보증금: 희망가의 10% 자동 차감 (예: 희망가 120만원 → 보증금 12만원)
                   - Reserve Price: 최저 내정가 설정 가능 (선택사항, 입찰자에게 비공개)
                   - 가격 범위: 시작가 ≤ Reserve Price ≤ 희망가
                   - 유찰 조건: 최고 입찰가 < Reserve Price시 유찰 처리
                   - 이미지: 최소 1개 이상 필수
                   - 기본 경매 시간: 24시간 (최대 168시간/7일)
                   - 스나이핑 방지: 마지막 10분 내 입찰시 10분 연장
                   - 지역 확장: 입찰 부족시 자동으로 확장 범위 제안
                   - 노쇼 방지: 정상 거래 완료시 보증금 100% 반환
                   """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "경매 등록 성공"),
            @ApiResponse(responseCode = "400", description = "보증금 부족, 이미지 누락, 유효하지 않은 카테고리/지역"),
            @ApiResponse(responseCode = "401", description = "JWT 토큰 인증 실패")
    })
    @PostMapping
    public ResponseEntity<AuctionResponse> createAuction(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateAuctionRequest request) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        AuctionResponse response = auctionService.createAuction(userId, request);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "진행중인 경매 목록 조회", description = "현재 진행중인 모든 경매를 페이지네이션으로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "경매 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 페이지 정보")
    })
    @GetMapping
    public ResponseEntity<Page<AuctionResponse>> getActiveAuctions(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuctionResponse> auctions = auctionService.getActiveAuctions(pageable);
        return ResponseEntity.ok(auctions);
    }
    
    @Operation(summary = "카테고리별 경매 조회", description = "특정 카테고리의 진행중인 경매를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "카테고리별 경매 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 카테고리 또는 페이지 정보")
    })
    @GetMapping("/category/{category}")
    public ResponseEntity<Page<AuctionResponse>> getAuctionsByCategory(
            @Parameter(description = "경매 카테고리") @PathVariable Category category,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuctionResponse> auctions = auctionService.getAuctionsByCategory(category, pageable);
        return ResponseEntity.ok(auctions);
    }
    
    @Operation(summary = "지역별 경매 조회", description = "특정 지역의 진행중인 경매를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "지역별 경매 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 지역 정보 또는 페이지 정보")
    })
    @GetMapping("/region")
    public ResponseEntity<Page<AuctionResponse>> getAuctionsByRegion(
            @Parameter(description = "지역 범위 (NATIONAL, REGIONAL)") @RequestParam RegionScope regionScope,
            @Parameter(description = "지역 코드 (지역별 검색시 필요)") @RequestParam(required = false) String regionCode,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuctionResponse> auctions = auctionService.getAuctionsByRegion(regionScope, regionCode, pageable);
        return ResponseEntity.ok(auctions);
    }
    
    @Operation(summary = "경매 상세 조회", description = "특정 경매의 상세 정보를 조회합니다. 조회수가 1 증가됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "경매 상세 조회 성공"),
            @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음")
    })
    @GetMapping("/{auctionId}")
    public ResponseEntity<AuctionResponse> getAuctionDetail(
            @Parameter(description = "경매 ID") @PathVariable Long auctionId) {
        AuctionResponse response = auctionService.getAuctionDetail(auctionId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "내 경매 목록 조회", description = "로그인한 사용자가 등록한 경매 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "내 경매 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/my")
    public ResponseEntity<Page<AuctionResponse>> getMyAuctions(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        Pageable pageable = PageRequest.of(page, size);
        Page<AuctionResponse> auctions = auctionService.getMyAuctions(userId, pageable);
        return ResponseEntity.ok(auctions);
    }
    
    // === 고급 검색 및 필터링 API ===
    
    @Operation(summary = "통합 경매 검색", 
               description = """
                   키워드, 카테고리, 지역, 가격 범위 등 복합 조건으로 경매를 검색합니다.
                   
                   **검색 조건:**
                   - keyword: 제목, 설명에서 검색 (대소문자 구분 없음)
                   - category: 카테고리 필터 (ELECTRONICS, CLOTHING, BOOKS, etc.)
                   - regionScope: 지역 범위 (NATIONWIDE, CITY, NEIGHBORHOOD)
                   - regionCode: 지역 코드 (regionScope가 CITY일 때 필요)
                   - minPrice, maxPrice: 가격 범위
                   - status: 경매 상태 (기본값: ACTIVE)
                   - sortBy: 정렬 옵션 (CREATED_DESC, PRICE_ASC, ENDING_SOON, etc.)
                   - endingSoonHours: N시간 이내 마감 경매만
                   - minBidCount: 최소 입찰 수
                   
                   **정렬 옵션:**
                   - CREATED_DESC: 최신순 (기본값)
                   - CREATED_ASC: 오래된순
                   - PRICE_ASC: 낮은 가격순
                   - PRICE_DESC: 높은 가격순
                   - ENDING_SOON: 마감 임박순
                   - VIEW_COUNT_DESC: 조회수 높은순
                   - BID_COUNT_DESC: 입찰수 높은순
                   
                   **사용 예시:**
                   - 키워드 검색: keyword=아이폰
                   - 가격 범위: minPrice=100000&maxPrice=500000
                   - 마감 임박: endingSoonHours=24&sortBy=ENDING_SOON
                   - 인기 경매: minBidCount=5&sortBy=BID_COUNT_DESC
                   """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 검색 조건")
    })
    @GetMapping("/search")
    public ResponseEntity<Page<AuctionResponse>> searchAuctions(
            @Parameter(description = "키워드 (제목, 설명 검색)") @RequestParam(required = false) String keyword,
            @Parameter(description = "카테고리") @RequestParam(required = false) Category category,
            @Parameter(description = "지역 범위") @RequestParam(required = false) RegionScope regionScope,
            @Parameter(description = "지역 코드") @RequestParam(required = false) String regionCode,
            @Parameter(description = "최소 가격") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "최대 가격") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "경매 상태") @RequestParam(required = false, defaultValue = "ACTIVE") AuctionStatus status,
            @Parameter(description = "정렬 옵션") @RequestParam(required = false, defaultValue = "CREATED_DESC") AuctionSearchRequest.SortOption sortBy,
            @Parameter(description = "N시간 이내 마감") @RequestParam(required = false) Integer endingSoonHours,
            @Parameter(description = "최소 입찰 수") @RequestParam(required = false) Integer minBidCount,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        // 요청 객체 생성
        AuctionSearchRequest searchRequest = new AuctionSearchRequest();
        searchRequest.setKeyword(keyword);
        searchRequest.setCategory(category);
        searchRequest.setRegionScope(regionScope);
        searchRequest.setRegionCode(regionCode);
        searchRequest.setMinPrice(minPrice);
        searchRequest.setMaxPrice(maxPrice);
        searchRequest.setStatus(status);
        searchRequest.setSortBy(sortBy);
        searchRequest.setEndingSoonHours(endingSoonHours);
        searchRequest.setMinBidCount(minBidCount);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuctionResponse> auctions = auctionService.searchAuctions(searchRequest, pageable);
        return ResponseEntity.ok(auctions);
    }
    
    @Operation(summary = "키워드 검색", description = "제목과 설명에서 키워드로 경매를 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @GetMapping("/search/keyword")
    public ResponseEntity<Page<AuctionResponse>> searchByKeyword(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @Parameter(description = "경매 상태") @RequestParam(required = false, defaultValue = "ACTIVE") AuctionStatus status,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuctionResponse> auctions = auctionService.searchByKeyword(keyword, status, pageable);
        return ResponseEntity.ok(auctions);
    }
    
    @Operation(summary = "가격 범위 검색", description = "지정된 가격 범위 내의 경매를 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 가격 범위")
    })
    @GetMapping("/search/price")
    public ResponseEntity<Page<AuctionResponse>> searchByPriceRange(
            @Parameter(description = "최소 가격") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "최대 가격") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "경매 상태") @RequestParam(required = false, defaultValue = "ACTIVE") AuctionStatus status,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuctionResponse> auctions = auctionService.searchByPriceRange(minPrice, maxPrice, status, pageable);
        return ResponseEntity.ok(auctions);
    }
    
    @Operation(summary = "마감 임박 경매", 
               description = "지정된 시간 내에 마감되는 경매를 조회합니다. 마감 시간 순으로 정렬됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 시간 값")
    })
    @GetMapping("/ending-soon")
    public ResponseEntity<Page<AuctionResponse>> getEndingSoonAuctions(
            @Parameter(description = "시간 (시간 단위, 기본값: 24시간)") @RequestParam(defaultValue = "24") int hours,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        if (hours <= 0 || hours > 168) { // 최대 1주일
            throw new IllegalArgumentException("시간은 1시간부터 168시간(1주일) 사이여야 합니다.");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuctionResponse> auctions = auctionService.getEndingSoonAuctions(hours, pageable);
        return ResponseEntity.ok(auctions);
    }
    
    @Operation(summary = "인기 경매", 
               description = "입찰 수가 많은 인기 경매를 조회합니다. 입찰 수 순으로 정렬됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 입찰 수")
    })
    @GetMapping("/popular")
    public ResponseEntity<Page<AuctionResponse>> getPopularAuctions(
            @Parameter(description = "최소 입찰 수 (기본값: 3)") @RequestParam(defaultValue = "3") int minBidCount,
            @Parameter(description = "경매 상태") @RequestParam(required = false, defaultValue = "ACTIVE") AuctionStatus status,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        if (minBidCount < 0) {
            throw new IllegalArgumentException("최소 입찰 수는 0 이상이어야 합니다.");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuctionResponse> auctions = auctionService.getPopularAuctions(minBidCount, status, pageable);
        return ResponseEntity.ok(auctions);
    }
    
    @Operation(summary = "상태별 경매 조회", description = "특정 상태의 경매를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 상태")
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<AuctionResponse>> getAuctionsByStatus(
            @Parameter(description = "경매 상태") @PathVariable AuctionStatus status,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuctionResponse> auctions = auctionService.getAuctionsByStatus(status, pageable);
        return ResponseEntity.ok(auctions);
    }
    
    @Operation(summary = "경매 강제 종료", description = "테스트용: 경매를 강제로 종료시킵니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "강제 종료 성공"),
            @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음")
    })
    @PatchMapping("/test/force-end/{id}")
    public ResponseEntity<AuctionResponse> forceEndAuction(
            @Parameter(description = "경매 ID") @PathVariable Long id) {
        
        AuctionResponse auction = auctionService.forceEndAuction(id);
        return ResponseEntity.ok(auction);
    }
}