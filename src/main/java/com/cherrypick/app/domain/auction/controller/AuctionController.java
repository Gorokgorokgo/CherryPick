package com.cherrypick.app.domain.auction.controller;

import com.cherrypick.app.domain.auction.dto.AuctionResponse;
import com.cherrypick.app.domain.auction.dto.CreateAuctionRequest;
import com.cherrypick.app.domain.auction.service.AuctionService;
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

@Tag(name = "경매 관리", description = "경매 등록, 조회, 검색 API")
@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {
    
    private final AuctionService auctionService;
    private final UserService userService;
    
    @Operation(summary = "경매 등록", description = "새로운 경매를 등록합니다. 보증금(희망가의 10%)이 자동으로 차감되며, 상품 이미지는 최소 1개 이상 필요합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "경매 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (보증금 부족, 유효하지 않은 정보 등)"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
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
}