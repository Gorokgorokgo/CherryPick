package com.cherrypick.app.domain.transaction.dto.response;

import com.cherrypick.app.domain.transaction.entity.Transaction;
import com.cherrypick.app.domain.transaction.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private Long id;
    private Long auctionId;
    private String auctionTitle;
    private Long sellerId;
    private String sellerNickname;
    private Long buyerId;
    private String buyerNickname;
    private BigDecimal finalPrice;
    private BigDecimal commissionFee;
    private BigDecimal sellerAmount;
    private TransactionStatus status;
    private Boolean sellerConfirmed;
    private Boolean buyerConfirmed;
    private LocalDateTime sellerConfirmedAt;
    private LocalDateTime buyerConfirmedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 후기 작성 가능 여부
    private Boolean canWriteReview;
    private Boolean hasWrittenReview;

    public static TransactionResponse from(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .auctionId(transaction.getAuction().getId())
                .auctionTitle(transaction.getAuction().getTitle())
                .sellerId(transaction.getSeller().getId())
                .sellerNickname(transaction.getSeller().getNickname())
                .buyerId(transaction.getBuyer().getId())
                .buyerNickname(transaction.getBuyer().getNickname())
                .finalPrice(transaction.getFinalPrice())
                .commissionFee(transaction.getCommissionFee())
                .sellerAmount(transaction.getSellerAmount())
                .status(transaction.getStatus())
                .sellerConfirmed(transaction.getSellerConfirmed())
                .buyerConfirmed(transaction.getBuyerConfirmed())
                .sellerConfirmedAt(transaction.getSellerConfirmedAt())
                .buyerConfirmedAt(transaction.getBuyerConfirmedAt())
                .completedAt(transaction.getCompletedAt())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .canWriteReview(transaction.getStatus() == TransactionStatus.COMPLETED)
                .hasWrittenReview(false) // 추후 ReviewService에서 설정
                .build();
    }

    public static TransactionResponse from(Transaction transaction, boolean hasWrittenReview) {
        TransactionResponse response = from(transaction);
        response.hasWrittenReview = hasWrittenReview;
        // 후기를 이미 작성했다면 작성 불가능
        if (hasWrittenReview) {
            response.canWriteReview = false;
        }
        return response;
    }
}
