package com.cherrypick.app.domain.connection.controller;

import com.cherrypick.app.domain.connection.dto.response.ConnectionResponse;
import com.cherrypick.app.domain.connection.dto.request.PayConnectionFeeRequest;
import com.cherrypick.app.domain.connection.dto.response.ConnectionFeeResponse;
import com.cherrypick.app.domain.connection.dto.response.PaymentResult;
import com.cherrypick.app.domain.connection.service.ConnectionServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@Tag(name = "9단계 - 연결 서비스", description = """
    판매자-구매자 연결 및 수수료 관리 API
    
    **개정된 비즈니스 모델:**
    - 보증금/예치 시스템 완전 제거 (법적 리스크 해결)
    - 연결 서비스 기반 수수료 모델 (낙찰가의 3%)
    - 수수료 결제 후 채팅 활성화
    - 레벨별 할인 혜택 (최대 40%)
    
    **거래 플로우:**
    1. 경매 낙찰 → 연결 서비스 자동 생성
    2. 판매자 수수료 결제 → 채팅방 활성화
    3. 인앱 채팅으로 거래 조건 협의
    4. 오프라인 직거래 진행
    5. 거래 완료 확인
    """)
@RestController
@RequestMapping("/api/connection")
@RequiredArgsConstructor
public class ConnectionController {
    
    private final ConnectionServiceImpl connectionService;
    
