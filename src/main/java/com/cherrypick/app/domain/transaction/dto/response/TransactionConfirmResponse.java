package com.cherrypick.app.domain.transaction.dto.response;

import com.cherrypick.app.domain.transaction.enums.TransactionStatus;
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

    public static TransactionConfirmResponse of(Long transactionId, TransactionStatus status,
                                                Boolean sellerConfirmed, Boolean buyerConfirmed,
                                                LocalDateTime completedAt, String message) {
        return TransactionConfirmResponse.builder()
                .transactionId(transactionId)
                .status(status)
                .sellerConfirmed(sellerConfirmed)
                .buyerConfirmed(buyerConfirmed)
                .completedAt(completedAt)
                .canWriteReview(status == TransactionStatus.COMPLETED)
                .message(message)
                .build();
    }
}
