package com.cherrypick.app.domain.bid.controller;

import com.cherrypick.app.domain.bid.dto.request.AutoBidSetupRequest;
import com.cherrypick.app.domain.bid.dto.request.PlaceBidRequest;
import com.cherrypick.app.domain.bid.dto.response.BidResponse;
import com.cherrypick.app.domain.bid.service.AutoBidService;
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

@Tag(name = "입찰 관리", description = "경매 입찰 내역 및 자동 입찰 관리")
@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;
    private final AutoBidService autoBidService;
    private final UserService userService;

    @Operation(summary = "경매별 입찰 내역 조회",
               description = "특정 경매의 모든 입찰 내역을 금액 높은 순으로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "입찰 내역 조회 성공"),
            @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음")
    })
    @GetMapping("/auction/{auctionId}")
    public ResponseEntity<Page<BidResponse>> getAuctionBids(
            @Parameter(description = "경매 ID") @PathVariable Long auctionId,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BidResponse> bids = bidService.getAuctionBids(auctionId, pageable);
        return ResponseEntity.ok(bids);
    }

    @Operation(summary = "수동 입찰",
               description = "특정 경매에 수동으로 입찰합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "입찰 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (입찰 금액 불충분 등)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<BidResponse> placeBid(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PlaceBidRequest request) {

        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        BidResponse response = bidService.placeBid(
                request.getAuctionId(),
                userId,
                request.getBidAmount()
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "자동 입찰 설정",
               description = "특정 경매에 대한 자동 입찰을 설정합니다. 최대 금액까지 자동으로 입찰합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "자동 입찰 설정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/auto")
    public ResponseEntity<BidResponse> setupAutoBid(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AutoBidSetupRequest request) {

        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        BidResponse response = autoBidService.setupAutoBid(
                request.getAuctionId(),
                userId,
                request.getMaxAutoBidAmount()
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 자동 입찰 설정 조회",
               description = "특정 경매에 대한 내 자동 입찰 설정을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "자동 입찰 설정 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/auto/{auctionId}")
    public ResponseEntity<BidResponse> getMyAutoBid(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "경매 ID") @PathVariable Long auctionId) {

        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        return autoBidService.getMyAutoBidSetting(auctionId, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "자동 입찰 취소",
               description = "특정 경매에 대한 자동 입찰 설정을 취소합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "취소 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/auto/{auctionId}")
    public ResponseEntity<Void> cancelAutoBid(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "경매 ID") @PathVariable Long auctionId) {

        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        autoBidService.cancelAutoBid(auctionId, userId);
        return ResponseEntity.noContent().build();
    }
}