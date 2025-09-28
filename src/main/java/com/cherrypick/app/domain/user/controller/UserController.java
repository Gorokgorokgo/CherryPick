package com.cherrypick.app.domain.user.controller;

import com.cherrypick.app.common.exception.AuthenticationFailedException;
import com.cherrypick.app.config.JwtConfig;
import com.cherrypick.app.domain.auction.dto.AuctionResponse;
import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.entity.AuctionImage;
import com.cherrypick.app.domain.auction.repository.AuctionImageRepository;
import com.cherrypick.app.domain.auction.service.AuctionBookmarkService;
import com.cherrypick.app.domain.auction.service.AuctionService;
import com.cherrypick.app.domain.bid.dto.response.BidResponse;
import com.cherrypick.app.domain.bid.service.BidService;
import com.cherrypick.app.domain.bid.repository.BidRepository;
import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.user.dto.request.UpdateProfileRequest;
import com.cherrypick.app.domain.user.dto.request.UpdateProfileImageRequest;
import com.cherrypick.app.domain.user.dto.response.UserProfileResponse;
import com.cherrypick.app.domain.user.dto.response.UserLevelInfoResponse;
import com.cherrypick.app.domain.user.dto.response.LevelProgressResponse;
import com.cherrypick.app.domain.user.service.UserService;
import com.cherrypick.app.domain.user.service.ExperienceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@Tag(name = "2단계 - 사용자 프로필", description = "사용자 정보 관리 | 닉네임, 개인정보 설정")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;
    private final ExperienceService experienceService;
    private final JwtConfig jwtConfig;

    // Bookmarks 관련 의존성
    private final AuctionBookmarkService bookmarkService;
    private final AuctionImageRepository auctionImageRepository;
    private final AuctionService auctionService;

    // Bids 관련 의존성
    private final BidService bidService;
    private final BidRepository bidRepository;

    public UserController(UserService userService,
                          ExperienceService experienceService,
                          JwtConfig jwtConfig,
                          AuctionBookmarkService bookmarkService,
                          AuctionImageRepository auctionImageRepository,
                          AuctionService auctionService,
                          BidService bidService,
                          BidRepository bidRepository) {
        this.userService = userService;
        this.experienceService = experienceService;
        this.jwtConfig = jwtConfig;
        this.bookmarkService = bookmarkService;
        this.auctionImageRepository = auctionImageRepository;
        this.auctionService = auctionService;
        this.bidService = bidService;
        this.bidRepository = bidRepository;
    }

    @GetMapping("/profile")
    @Operation(summary = "프로필 조회", description = "현재 로그인된 사용자의 프로필 정보를 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<UserProfileResponse> getProfile(HttpServletRequest request) {
        Long userId = extractUserIdFromRequest(request);
        UserProfileResponse profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    @Operation(summary = "프로필 수정", 
               description = "사용자의 프로필 정보를 수정합니다. 닉네임, 프로필 이미지, 실명, 생년월일, 성별, 주소, 자기소개, 공개 설정 등을 수정할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로필 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 중복 닉네임"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest updateRequest,
            HttpServletRequest request) {
        Long userId = extractUserIdFromRequest(request);
        UserProfileResponse updatedProfile = userService.updateProfile(userId, updateRequest);
        return ResponseEntity.ok(updatedProfile);
    }

    @PutMapping("/profile/image")
    @Operation(summary = "프로필 이미지 수정", 
               description = "사용자의 프로필 이미지를 수정합니다. 기존 이미지는 자동으로 삭제됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로필 이미지 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 이미지 URL"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<UserProfileResponse> updateProfileImage(
            @Valid @RequestBody UpdateProfileImageRequest imageRequest,
            HttpServletRequest request) {
        Long userId = extractUserIdFromRequest(request);
        UserProfileResponse updatedProfile = userService.updateProfileImage(userId, imageRequest);
        return ResponseEntity.ok(updatedProfile);
    }

    @DeleteMapping("/profile/image")
    @Operation(summary = "프로필 이미지 삭제", 
               description = "사용자의 프로필 이미지를 삭제합니다. S3에서도 완전히 제거됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로필 이미지 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<UserProfileResponse> deleteProfileImage(HttpServletRequest request) {
        Long userId = extractUserIdFromRequest(request);
        UserProfileResponse updatedProfile = userService.deleteProfileImage(userId);
        return ResponseEntity.ok(updatedProfile);
    }

    @GetMapping("/bookmarks")
    @Operation(summary = "내 북마크 경매 목록", description = "현재 로그인된 사용자의 북마크한 경매 목록을 최신순으로 반환합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "북마크 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<List<AuctionResponse>> getMyBookmarks(HttpServletRequest request) {
        Long userId = extractUserIdFromRequest(request);

        // 1) 사용자의 북마크된 경매 목록 조회
        List<Auction> auctions = bookmarkService.getUserBookmarks(userId);
        if (auctions.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        // 2) 이미지 벌크 조회 후 경매별로 매핑
        List<Long> auctionIds = auctions.stream().map(Auction::getId).toList();
        List<AuctionImage> allImages = auctionImageRepository.findByAuctionIdInOrderByAuctionIdAndSortOrder(auctionIds);
        Map<Long, List<AuctionImage>> imageMap = allImages.stream()
                .collect(Collectors.groupingBy(img -> img.getAuction().getId()));

        // 3) 응답 변환 + 북마크 정보 채움
        List<AuctionResponse> responses = auctions.stream()
                .map(a -> {
                    var images = imageMap.getOrDefault(a.getId(), List.of());
                    var resp = AuctionResponse.from(a, images);
                    // 사용자의 북마크 목록이므로 true 고정
                    resp.setBookmarked(true);
                    // 전체 찜 수
                    Long count = bookmarkService.getBookmarkCount(a.getId());
                    resp.setBookmarkCount(count);
                    return resp;
                })
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/auctions")
    @Operation(summary = "내가 등록한 경매 목록", description = "현재 로그인된 사용자가 등록한 경매 목록을 반환합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<List<AuctionResponse>> getMyAuctions(HttpServletRequest request,
                                                               @RequestParam(required = false, defaultValue = "50") int limit) {
        Long userId = extractUserIdFromRequest(request);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, Math.max(1, Math.min(limit, 100)));
        var page = auctionService.getMyAuctions(userId, pageable);
        return ResponseEntity.ok(page.getContent());
    }

    @GetMapping("/me")
    @Operation(summary = "현재 사용자 정보 조회", description = "현재 로그인된 사용자의 프로필 정보를 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<UserProfileResponse> getMe(HttpServletRequest request) {
        Long userId = extractUserIdFromRequest(request);
        UserProfileResponse profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/bids")
    @Operation(summary = "내 입찰 목록 조회", description = "현재 로그인된 사용자의 입찰 내역을 페이지 형태로 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<Page<BidResponse>> getMyBids(HttpServletRequest request,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        Long userId = extractUserIdFromRequest(request);
        Pageable pageable = PageRequest.of(page, size);
        Page<BidResponse> bids = bidService.getMyBids(userId, pageable);
        return ResponseEntity.ok(bids);
    }

    // 자동입찰 관리용 응답 DTO (프론트 요구 필드 포함)
    public record AutoBidItemResponse(
            Long id,
            Long auctionId,
            String title,
            String imageUrl,
            java.math.BigDecimal maxAutoBidAmount,
            java.math.BigDecimal currentPrice,
            Integer bidCount,
            String auctionStatus,
            String category,
            String endTime
    ) {}

    @GetMapping("/auto-bids")
    @Operation(summary = "내 자동입찰 목록", description = "현재 로그인 사용자의 활성 자동입찰 목록을 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<List<AutoBidItemResponse>> getMyAutoBids(HttpServletRequest request) {
        Long userId = extractUserIdFromRequest(request);

        // 1) 활성 자동입찰 설정 조회 (bidAmount=0, ACTIVE)
        List<Bid> configs = bidRepository.findActiveAutoBidsByBidderId(userId);
        if (configs.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        // 2) 관련 경매 ID 수집 및 이미지 벌크 조회
        List<Long> auctionIds = configs.stream().map(b -> b.getAuction().getId()).toList();
        List<AuctionImage> allImages = auctionImageRepository.findByAuctionIdInOrderByAuctionIdAndSortOrder(auctionIds);
        Map<Long, List<AuctionImage>> imageMap = allImages.stream()
                .collect(Collectors.groupingBy(img -> img.getAuction().getId()));

        // 3) 응답 조립 (썸네일은 첫 번째 이미지)
        List<AutoBidItemResponse> items = configs.stream()
                .map(b -> {
                    var auction = b.getAuction();
                    var images = imageMap.getOrDefault(auction.getId(), List.of());
                    String imageUrl = images.isEmpty() ? null : images.get(0).getImageUrl();
                    return new AutoBidItemResponse(
                            b.getId(),
                            auction.getId(),
                            auction.getTitle(),
                            imageUrl,
                            b.getMaxAutoBidAmount(),
                            auction.getCurrentPrice(),
                            auction.getBidCount(),
                            auction.getStatus().name(),
                            auction.getCategory().name(),
                            auction.getEndAt() != null ? auction.getEndAt().toString() : null
                    );
                })
                .toList();

        return ResponseEntity.ok(items);
    }

    private Long extractUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtConfig.extractUserId(token);
        }
        throw new AuthenticationFailedException();
    }
    
    @GetMapping("/level")
    @Operation(summary = "레벨 정보 조회", description = "구매자/판매자 레벨 진행률을 조회합니다 (레벨대별 차등 표시)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "레벨 정보 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<UserLevelInfoResponse> getUserLevelInfo(HttpServletRequest request) {
        Long userId = extractUserIdFromRequest(request);
        UserLevelInfoResponse levelInfo = experienceService.getUserLevelInfo(userId);
        return ResponseEntity.ok(levelInfo);
    }
    
    @GetMapping("/level/buyer")
    @Operation(summary = "구매자 레벨 진행률 조회", description = "구매자 레벨 진행률을 조회합니다 (심리적 배려)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "구매자 레벨 정보 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<LevelProgressResponse> getBuyerLevelProgress(HttpServletRequest request) {
        Long userId = extractUserIdFromRequest(request);
        LevelProgressResponse progress = experienceService.getBuyerLevelProgress(userId);
        return ResponseEntity.ok(progress);
    }
    
    @GetMapping("/level/seller")
    @Operation(summary = "판매자 레벨 진행률 조회", description = "판매자 레벨 진행률을 조회합니다 (심리적 배려)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "판매자 레벨 정보 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<LevelProgressResponse> getSellerLevelProgress(HttpServletRequest request) {
        Long userId = extractUserIdFromRequest(request);
        LevelProgressResponse progress = experienceService.getSellerLevelProgress(userId);
        return ResponseEntity.ok(progress);
    }
}