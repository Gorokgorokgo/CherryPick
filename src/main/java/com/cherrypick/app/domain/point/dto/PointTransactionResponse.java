package com.cherrypick.app.domain.point.dto;

import com.cherrypick.app.domain.point.PointTransaction;
import com.cherrypick.app.domain.point.PointTransactionStatus;
import com.cherrypick.app.domain.point.PointTransactionType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class PointTransactionResponse {
    
    private Long id;
    private PointTransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String relatedType;
    private Long relatedId;
    private String description;
    private PointTransactionStatus status;
    private LocalDateTime createdAt;
    
    public static PointTransactionResponse from(PointTransaction transaction) {
        PointTransactionResponse response = new PointTransactionResponse();
        response.setId(transaction.getId());
        response.setType(transaction.getType());
        response.setAmount(transaction.getAmount());
        response.setBalanceAfter(transaction.getBalanceAfter());
        response.setRelatedType(transaction.getRelatedType());
        response.setRelatedId(transaction.getRelatedId());
        response.setDescription(transaction.getDescription());
        response.setStatus(transaction.getStatus());
        response.setCreatedAt(transaction.getCreatedAt());
        return response;
    }
}