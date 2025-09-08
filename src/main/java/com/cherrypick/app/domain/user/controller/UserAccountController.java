package com.cherrypick.app.domain.user.controller;

import com.cherrypick.app.domain.user.dto.response.AccountResponse;
import com.cherrypick.app.domain.user.dto.request.AddAccountRequest;
import com.cherrypick.app.domain.user.service.UserAccountService;
import com.cherrypick.app.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "3단계 - 계좌 관리", description = "포인트 충전/출금을 위한 계좌 등록 및 관리 | 본인 명의 계좌만 등록 가능")
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class UserAccountController {
    
    private final UserAccountService userAccountService;
    private final UserService userService;
    
    @Operation(summary = "계좌 등록", description = "사용자 계좌를 등록합니다. 최대 5개까지 등록 가능하며, 첫 번째 계좌는 자동으로 기본 계좌로 설정됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "계좌 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (계좌 개수 초과, 중복 계좌 등)"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    public ResponseEntity<AccountResponse> addAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddAccountRequest request) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        AccountResponse response = userAccountService.addAccount(userId, request);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "내 계좌 목록 조회", description = "등록된 모든 계좌 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "계좌 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getUserAccounts(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        List<AccountResponse> accounts = userAccountService.getUserAccounts(userId);
        return ResponseEntity.ok(accounts);
    }
    
    @Operation(summary = "기본 계좌 조회", description = "현재 설정된 기본 계좌 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "기본 계좌 조회 성공"),
            @ApiResponse(responseCode = "404", description = "등록된 계좌이 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/primary")
    public ResponseEntity<AccountResponse> getPrimaryAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        AccountResponse account = userAccountService.getPrimaryAccount(userId);
        return ResponseEntity.ok(account);
    }
    
    @Operation(summary = "기본 계좌 설정", description = "지정한 계좌을 기본 계좌로 설정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "기본 계좌 설정 성공"),
            @ApiResponse(responseCode = "404", description = "계좌을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PutMapping("/{accountId}/primary")
    public ResponseEntity<Void> setPrimaryAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "계좌 ID") @PathVariable Long accountId) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        userAccountService.setPrimaryAccount(userId, accountId);
        return ResponseEntity.noContent().build();
    }
    
    @Operation(summary = "계좌 삭제", description = "등록된 계좌을 삭제합니다. 기본 계좌은 다른 계좌이 있을 때만 삭제할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "계좌 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "기본 계좌 삭제 불가 또는 사용 중인 계좌"),
            @ApiResponse(responseCode = "404", description = "계좌을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "삭제할 계좌 ID") @PathVariable Long accountId) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        userAccountService.deleteAccount(userId, accountId);
        return ResponseEntity.noContent().build();
    }
}