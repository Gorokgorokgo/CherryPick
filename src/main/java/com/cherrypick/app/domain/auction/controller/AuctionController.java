package com.cherrypick.app.domain.auction.controller;

import com.cherrypick.app.domain.auction.dto.AuctionResponse;
import com.cherrypick.app.domain.auction.dto.AuctionSearchRequest;
import com.cherrypick.app.domain.auction.dto.CreateAuctionRequest;
import com.cherrypick.app.domain.auction.dto.TopBidderResponse;
import com.cherrypick.app.domain.auction.dto.UpdateAuctionRequest;
import com.cherrypick.app.domain.auction.service.AuctionService;
import com.cherrypick.app.domain.auction.service.AuctionBookmarkService;
import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.user.service.UserService;
import com.cherrypick.app.domain.chat.dto.response.ChatRoomResponse;
import com.cherrypick.app.domain.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "6단계 - 경매 관리", description = "경매 등록, 조회, 검색 | 보증금 10% 자동 차감")
@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;
    private final UserService userService;
    private final AuctionBookmarkService bookmarkService;
    private final ChatService chatService;
    
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
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        Long userId = userDetails != null ? userService.getUserIdByEmail(userDetails.getUsername()) : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<AuctionResponse> auctions = auctionService.getActiveAuctions(pageable, userId);
        return ResponseEntity.ok(auctions);
    }
    
    @Operation(summary = "카테고리별 경매 조회", description = "특정 카테고리의 진행중인 경매를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "카테고리별 경매 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 카테고리 또는 페이지 정보")
    })
    @GetMapping("/category/{category}")
    public ResponseEntity<Page<AuctionResponse>> getAuctionsByCategory(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "경매 카테고리") @PathVariable Category category,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        Long userId = userDetails != null ? userService.getUserIdByEmail(userDetails.getUsername()) : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<AuctionResponse> auctions = auctionService.getAuctionsByCategory(category, pageable, userId);
        return ResponseEntity.ok(auctions);
    }
    
    @Operation(summary = "지역별 경매 조회", description = "특정 지역의 진행중인 경매를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "지역별 경매 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 지역 정보 또는 페이지 정보")
    })
    @GetMapping("/region")
    public ResponseEntity<Page<AuctionResponse>> getAuctionsByRegion(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "지역 범위 (NATIONAL, REGIONAL)") @RequestParam RegionScope regionScope,
            @Parameter(description = "지역 코드 (지역별 검색시 필요)") @RequestParam(required = false) String regionCode,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        Long userId = userDetails != null ? userService.getUserIdByEmail(userDetails.getUsername()) : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<AuctionResponse> auctions = auctionService.getAuctionsByRegion(regionScope, regionCode, pageable, userId);
        return ResponseEntity.ok(auctions);
    }
    
    @Operation(summary = "경매 상세 조회", description = "특정 경매의 상세 정보를 조회합니다. 조회수는 별도 API(/view)로 증가시킵니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "경매 상세 조회 성공"),
            @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음")
    })
    @GetMapping("/{auctionId}")
    public ResponseEntity<AuctionResponse> getAuctionDetail(
            @Parameter(description = "경매 ID") @PathVariable Long auctionId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        AuctionResponse response = auctionService.getAuctionDetail(auctionId);
        // 북마크 정보 채우기
        try {
            long count = bookmarkService.getBookmarkCount(auctionId);
            response.setBookmarkCount(count);
            if (userDetails != null) {
                Long userId = userService.getUserIdByEmail(userDetails.getUsername());
                boolean isBookmarked = bookmarkService.isBookmarked(auctionId, userId);
                response.setBookmarked(isBookmarked);
            } else {
                response.setBookmarked(false);
            }
        } catch (Exception e) {
            // 예외가 발생해도 상세 응답 자체는 반환 (기본값 유지)
        }
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "경매 상세 조회 (조회수 증가 없음)", description = "특정 경매의 상세 정보를 조회합니다. 조회수는 증가하지 않습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "경매 상세 조회 성공"),
            @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음")
    })
    @GetMapping("/{auctionId}/info")
    public ResponseEntity<AuctionResponse> getAuctionDetailWithoutViewIncrement(
            @Parameter(description = "경매 ID") @PathVariable Long auctionId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        AuctionResponse response = auctionService.getAuctionDetailWithoutViewIncrement(auctionId);
        // 북마크 정보 채우기
        try {
            long count = bookmarkService.getBookmarkCount(auctionId);
            response.setBookmarkCount(count);
            if (userDetails != null) {
                Long userId = userService.getUserIdByEmail(userDetails.getUsername());
                boolean isBookmarked = bookmarkService.isBookmarked(auctionId, userId);
                response.setBookmarked(isBookmarked);
            } else {
                response.setBookmarked(false);
            }
        } catch (Exception e) {
            // 예외 무시하고 기본값 유지
        }
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "경매 조회수 증가", description = "경매의 조회수를 1 증가시킵니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회수 증가 성공"),
            @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음")
    })
    @PostMapping("/{auctionId}/view")
    public ResponseEntity<Map<String, String>> increaseAuctionViewCount(
            @Parameter(description = "경매 ID") @PathVariable Long auctionId) {
        auctionService.increaseAuctionViewCount(auctionId);
        Map<String, String> response = Map.of("message", "조회수가 증가되었습니다");
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
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
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

        Long userId = userDetails != null ? userService.getUserIdByEmail(userDetails.getUsername()) : null;

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
        Page<AuctionResponse> auctions = auctionService.searchAuctions(searchRequest, pageable, userId);
        return ResponseEntity.ok(auctions);
    }
    
    @Operation(summary = "키워드 검색", description = "제목과 설명에서 키워드로 경매를 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @GetMapping("/search/keyword")
    public ResponseEntity<Page<AuctionResponse>> searchByKeyword(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @Parameter(description = "경매 상태") @RequestParam(required = false, defaultValue = "ACTIVE") AuctionStatus status,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        Long userId = userDetails != null ? userService.getUserIdByEmail(userDetails.getUsername()) : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<AuctionResponse> auctions = auctionService.searchByKeyword(keyword, status, pageable, userId);
        return ResponseEntity.ok(auctions);
    }
    
    @Operation(summary = "가격 범위 검색", description = "지정된 가격 범위 내의 경매를 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 가격 범위")
    })
    @GetMapping("/search/price")
    public ResponseEntity<Page<AuctionResponse>> searchByPriceRange(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "최소 가격") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "최대 가격") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "경매 상태") @RequestParam(required = false, defaultValue = "ACTIVE") AuctionStatus status,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        Long userId = userDetails != null ? userService.getUserIdByEmail(userDetails.getUsername()) : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<AuctionResponse> auctions = auctionService.searchByPriceRange(minPrice, maxPrice, status, pageable, userId);
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
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "시간 (시간 단위, 기본값: 24시간)") @RequestParam(defaultValue = "24") int hours,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        if (hours <= 0 || hours > 168) { // 최대 1주일
            throw new IllegalArgumentException("시간은 1시간부터 168시간(1주일) 사이여야 합니다.");
        }

        Long userId = userDetails != null ? userService.getUserIdByEmail(userDetails.getUsername()) : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<AuctionResponse> auctions = auctionService.getEndingSoonAuctions(hours, pageable, userId);
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
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "최소 입찰 수 (기본값: 3)") @RequestParam(defaultValue = "3") int minBidCount,
            @Parameter(description = "경매 상태") @RequestParam(required = false, defaultValue = "ACTIVE") AuctionStatus status,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        if (minBidCount < 0) {
            throw new IllegalArgumentException("최소 입찰 수는 0 이상이어야 합니다.");
        }

        Long userId = userDetails != null ? userService.getUserIdByEmail(userDetails.getUsername()) : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<AuctionResponse> auctions = auctionService.getPopularAuctions(minBidCount, status, pageable, userId);
        return ResponseEntity.ok(auctions);
    }
    
    @Operation(summary = "상태별 경매 조회", description = "특정 상태의 경매를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 상태")
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<AuctionResponse>> getAuctionsByStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "경매 상태") @PathVariable AuctionStatus status,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        Long userId = userDetails != null ? userService.getUserIdByEmail(userDetails.getUsername()) : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<AuctionResponse> auctions = auctionService.getAuctionsByStatus(status, pageable, userId);
        return ResponseEntity.ok(auctions);
    }
    
    @Operation(summary = "개발자 옵션: 모든 경매 조회", description = "개발/테스트용: 상태와 관계없이 모든 경매를 조회합니다.")
    @GetMapping("/test/all")
    public ResponseEntity<Page<AuctionResponse>> getAllAuctionsForDev(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<AuctionResponse> auctions = auctionService.getAllAuctionsForDev(pageable);
        return ResponseEntity.ok(auctions);
    }

    @Operation(summary = "경매 시간 조정", description = "개발/테스트용: 경매 종료 시간을 조정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "시간 조정 성공"),
            @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음")
    })
    @PatchMapping("/test/adjust-time/{id}")
    public ResponseEntity<AuctionResponse> adjustAuctionTime(
            @Parameter(description = "경매 ID") @PathVariable Long id,
            @Parameter(description = "조정할 분 (양수: 시간 추가, 음수: 시간 감소)") @RequestParam int minutes) {

        AuctionResponse auction = auctionService.adjustAuctionTime(id, minutes);
        return ResponseEntity.ok(auction);
    }

    @Operation(summary = "경매 재활성화", description = "테스트용/재등록: 종료된 경매를 재활성화합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "재활성화 성공"),
            @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음")
    })
    @PatchMapping("/test/reactivate/{id}")
    public ResponseEntity<AuctionResponse> reactivateAuction(
            @Parameter(description = "경매 ID") @PathVariable Long id,
            @Parameter(description = "재활성화 후 진행할 시간 (시간 단위)") @RequestParam(defaultValue = "3") int hours) {

        AuctionResponse auction = auctionService.reactivateAuction(id, hours);
        return ResponseEntity.ok(auction);
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

    // ================== 유찰 경매 관련 API ==================

    @Operation(summary = "유찰 경매 최고입찰자 조회",
               description = "유찰된 경매의 최고입찰자 정보를 조회합니다. 입찰이 없으면 204 No Content를 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "최고입찰자 조회 성공"),
            @ApiResponse(responseCode = "204", description = "입찰 내역 없음"),
            @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음")
    })
    @GetMapping("/{id}/top-bidder")
    public ResponseEntity<TopBidderResponse> getTopBidder(
            @Parameter(description = "경매 ID") @PathVariable Long id) {

        return auctionService.getTopBidder(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @Operation(summary = "유찰 경매 채팅방 생성",
               description = "유찰된 경매의 최고입찰자와 판매자 간 채팅방을 생성합니다. 판매자만 호출 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "채팅방 생성 성공"),
            @ApiResponse(responseCode = "400", description = "입찰 내역이 없거나 이미 채팅방이 존재함"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (판매자 아님)"),
            @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음")
    })
    @PostMapping("/{id}/create-failed-auction-chat")
    public ResponseEntity<ChatRoomResponse> createFailedAuctionChat(
            @Parameter(description = "경매 ID") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        ChatRoomResponse response = chatService.createFailedAuctionChatRoom(id, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "경매 재활성화 (재등록)",
               description = "종료된 경매를 재활성화합니다. 판매자만 호출 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "재활성화 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (판매자 아님)"),
            @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음")
    })
    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<AuctionResponse> reactivateAuctionProd(
            @Parameter(description = "경매 ID") @PathVariable Long id,
            @Parameter(description = "재활성화 후 진행할 시간 (시간 단위)") @RequestParam(defaultValue = "24") int hours,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        // 권한 확인은 서비스 레이어에서 처리하지 않으므로 여기서 확인
        // (필요 시 AuctionService.reactivateAuction에 userId 파라미터 추가)
        AuctionResponse auction = auctionService.reactivateAuction(id, hours);
        return ResponseEntity.ok(auction);
    }

    // ================== 북마크 관련 API ==================

    @Operation(summary = "북마크 토글",
               description = "경매를 북마크에 추가하거나 제거합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "북마크 토글 성공"),
            @ApiResponse(responseCode = "404", description = "경매 또는 사용자를 찾을 수 없음")
    })
    @PostMapping("/{id}/bookmark")
    public ResponseEntity<Map<String, Object>> toggleBookmark(
            @Parameter(description = "경매 ID") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = userService.getUserByEmail(userDetails.getUsername()).getId();
        Map<String, Object> result = bookmarkService.toggleBookmark(id, userId);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "북마크 수 조회",
               description = "특정 경매의 총 북마크 수를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "북마크 수 조회 성공"),
            @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음")
    })
    @GetMapping("/{id}/bookmark-count")
    public ResponseEntity<Map<String, Object>> getBookmarkCount(
            @Parameter(description = "경매 ID") @PathVariable Long id) {

        long count = bookmarkService.getBookmarkCount(id);
        return ResponseEntity.ok(Map.of("bookmarkCount", count));
    }

    @Operation(summary = "북마크 상태 조회",
               description = "사용자가 특정 경매를 북마크했는지 여부를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "북마크 상태 조회 성공"),
            @ApiResponse(responseCode = "404", description = "경매 또는 사용자를 찾을 수 없음")
    })
    @GetMapping("/{id}/bookmark-status")
    public ResponseEntity<Map<String, Object>> getBookmarkStatus(
            @Parameter(description = "경매 ID") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = userService.getUserByEmail(userDetails.getUsername()).getId();
        boolean isBookmarked = bookmarkService.isBookmarked(id, userId);
        return ResponseEntity.ok(Map.of("isBookmarked", isBookmarked));
    }

    @Operation(summary = "북마크 정보 조회",
               description = "특정 경매의 북마크 수와 사용자의 북마크 상태를 함께 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "북마크 정보 조회 성공"),
            @ApiResponse(responseCode = "404", description = "경매 또는 사용자를 찾을 수 없음")
    })
    @GetMapping("/{id}/bookmark-info")
    public ResponseEntity<Map<String, Object>> getBookmarkInfo(
            @Parameter(description = "경매 ID") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = userService.getUserByEmail(userDetails.getUsername()).getId();
        Map<String, Object> info = bookmarkService.getBookmarkInfo(id, userId);
        return ResponseEntity.ok(info);
    }

    // 요청 DTO: { "auctionIds": [1,2,3] } 형태 지원
    public static class BatchBookmarkRequest {
        public List<Long> auctionIds;
        public List<Long> getAuctionIds() { return auctionIds; }
        public void setAuctionIds(List<Long> auctionIds) { this.auctionIds = auctionIds; }
    }

    @Operation(summary = "배치 북마크 정보 조회",
               description = "여러 경매의 북마크 수와 사용자의 북마크 상태를 한 번에 조회합니다. 최대 20개까지 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "배치 북마크 정보 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (빈 목록 또는 너무 많은 ID)"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/batch/bookmark-info")
    public ResponseEntity<Map<String, Object>> getBatchBookmarkInfo(
            @RequestBody Object body,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<Long> auctionIds = null;
        if (body instanceof List<?>) {
            auctionIds = ((List<?>) body).stream()
                    .filter(o -> o instanceof Number)
                    .map(o -> ((Number) o).longValue())
                    .toList();
        } else if (body instanceof BatchBookmarkRequest req) {
            auctionIds = req.getAuctionIds();
        } else if (body instanceof java.util.Map<?,?> map) {
            Object idsObj = map.get("auctionIds");
            if (idsObj instanceof List<?>) {
                auctionIds = ((List<?>) idsObj).stream()
                        .filter(o -> o instanceof Number)
                        .map(o -> ((Number) o).longValue())
                        .toList();
            }
        }

        if (auctionIds == null || auctionIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "auctionIds가 비어있습니다.")
            );
        }
        if (auctionIds.size() > 50) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "최대 50개까지 조회할 수 있습니다.")
            );
        }

        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        Map<String, Object> batchInfo = bookmarkService.getBatchBookmarkInfo(auctionIds, userId);

        Map<String, Object> response = Map.of(
            "success", true,
            "data", batchInfo,
            "message", "배치 북마크 정보 조회 성공"
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "경매 수정",
               description = """
                   등록한 경매의 제목과 설명을 수정합니다.

                   **수정 가능 조건:**
                   - 본인이 등록한 경매만 수정 가능
                   - 입찰이 없는 경매만 수정 가능
                   - 진행 중인 경매만 수정 가능

                   **수정 가능 항목 (eBay 정책):**
                   - 제목: 오타 수정, 정보 추가
                   - 설명: 추가 정보 제공, 상세 설명 보완

                   **수정 불가 항목:**
                   - 시작가, 희망가, 카테고리, 지역, 이미지 등
                   """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "경매 수정 성공"),
            @ApiResponse(responseCode = "400", description = "입찰이 있거나 종료된 경매"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (본인 경매 아님)"),
            @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음")
    })
    @PutMapping("/{auctionId}")
    public ResponseEntity<AuctionResponse> updateAuction(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "경매 ID") @PathVariable Long auctionId,
            @Valid @RequestBody UpdateAuctionRequest request) {

        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        AuctionResponse response = auctionService.updateAuction(auctionId, userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "경매 삭제 (소프트 삭제)",
               description = """
                   등록한 경매를 삭제합니다. (실제 삭제가 아닌 상태 변경)

                   **삭제 가능 조건:**
                   - 본인이 등록한 경매만 삭제 가능
                   - 입찰이 없는 경매만 삭제 가능
                   - 진행 중인 경매만 삭제 가능

                   **주의사항:**
                   - 삭제된 경매는 목록에 노출되지 않습니다
                   - 데이터는 보관되며 관리자만 확인 가능합니다
                   - 입찰이 있는 경매는 삭제할 수 없습니다
                   """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "경매 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "입찰이 있거나 종료된 경매"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (본인 경매 아님)"),
            @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음")
    })
    @DeleteMapping("/{auctionId}")
    public ResponseEntity<Map<String, Object>> deleteAuction(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "경매 ID") @PathVariable Long auctionId) {

        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        auctionService.deleteAuction(auctionId, userId);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "경매가 삭제되었습니다."
        ));
    }
}