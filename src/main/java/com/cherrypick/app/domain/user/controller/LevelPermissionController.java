package com.cherrypick.app.domain.user.controller;

import com.cherrypick.app.domain.user.dto.response.LevelPermissionResponse;
import com.cherrypick.app.domain.user.service.LevelPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "10단계 - 레벨 권한 시스템", description = """
    레벨 기반 권한 관리 API - 보증금 시스템 대체
    
    **개정된 신뢰도 관리:**
    - 보증금 시스템 완전 제거 (법적 리스크 해결)
    - 경험치 기반 100레벨 시스템 활용
    - 레벨별 차등 권한 및 혜택 제공
    - 입찰 제한을 통한 저레벨 사용자 보호
    
    **레벨 구간별 혜택:**
    - 🟢 초보 (1-20레벨): 기본 권한, 5만원 입찰 제한
    - 🟡 성장 (21-40레벨): 20만원 입찰, 10% 수수료 할인
    - 🟠 숙련 (41-60레벨): 50만원 입찰, 20% 수수료 할인
    - 🔴 고수 (61-80레벨): 무제한 입찰, 30% 수수료 할인
    - 🟣 마스터+ (81-100레벨): 모든 혜택, 40% 수수료 할인
    
    **입찰 제한 정책:**
    - 저레벨 사용자 보호 및 신뢰도 기반 거래 질서 유지
    - 레벨업을 통한 자연스러운 권한 확장
    - 고레벨 사용자 우대 혜택
    """)
@RestController
@RequestMapping("/api/level")
@RequiredArgsConstructor
public class LevelPermissionController {
    
    private final LevelPermissionService levelPermissionService;
    
    @Operation(
        summary = "입찰 권한 확인",
        description = """
            특정 입찰 금액에 대한 사용자의 입찰 권한을 확인합니다.
            
            **입찰 제한 정책:**
            - 초보 (1-20레벨): 최대 50,000원
            - 성장 (21-40레벨): 최대 200,000원
            - 숙련 (41-60레벨): 최대 500,000원
            - 고수+ (61-100레벨): 무제한
            
            **권한 확인 결과:**
            - 입찰 가능 여부
            - 현재 레벨별 최대 입찰 금액
            - 레벨별 권한 및 혜택 목록
            - 수수료 할인율 정보
            
            **활용 예시:**
            - 입찰 전 권한 사전 확인
            - 입찰 제한 안내 메시지 표시
            - 레벨업 유도 가이드 제공
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "입찰 권한 확인 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LevelPermissionResponse.class),
                examples = @ExampleObject(
                    name = "입찰 권한 확인 응답",
                    value = """
                    {
                      "userId": 1,
                      "buyerLevel": 25,
                      "sellerLevel": 30,
                      "canBid": true,
                      "maxBidAmount": 200000,
                      "levelTier": "🟡 성장 (Lv 21-40)",
                      "permissions": [
                        "일반 입찰 참여 (최대 20만원)",
                        "경매 등록",
                        "채팅 기능",
                        "연결 수수료 10% 할인"
                      ],
                      "benefits": [
                        "거래 우선권",
                        "월간 통계 리포트"
                      ],
                      "connectionFeeDiscount": 0.1
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 입찰 금액"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/bidding-permission")
    public ResponseEntity<LevelPermissionResponse> checkBiddingPermission(
            @Parameter(description = "사용자 ID", example = "1") @RequestHeader("User-Id") Long userId,
            @Parameter(description = "입찰 금액", example = "150000") @RequestParam BigDecimal bidAmount) {
        
        LevelPermissionResponse response = levelPermissionService.checkBiddingPermission(userId, bidAmount);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "사용자 레벨 권한 정보 조회",
        description = """
            사용자의 전체 레벨 권한 정보를 조회합니다.
            
            **포함 정보:**
            - 구매 레벨 및 판매 레벨
            - 현재 레벨 등급 (초보/성장/숙련/고수/마스터)
            - 레벨별 권한 목록
            - 레벨별 혜택 목록
            - 최대 입찰 가능 금액
            - 연결 수수료 할인율
            
            **활용 목적:**
            - 마이페이지 레벨 정보 표시
            - 권한 안내 및 레벨업 유도
            - 혜택 안내 및 차별화 서비스 제공
            
            **레벨 혜택 예시:**
            - 입찰 제한 완화
            - 수수료 할인 (최대 40%)
            - 우선 고객 지원
            - 특별 이벤트 초대
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "레벨 권한 정보 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LevelPermissionResponse.class),
                examples = @ExampleObject(
                    name = "레벨 권한 정보 응답",
                    value = """
                    {
                      "userId": 1,
                      "buyerLevel": 45,
                      "sellerLevel": 52,
                      "canBid": true,
                      "maxBidAmount": 500000,
                      "levelTier": "🟠 숙련 (Lv 41-60)",
                      "permissions": [
                        "고액 입찰 참여 (최대 50만원)",
                        "프리미엄 경매 등록",
                        "우선 채팅 연결",
                        "연결 수수료 20% 할인"
                      ],
                      "benefits": [
                        "프리미엄 뱃지 표시",
                        "거래 신뢰도 우대",
                        "특별 이벤트 초대"
                      ],
                      "connectionFeeDiscount": 0.2
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/my-permissions")
    public ResponseEntity<LevelPermissionResponse> getMyPermissions(
            @Parameter(description = "사용자 ID", example = "1") @RequestHeader("User-Id") Long userId) {
        
        LevelPermissionResponse response = levelPermissionService.getUserPermissionInfo(userId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "연결 수수료 할인율 조회",
        description = """
            판매자의 레벨에 따른 연결 수수료 할인율을 조회합니다.
            
            **할인율 정책:**
            - 초보 (0-20레벨): 0% (할인 없음)
            - 성장 (21-40레벨): 10% 할인
            - 숙련 (41-60레벨): 20% 할인
            - 고수 (61-80레벨): 30% 할인
            - 마스터+ (81-100레벨): 40% 할인
            
            **할인 적용:**
            - 판매자의 판매 레벨 기준
            - 연결 수수료 결제 시 자동 할인
            - 할인된 금액으로 최종 결제
            
            **예시:**
            - 기본 수수료: 3,000원
            - 레벨 50 (숙련자): 20% 할인 → 2,400원
            - 레벨 85 (마스터): 40% 할인 → 1,800원
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "할인율 조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "할인율 응답",
                    value = """
                    {
                      "userId": 1,
                      "sellerLevel": 65,
                      "discountRate": 0.3,
                      "discountPercentage": 30,
                      "levelTier": "🔴 고수 (Lv 61-80)",
                      "message": "판매 레벨(65) 혜택으로 연결 수수료 30% 할인 적용"
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/connection-fee-discount")
    public ResponseEntity<?> getConnectionFeeDiscount(
            @Parameter(description = "판매자 ID", example = "1") @RequestHeader("User-Id") Long sellerId) {
        
        double discount = levelPermissionService.getConnectionFeeDiscount(sellerId);
        
        // 간단한 응답 객체 생성
        var response = new Object() {
            public final Long userId = sellerId;
            public final double discountRate = discount;
            public final int discountPercentage = (int)(discount * 100);
            public final String message = discount > 0 
                ? String.format("레벨별 혜택으로 연결 수수료 %d%% 할인 적용", discountPercentage)
                : "레벨업 시 연결 수수료 할인 혜택을 받을 수 있습니다.";
        };
        
        return ResponseEntity.ok(response);
    }
}