package com.cherrypick.app.domain.transaction.controller;

import com.cherrypick.app.domain.transaction.dto.response.TransactionConfirmResponse;
import com.cherrypick.app.domain.transaction.dto.response.TransactionResponse;
import com.cherrypick.app.domain.transaction.enums.TransactionStatus;
import com.cherrypick.app.domain.transaction.service.TransactionService;
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

@Tag(name = "거래 관리", description = """
    거래 완료 확인 및 거래 내역 관리 API

    **거래 플로우:**
    1. 경매 종료 → Transaction 생성 (PENDING)
    2. 판매자/구매자 각각 거래 확인
    3. 양방향 확인 완료 → COMPLETED + 경험치 지급
    4. 후기 작성 가능
    """)
@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(
        summary = "거래 확인 (판매자/구매자)",
        description = """
            거래 완료를 확인합니다.

            **확인 프로세스:**
            - 판매자 또는 구매자가 거래 완료 버튼 클릭
            - 상대방이 이미 확인했다면 → 즉시 거래 완료
            - 상대방이 아직 확인 안 했다면 → 대기 상태

            **거래 완료 시 자동 처리:**
            - Transaction 상태 → COMPLETED
            - 판매자에게 수령 금액 지급
            - 양측에 경험치 지급 (기본 80 + 금액별 보너스)
            - 거래 완료 알림 발송
            - 후기 작성 가능 상태로 전환
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "거래 확인 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TransactionConfirmResponse.class),
                examples = {
                    @ExampleObject(
                        name = "단일 확인 (상대방 대기 중)",
                        value = """
                        {
                          "transactionId": 123,
                          "status": "SELLER_CONFIRMED",
                          "sellerConfirmed": true,
                          "buyerConfirmed": false,
                          "completedAt": null,
                          "canWriteReview": false,
                          "message": "거래 확인이 완료되었습니다. 상대방의 확인을 기다리는 중입니다."
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "양방향 확인 완료",
                        value = """
                        {
                          "transactionId": 123,
                          "status": "COMPLETED",
                          "sellerConfirmed": true,
                          "buyerConfirmed": true,
                          "completedAt": "2025-11-01T14:30:00",
                          "canWriteReview": true,
                          "message": "거래가 완료되었습니다! 경험치가 지급되었습니다. 후기를 작성해주세요."
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 - 이미 확인했거나 거래 상태가 적절하지 않음"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음 - 거래 당사자만 확인 가능"),
        @ApiResponse(responseCode = "404", description = "거래를 찾을 수 없음")
    })
    @PostMapping("/{transactionId}/confirm")
    public ResponseEntity<TransactionConfirmResponse> confirmTransaction(
            @Parameter(description = "거래 ID", example = "123") @PathVariable Long transactionId,
            @Parameter(description = "사용자 ID", example = "1") @RequestHeader("User-Id") Long userId) {

        TransactionConfirmResponse response = transactionService.confirmTransaction(transactionId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "경매 ID로 거래 확인 (유찰 경매 직거래)",
        description = "경매 ID로 거래를 확인합니다. Transaction이 없으면 자동으로 생성합니다 (유찰 경매 직거래용)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "거래 확인 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음")
    })
    @PostMapping("/auction/{auctionId}/confirm")
    public ResponseEntity<TransactionConfirmResponse> confirmTransactionByAuction(
            @Parameter(description = "경매 ID", example = "123") @PathVariable Long auctionId,
            @Parameter(description = "사용자 ID", example = "1") @RequestHeader("User-Id") Long userId) {

        TransactionConfirmResponse response = transactionService.confirmTransactionByAuction(auctionId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "경매 ID로 거래 조회",
        description = "경매 ID로 해당 경매의 거래 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "거래 조회 성공"),
        @ApiResponse(responseCode = "404", description = "거래를 찾을 수 없음")
    })
    @GetMapping("/auction/{auctionId}")
    public ResponseEntity<TransactionResponse> getTransactionByAuction(
            @Parameter(description = "경매 ID", example = "123") @PathVariable Long auctionId) {

        TransactionResponse response = transactionService.getTransactionByAuction(auctionId);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "내 거래 내역 조회",
        description = """
            사용자의 거래 내역을 조회합니다.

            **필터링:**
            - status: 거래 상태별 필터링 (선택 사항)
            - 전체 조회 시 status 파라미터 생략

            **정렬:** 최신 생성순 (created_at DESC)
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "거래 내역 조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "content": [
                        {
                          "id": 123,
                          "auctionId": 456,
                          "auctionTitle": "iPhone 14 Pro 판매",
                          "sellerId": 1,
                          "sellerNickname": "판매자123",
                          "buyerId": 2,
                          "buyerNickname": "구매자456",
                          "finalPrice": 700000,
                          "status": "COMPLETED",
                          "sellerConfirmed": true,
                          "buyerConfirmed": true,
                          "completedAt": "2025-11-01T14:30:00",
                          "canWriteReview": true,
                          "hasWrittenReview": false
                        }
                      ],
                      "totalElements": 10,
                      "totalPages": 1
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/my")
    public ResponseEntity<Page<TransactionResponse>> getMyTransactions(
            @Parameter(description = "사용자 ID", example = "1") @RequestHeader("User-Id") Long userId,
            @Parameter(description = "거래 상태 필터 (선택)", example = "PENDING")
            @RequestParam(required = false) TransactionStatus status,
            @Parameter(description = "페이지 정보") @PageableDefault(size = 20) Pageable pageable) {

        Page<TransactionResponse> transactions = transactionService.getMyTransactions(userId, status, pageable);
        return ResponseEntity.ok(transactions);
    }

    @Operation(
        summary = "거래 상세 조회",
        description = """
            특정 거래의 상세 정보를 조회합니다.

            **조회 권한:**
            - 거래 당사자(판매자 또는 구매자)만 조회 가능
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "거래 상세 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TransactionResponse.class)
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음 - 거래 당사자만 조회 가능"),
        @ApiResponse(responseCode = "404", description = "거래를 찾을 수 없음")
    })
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransactionDetail(
            @Parameter(description = "거래 ID", example = "123") @PathVariable Long transactionId,
            @Parameter(description = "사용자 ID", example = "1") @RequestHeader("User-Id") Long userId) {

        TransactionResponse transaction = transactionService.getTransactionDetail(transactionId, userId);
        return ResponseEntity.ok(transaction);
    }
}
