package com.cherrypick.app.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "레벨 기반 권한 정보")
public class LevelPermissionResponse {
    
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    
    @Schema(description = "구매 레벨", example = "25")
    private int buyerLevel;
    
    @Schema(description = "판매 레벨", example = "30")
    private int sellerLevel;
    
    @Schema(description = "입찰 가능 여부", example = "true")
    private boolean canBid;
    
    @Schema(description = "최대 입찰 가능 금액", example = "200000")
    private BigDecimal maxBidAmount;
    
    @Schema(description = "레벨 등급", example = "🟡 성장 (Lv 21-40)")
    private String levelTier;
    
    @Schema(description = "사용자 권한 목록")
    private String[] permissions;
    
    @Schema(description = "사용자 혜택 목록")
    private String[] benefits;
    
    @Schema(description = "연결 수수료 할인율", example = "0.1")
    private double connectionFeeDiscount;
    
    /**
     * 입찰 제한 메시지 조회
     */
    public String getBidLimitMessage() {
        if (canBid) {
            return null;
        }
        
        return String.format("현재 구매 레벨(%d)로는 최대 %s원까지 입찰 가능합니다. " +
                           "더 높은 금액 입찰을 위해서는 레벨업이 필요합니다.", 
                           buyerLevel, formatCurrency(maxBidAmount));
    }
    
    /**
     * 수수료 할인 메시지 조회
     */
    public String getDiscountMessage() {
        if (connectionFeeDiscount > 0) {
            return String.format("판매 레벨(%d) 혜택으로 연결 수수료 %d%% 할인 적용", 
                               sellerLevel, (int)(connectionFeeDiscount * 100));
        }
        
        return "레벨업 시 연결 수수료 할인 혜택을 받을 수 있습니다.";
    }
    
    /**
     * 통화 포맷팅
     */
    private String formatCurrency(BigDecimal amount) {
        return String.format("%,d", amount.longValue());
    }
}