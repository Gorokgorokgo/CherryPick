package com.cherrypick.app.domain.point.controller;

import com.cherrypick.app.domain.point.dto.request.ChargePointRequest;
import com.cherrypick.app.domain.point.dto.request.WithdrawPointRequest;
import com.cherrypick.app.domain.point.dto.response.PointBalanceResponse;
import com.cherrypick.app.domain.point.dto.response.PointTransactionResponse;
import com.cherrypick.app.domain.point.service.PointService;
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

@Tag(name = "4단계 - 포인트 시스템", description = "포인트 충전/출금 및 거래 내역 관리 | 모든 경매 거래의 기반")
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {
    
    private final PointService pointService;
    private final UserService userService;
    
    @Operation(summary = "포인트 충전", 
               description = """
                   본인 명의 계좌를 통해 포인트를 충전합니다.
                   
                   **충전 규칙:**
                   - 본인 명의 계좌만 사용 가능
                   - 충전 수수료: 무료
                   - 최대 보유 포인트: 제한 없음
                   - 충전 방법: 본인 명의 계좌만 가능
                   """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "충전 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (금액, 계좌 오류 등)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "결제 처리 실패")
    })
    @PostMapping("/charge")
    public ResponseEntity<PointTransactionResponse> chargePoints(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChargePointRequest request) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        PointTransactionResponse response = pointService.chargePoints(userId, request);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "포인트 출금", 
               description = """
                   등록된 계좌로 포인트를 출금합니다.
                   
                   **출금 규칙:**
                   - 충전한 본인 명의 계좌로만 출금 가능
                   - 출금 수수료: 무료
                   - 최소 출금 금액: 제한 없음
                   - 예치된 포인트는 출금 불가 (경매 참여중인 포인트)
                   """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "출금 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (잔액 부족, 계좌 오류 등)"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/withdraw")
    public ResponseEntity<PointTransactionResponse> withdrawPoints(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody WithdrawPointRequest request) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        PointTransactionResponse response = pointService.withdrawPoints(userId, request);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "포인트 잔액 조회", description = "현재 사용자의 포인트 잔액을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "잔액 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/balance")
    public ResponseEntity<PointBalanceResponse> getPointBalance(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        PointBalanceResponse response = pointService.getPointBalance(userId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "포인트 거래 내역 조회", description = "사용자의 포인트 충전/출금/사용 내역을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "거래 내역 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/history")
    public ResponseEntity<Page<PointTransactionResponse>> getPointHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        Pageable pageable = PageRequest.of(page, size);
        Page<PointTransactionResponse> response = pointService.getPointTransactionHistory(userId, pageable);
        return ResponseEntity.ok(response);
    }
}