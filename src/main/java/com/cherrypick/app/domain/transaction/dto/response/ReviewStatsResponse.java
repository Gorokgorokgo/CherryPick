package com.cherrypick.app.domain.transaction.dto.response;

import com.cherrypick.app.domain.transaction.enums.ReviewType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatsResponse {

    private Long userId;
    private ReviewType reviewType;
    private Integer goodCount;
    private Integer normalCount;
    private Integer badCount;
    private Integer totalReviews;
    private Double positiveRate; // 긍정 평가율 (goodCount / totalReviews * 100)

    public static ReviewStatsResponse of(Long userId, ReviewType reviewType,
                                        int goodCount, int normalCount, int badCount) {
        int totalReviews = goodCount + normalCount + badCount;
        double positiveRate = totalReviews > 0 ? (goodCount * 100.0 / totalReviews) : 0.0;

        // 소수점 둘째 자리까지 반올림
        positiveRate = Math.round(positiveRate * 100.0) / 100.0;

        return ReviewStatsResponse.builder()
                .userId(userId)
                .reviewType(reviewType)
                .goodCount(goodCount)
                .normalCount(normalCount)
                .badCount(badCount)
                .totalReviews(totalReviews)
                .positiveRate(positiveRate)
                .build();
    }
}