    @Operation(
        summary = "연결 서비스 수수료 정보 조회",
        description = """
            연결 서비스의 수수료 정보를 조회합니다.
            
            **수수료 정책:**
            - 기본 수수료율: 현재 0% (무료 프로모션)
            - 추후 점진적 인상 예정: 1% → 2% → 3%
            
            **판매자 레벨별 할인:**
            - 레벨 1: 0% 할인
            - 레벨 2: 5% 할인  
            - 레벨 3: 10% 할인
            - ...
            - 레벨 10: 50% 할인
            
            **결제 전 확인사항:**
            - 할인 적용된 최종 수수료 확인
            - 판매자 레벨 및 혜택 확인
            - 무료 프로모션 여부 확인
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "수수료 정보 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ConnectionFeeResponse.class),
                examples = @ExampleObject(
                    name = "수수료 정보 응답",
                    value = """
                    {
                      "connectionId": 1,
                      "finalPrice": 70000,
                      "baseFeeRate": 0.0,
                      "baseFee": 0,
                      "discountRate": 10,
                      "discountAmount": 0,
                      "finalFee": 0,
                      "sellerLevel": 3,
                      "isFreePromotion": true
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음 - 판매자만 조회 가능"),
        @ApiResponse(responseCode = "404", description = "연결 서비스를 찾을 수 없음")
    })
    @GetMapping("/{connectionId}/fee")
    public ResponseEntity<ConnectionFeeResponse> getConnectionFeeInfo(
            @Parameter(description = "연결 서비스 ID", example = "1") @PathVariable Long connectionId,
            @Parameter(description = "판매자 ID", example = "1") @RequestHeader("User-Id") Long sellerId) {
        
        ConnectionFeeResponse response = connectionService.getConnectionFeeInfo(connectionId, sellerId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "연결 서비스 수수료 결제 (신규)",
        description = """
            판매자가 연결 수수료를 결제하여 구매자와의 채팅을 활성화합니다.
            
            **현재 정책:**
            - 무료 프로모션 기간 - 결제 없이 즉시 활성화
            - 추후 유료 전환시 포인트 결제 시스템 활용
            
            **결제 검증:**
            - 프론트엔드 계산 수수료와 서버 계산 일치 확인
            - 이중 결제 방지 로직
            - 권한 검증 (판매자만 결제 가능)
            
            **결제 완료 후:**
            - 연결 서비스 PENDING → ACTIVE 상태 변경
            - 채팅방 자동 활성화
            - 양방향 실시간 채팅 가능
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "연결 수수료 결제 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaymentResult.class),
                examples = @ExampleObject(
                    name = "결제 완료 응답",
                    value = """
                    {
                      "connectionId": 1,
                      "success": true,
                      "status": "ACTIVE",
                      "chatRoomActivated": true,
                      "connectedAt": "2024-08-04T10:30:00",
                      "message": "연결 서비스가 활성화되었습니다. (무료 프로모션)"
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 - 수수료 불일치 또는 이미 처리된 연결"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음 - 판매자만 결제 가능"),
        @ApiResponse(responseCode = "404", description = "연결 서비스를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "결제 처리 실패")
    })
    @PostMapping("/{connectionId}/pay")
    public ResponseEntity<PaymentResult> payConnectionFee(
            @Parameter(description = "연결 서비스 ID", example = "1") @PathVariable Long connectionId,
            @Parameter(description = "판매자 ID", example = "1") @RequestHeader("User-Id") Long sellerId,
            @Parameter(description = "결제 요청 정보") @Valid @RequestBody PayConnectionFeeRequest request) {
        
        // 경로 변수와 요청 본문의 connectionId 일치 확인
        if (!connectionId.equals(request.getConnectionId())) {
            return ResponseEntity.badRequest().body(
                PaymentResult.builder()
                    .connectionId(connectionId)
                    .success(false)
                    .message("요청 정보가 일치하지 않습니다.")
                    .errorCode("INVALID_REQUEST")
                    .build()
            );
        }
        
        PaymentResult result = connectionService.payConnectionFee(request, sellerId);
        
        if (result.getSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    @Operation(
        summary = "연결 서비스 결제 처리",
        description = """
            판매자가 연결 수수료를 결제하여 구매자와의 채팅을 활성화합니다.
            
            **수수료 정책:**
            - 기본 수수료: 낙찰가의 3%
            - 최소 수수료: 1,000원
            - 최대 수수료: 10,000원
            
            **레벨별 할인:**
            - 초보자 (0-20레벨): 할인 없음
            - 성장자 (21-40레벨): 10% 할인
            - 숙련자 (41-60레벨): 20% 할인
            - 고수 (61-80레벨): 30% 할인
            - 마스터+ (81-100레벨): 40% 할인
            
            **결제 완료 후:**
            - 채팅방 자동 활성화
            - 양방향 실시간 채팅 가능
            - 거래 조건 협의 및 만남 약속
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "연결 수수료 결제 성공 - 채팅 활성화",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ConnectionResponse.class),
                examples = @ExampleObject(
                    name = "결제 완료 응답",
                    value = """
                    {
                      "id": 1,
                      "auctionId": 123,
                      "auctionTitle": "iPhone 14 Pro 판매",
                      "sellerId": 1,
                      "sellerNickname": "판매자123",
                      "buyerId": 2,
                      "buyerNickname": "구매자456",
                      "connectionFee": 2100,
                      "finalPrice": 70000,
                      "status": "ACTIVE",
                      "statusDescription": "활성화",
                      "connectedAt": "2024-08-04T10:30:00",
                      "createdAt": "2024-08-04T10:00:00"
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 - 이미 처리된 연결 서비스"),
        @ApiResponse(responseCode = "401", description = "인증 실패 - JWT 토큰 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음 - 판매자만 결제 가능"),
        @ApiResponse(responseCode = "404", description = "연결 서비스를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "결제 처리 실패")
    })
    @PostMapping("/{connectionId}/payment")
    public ResponseEntity<ConnectionResponse> processPayment(
            @Parameter(description = "연결 서비스 ID", example = "1") @PathVariable Long connectionId,
            @Parameter(description = "판매자 ID", example = "1") @RequestHeader("User-Id") Long sellerId) {
        
        ConnectionResponse response = connectionService.processConnectionPayment(connectionId, sellerId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "내 판매 연결 서비스 목록",
        description = """
            판매자의 연결 서비스 목록을 조회합니다.
            
            **조회 내용:**
            - 내가 판매한 경매의 연결 서비스 목록
            - 수수료 결제 현황 및 채팅 활성화 상태
            - 거래 진행 상황 및 완료 여부
            
            **상태별 필터링:**
            - PENDING: 수수료 결제 대기 중
            - ACTIVE: 채팅 활성화 (거래 진행 중)
            - COMPLETED: 거래 완료
            - CANCELLED: 취소된 연결
            
            **정렬:** 최신 생성순 (created_at DESC)
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "연결 서비스 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "판매자 연결 목록",
                    value = """
                    {
                      "content": [
                        {
                          "id": 1,
                          "auctionId": 123,
                          "auctionTitle": "iPhone 14 Pro 판매",
                          "sellerId": 1,
                          "sellerNickname": "판매자123",
                          "buyerId": 2,
                          "buyerNickname": "구매자456",
                          "connectionFee": 2100,
                          "finalPrice": 70000,
                          "status": "ACTIVE",
                          "statusDescription": "활성화",
                          "connectedAt": "2024-08-04T10:30:00",
                          "createdAt": "2024-08-04T10:00:00"
                        }
                      ],
                      "pageable": {
                        "page": 0,
                        "size": 20
                      },
                      "totalElements": 1,
                      "totalPages": 1
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/seller/my")
    public ResponseEntity<Page<ConnectionResponse>> getMySellerConnections(
            @Parameter(description = "판매자 ID", example = "1") @RequestHeader("User-Id") Long sellerId,
            @Parameter(description = "페이지 정보 (기본: 0페이지, 20개씩)") @PageableDefault(size = 20) Pageable pageable) {
        
        Page<ConnectionResponse> connections = connectionService.getMySellerConnections(sellerId, pageable);
        return ResponseEntity.ok(connections);
    }
    
    @Operation(
        summary = "내 구매 연결 서비스 목록",
        description = """
            구매자의 연결 서비스 목록을 조회합니다.
            
            **조회 내용:**
            - 내가 낙찰받은 경매의 연결 서비스 목록
            - 판매자의 수수료 결제 현황
            - 채팅 활성화 상태 및 거래 진행 상황
            
            **구매자 관점 상태:**
            - PENDING: 판매자 수수료 결제 대기 중 (채팅 불가)
            - ACTIVE: 채팅 가능 (거래 협의 진행)
            - COMPLETED: 거래 완료
            
            **주의사항:**
            - 구매자는 수수료를 결제하지 않음
            - 판매자가 수수료 결제 완료 시 채팅 활성화
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "연결 서비스 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/buyer/my")
    public ResponseEntity<Page<ConnectionResponse>> getMyBuyerConnections(
            @Parameter(description = "구매자 ID", example = "2") @RequestHeader("User-Id") Long buyerId,
            @Parameter(description = "페이지 정보 (기본: 0페이지, 20개씩)") @PageableDefault(size = 20) Pageable pageable) {
        
        Page<ConnectionResponse> connections = connectionService.getMyBuyerConnections(buyerId, pageable);
        return ResponseEntity.ok(connections);
    }
    
    @Operation(
        summary = "연결 서비스 상세 조회",
        description = """
            특정 연결 서비스의 상세 정보를 조회합니다.
            
            **조회 권한:**
            - 해당 연결의 판매자 또는 구매자만 조회 가능
            - 제3자는 접근 불가 (개인정보 보호)
            
            **상세 정보:**
            - 경매 정보 (제목, ID)
            - 판매자/구매자 정보 (닉네임, ID)
            - 수수료 및 최종 낙찰가
            - 연결 상태 및 진행 단계
            - 채팅 활성화 시간
            - 거래 완료 시간 (해당시)
            
            **활용 목적:**
            - 거래 진행 상황 확인
            - 수수료 결제 필요 여부 확인
            - 채팅 가능 여부 확인
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "연결 서비스 상세 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ConnectionResponse.class)
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음 - 연결 당사자만 조회 가능"),
        @ApiResponse(responseCode = "404", description = "연결 서비스를 찾을 수 없음")
    })
    @GetMapping("/{connectionId}")
    public ResponseEntity<ConnectionResponse> getConnectionDetail(
            @Parameter(description = "연결 서비스 ID", example = "1") @PathVariable Long connectionId,
            @Parameter(description = "사용자 ID (판매자 또는 구매자)", example = "1") @RequestHeader("User-Id") Long userId) {
        
        ConnectionResponse response = connectionService.getConnectionDetail(connectionId, userId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "거래 완료 처리",
        description = """
            연결된 거래를 완료 상태로 변경합니다.
            
            **완료 처리 권한:**
            - 판매자 또는 구매자 모두 완료 처리 가능
            - 양방향 확인 시스템 (추후 구현 예정)
            
            **완료 처리 조건:**
            - 연결 서비스가 ACTIVE 상태여야 함
            - 채팅이 활성화된 상태여야 함
            - 실제 거래가 완료된 후 처리
            
            **완료 처리 효과:**
            - 연결 상태를 COMPLETED로 변경
            - 완료 시간 기록
            - 양측에 완료 알림 발송 (예정)
            - 평점 및 후기 시스템 연동 (예정)
            
            **주의사항:**
            - 완료 처리 후에는 되돌릴 수 없음
            - 거래가 실제로 완료된 후에만 처리해주세요
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "거래 완료 처리 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ConnectionResponse.class),
                examples = @ExampleObject(
                    name = "거래 완료 응답",
                    value = """
                    {
                      "id": 1,
                      "auctionId": 123,
                      "auctionTitle": "iPhone 14 Pro 판매",
                      "sellerId": 1,
                      "sellerNickname": "판매자123",
                      "buyerId": 2,
                      "buyerNickname": "구매자456",
                      "connectionFee": 2100,
                      "finalPrice": 70000,
                      "status": "COMPLETED",
                      "statusDescription": "완료",
                      "connectedAt": "2024-08-04T10:30:00",
                      "completedAt": "2024-08-04T15:45:00",
                      "createdAt": "2024-08-04T10:00:00"
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 - 이미 완료된 거래 또는 비활성화 상태"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음 - 연결 당사자만 완료 처리 가능"),
        @ApiResponse(responseCode = "404", description = "연결 서비스를 찾을 수 없음")
    })
    @PostMapping("/{connectionId}/complete")
    public ResponseEntity<ConnectionResponse> completeTransaction(
            @Parameter(description = "연결 서비스 ID", example = "1") @PathVariable Long connectionId,
            @Parameter(description = "사용자 ID (판매자 또는 구매자)", example = "1") @RequestHeader("User-Id") Long userId) {
        
        ConnectionResponse response = connectionService.completeTransaction(connectionId, userId);
        return ResponseEntity.ok(response);
    }
}