package com.cherrypick.app.domain.connection.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateConnectionRequest {
    
    private Long auctionId;
    private Long sellerId;
    private Long buyerId;
    private BigDecimal finalPrice;
    
    public void validate() {
        if (auctionId == null) {
            throw new IllegalArgumentException("경매 ID는 필수입니다.");
        }
        
        if (sellerId == null) {
            throw new IllegalArgumentException("판매자 ID는 필수입니다.");
        }
        
        if (buyerId == null) {
            throw new IllegalArgumentException("구매자 ID는 필수입니다.");
        }
        
        if (finalPrice == null || finalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("최종 낙찰가는 0원보다 커야 합니다.");
        }
        
        if (sellerId.equals(buyerId)) {
            throw new IllegalArgumentException("판매자와 구매자가 동일할 수 없습니다.");
        }
    }
}