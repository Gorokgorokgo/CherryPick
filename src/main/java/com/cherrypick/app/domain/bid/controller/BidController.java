package com.cherrypick.app.domain.bid.controller;

import com.cherrypick.app.domain.bid.dto.request.PlaceBidRequest;
import com.cherrypick.app.domain.bid.dto.response.BidResponse;
import com.cherrypick.app.domain.bid.service.BidService;
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

@Tag(name = "입찰 관리", description = "경매 입찰, 입찰 내역 조회 API")
@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidController {
    
    private final BidService bidService;
    private final UserService userService;
    
    @Operation(summary = "입찰하기", 
               description = "경매에 입찰합니다. 입찰 금액만큼 포인트가 예치되며, 기존 최고가 입찰자의 포인트는 해제됩니다. " +
                           "자동 입찰 기능을 사용하면 설정한 최대 금액까지 자동으로 입찰이 진행됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "입찰 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (포인트 부족, 경매 종료, 자신의 경매 등)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<BidResponse> placeBid(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PlaceBidRequest request) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        BidResponse response = bidService.placeBid(userId, request);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "경매별 입찰 내역 조회", 
               description = "특정 경매의 모든 입찰 내역을 조회합니다. 입찰 금액 높은 순으로 정렬됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "입찰 내역 조회 성공"),
            @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음")
    })
    @GetMapping("/auction/{auctionId}")
    public ResponseEntity<Page<BidResponse>> getBidsByAuction(
            @Parameter(description = "경매 ID") @PathVariable Long auctionId,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<BidResponse> bids = bidService.getBidsByAuction(auctionId, pageable);
        return ResponseEntity.ok(bids);
    }
    
    @Operation(summary = "내 입찰 내역 조회", 
               description = "로그인한 사용자의 모든 입찰 내역을 조회합니다. 최근 입찰한 순으로 정렬됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "내 입찰 내역 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/my")
    public ResponseEntity<Page<BidResponse>> getMyBids(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        Pageable pageable = PageRequest.of(page, size);
        Page<BidResponse> bids = bidService.getMyBids(userId, pageable);
        return ResponseEntity.ok(bids);
    }
    
    @Operation(summary = "최고가 입찰 조회", 
               description = "특정 경매의 현재 최고가 입찰 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "최고가 입찰 조회 성공"),
            @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없거나 입찰이 없음")
    })
    @GetMapping("/auction/{auctionId}/highest")
    public ResponseEntity<BidResponse> getHighestBid(
            @Parameter(description = "경매 ID") @PathVariable Long auctionId) {
        
        BidResponse highestBid = bidService.getHighestBid(auctionId);
        return ResponseEntity.ok(highestBid);
    }
}