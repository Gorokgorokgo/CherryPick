package com.cherrypick.app.domain.transaction.dto.response;

import com.cherrypick.app.domain.transaction.entity.TransactionReview;
import com.cherrypick.app.domain.transaction.enums.RatingType;
import com.cherrypick.app.domain.transaction.enums.ReviewType;
import com.cherrypick.app.domain.user.dto.response.ExperienceGainResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long reviewId;
    private Long transactionId;
    private Long reviewerId;
    private String reviewerNickname;
    private Long revieweeId;
    private String revieweeNickname;
    private ReviewType reviewType;
    private RatingType ratingType;
    private String content;
    private Integer experienceBonus;
    private ExperienceGainResponse experienceData; // 경험치 상세 데이터
    private LocalDateTime createdAt;

    public static ReviewResponse from(TransactionReview review, Integer experienceBonus) {
        return ReviewResponse.builder()
                .reviewId(review.getId())
                .transactionId(review.getTransaction().getId())
                .reviewerId(review.getReviewer().getId())
                .reviewerNickname(review.getReviewer().getNickname())
                .revieweeId(review.getReviewee().getId())
                .revieweeNickname(review.getReviewee().getNickname())
                .reviewType(review.getReviewType())
                .ratingType(review.getRatingType())
                .content(review.getContent())
                .experienceBonus(experienceBonus)
                .createdAt(review.getCreatedAt())
                .build();
    }

    public static ReviewResponse fromWithExperience(TransactionReview review, ExperienceGainResponse experienceResponse) {
        return ReviewResponse.builder()
                .reviewId(review.getId())
                .transactionId(review.getTransaction().getId())
                .reviewerId(review.getReviewer().getId())
                .reviewerNickname(review.getReviewer().getNickname())
                .revieweeId(review.getReviewee().getId())
                .revieweeNickname(review.getReviewee().getNickname())
                .reviewType(review.getReviewType())
                .ratingType(review.getRatingType())
                .content(review.getContent())
                .experienceBonus(experienceResponse.getExpGained())
                .experienceData(experienceResponse)
                .createdAt(review.getCreatedAt())
                .build();
    }
}
