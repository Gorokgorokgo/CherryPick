package com.cherrypick.app.domain.transaction.controller;

import com.cherrypick.app.domain.transaction.dto.request.CreateReviewRequest;
import com.cherrypick.app.domain.transaction.dto.response.ReviewResponse;
import com.cherrypick.app.domain.transaction.dto.response.ReviewStatsResponse;
import com.cherrypick.app.domain.transaction.enums.ReviewType;
import com.cherrypick.app.domain.transaction.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "후기 관리", description = """
    거래 후기 작성 및 조회 API

    **3단계 후기 시스템:**
    - 😊 좋았어요: 착하게 거래를 진행해주셨어요
    - 😐 평범해요: 좋은 물건 감사합니다
    - 😞 별로에요: 답변이 너무 느려요

    **후기 플로우:**
    1. 거래 완료 (양방향 확인)
    2. 후기 작성 가능 상태
    3. 판매자/구매자 각각 후기 작성
    4. 후기 작성 시 +10 EXP 보너스
    5. 상대방에게 알림 발송
    """)
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(
        summary = "거래 후기 작성",
        description = """
            완료된 거래에 대한 후기를 작성합니다.

            **작성 조건:**
            - 거래가 COMPLETED 상태여야 함
            - 거래 당사자(판매자 또는 구매자)만 작성 가능
            - 1회만 작성 가능 (중복 작성 불가)

            **3단계 평가:**
            - GOOD: 좋았어요 (긍정 평가)
            - NORMAL: 평범해요 (중립 평가)
            - BAD: 별로에요 (부정 평가)

            **작성 효과:**
            - 후기 작성자: +10 EXP 보너스
            - 후기 대상자: 통계에 반영
            - 상대방에게 알림 발송
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "후기 작성 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ReviewResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "reviewId": 1,
                      "transactionId": 123,
                      "reviewerId": 2,
                      "reviewerNickname": "구매자456",
                      "revieweeId": 1,
                      "revieweeNickname": "판매자123",
                      "reviewType": "SELLER",
                      "ratingType": "GOOD",
                      "experienceBonus": 10,
                      "createdAt": "2025-11-01T15:00:00"
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 - 거래 미완료 또는 중복 작성"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음 - 거래 당사자만 작성 가능"),
        @ApiResponse(responseCode = "404", description = "거래를 찾을 수 없음")
    })
    @PostMapping("/transactions/{transactionId}")
    public ResponseEntity<ReviewResponse> createReview(
            @Parameter(description = "거래 ID", example = "123") @PathVariable Long transactionId,
            @Parameter(description = "사용자 ID", example = "2") @RequestHeader("User-Id") Long userId,
            @Parameter(description = "후기 작성 요청") @Valid @RequestBody CreateReviewRequest request) {

        ReviewResponse response = reviewService.createReview(transactionId, request, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "사용자 후기 통계 조회",
        description = """
            특정 사용자의 후기 통계를 조회합니다.

            **조회 가능 타입:**
            - SELLER: 판매자로서 받은 후기 통계
            - BUYER: 구매자로서 받은 후기 통계

            **응답 정보:**
            - 좋았어요 / 평범해요 / 별로에요 개수
            - 총 후기 수
            - 긍정 평가율 (goodCount / totalReviews * 100)
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "후기 통계 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ReviewStatsResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "userId": 1,
                      "reviewType": "SELLER",
                      "goodCount": 45,
                      "normalCount": 3,
                      "badCount": 1,
                      "totalReviews": 49,
                      "positiveRate": 91.84
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/users/{userId}/stats")
    public ResponseEntity<ReviewStatsResponse> getReviewStats(
            @Parameter(description = "사용자 ID", example = "1") @PathVariable Long userId,
            @Parameter(description = "후기 타입", example = "SELLER")
            @RequestParam(defaultValue = "SELLER") ReviewType type) {

        ReviewStatsResponse stats = reviewService.getReviewStats(userId, type);
        return ResponseEntity.ok(stats);
    }

    @Operation(
        summary = "받은 후기 목록 조회",
        description = """
            특정 사용자가 받은 후기 목록을 조회합니다.

            **조회 가능 타입:**
            - SELLER: 판매자로서 받은 후기
            - BUYER: 구매자로서 받은 후기
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "후기 목록 조회 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/users/{userId}")
    public ResponseEntity<java.util.List<ReviewResponse>> getReviews(
            @Parameter(description = "사용자 ID", example = "1") @PathVariable Long userId,
            @Parameter(description = "후기 타입", example = "SELLER")
            @RequestParam(defaultValue = "SELLER") ReviewType type) {

        java.util.List<ReviewResponse> reviews = reviewService.getReviews(userId, type);
        return ResponseEntity.ok(reviews);
    }
}
