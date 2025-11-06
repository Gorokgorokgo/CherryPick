package com.cherrypick.app.domain.transaction.dto.response;

import com.cherrypick.app.domain.transaction.enums.TransactionStatus;
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
public class TransactionConfirmResponse {

    private Long transactionId;
    private TransactionStatus status;
    private Boolean sellerConfirmed;
    private Boolean buyerConfirmed;
    private LocalDateTime completedAt;
    private Boolean canWriteReview;
    private String message;

    // 경험치 정보 (거래 완료 시에만)
    private ExperienceGainResponse buyerExperience;
    private ExperienceGainResponse sellerExperience;

    public static TransactionConfirmResponse of(Long transactionId, TransactionStatus status,
                                                Boolean sellerConfirmed, Boolean buyerConfirmed,
                                                LocalDateTime completedAt, String message, Boolean canWriteReview) {
        return TransactionConfirmResponse.builder()
                .transactionId(transactionId)
                .status(status)
                .sellerConfirmed(sellerConfirmed)
                .buyerConfirmed(buyerConfirmed)
                .completedAt(completedAt)
                .canWriteReview(canWriteReview)
                .message(message)
                .build();
    }

    public static TransactionConfirmResponse ofWithExperience(
            Long transactionId, TransactionStatus status,
            Boolean sellerConfirmed, Boolean buyerConfirmed,
            LocalDateTime completedAt, String message, Boolean canWriteReview,
            ExperienceGainResponse buyerExperience, ExperienceGainResponse sellerExperience) {
        return TransactionConfirmResponse.builder()
                .transactionId(transactionId)
                .status(status)
                .sellerConfirmed(sellerConfirmed)
                .buyerConfirmed(buyerConfirmed)
                .completedAt(completedAt)
                .canWriteReview(canWriteReview)
                .message(message)
                .buyerExperience(buyerExperience)
                .sellerExperience(sellerExperience)
                .build();
    }
}
