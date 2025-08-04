package com.cherrypick.app.domain.connection.dto.response;

import com.cherrypick.app.domain.connection.entity.ConnectionService;
import com.cherrypick.app.domain.connection.enums.ConnectionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "연결 서비스 응답 정보")
public class ConnectionResponse {
    
    @Schema(description = "연결 서비스 ID", example = "1")
    private Long id;
    
    @Schema(description = "경매 ID", example = "123")
    private Long auctionId;
    
    @Schema(description = "경매 제목", example = "iPhone 14 Pro 판매")
    private String auctionTitle;
    
    @Schema(description = "판매자 ID", example = "1")
    private Long sellerId;
    
    @Schema(description = "판매자 닉네임", example = "판매자123")
    private String sellerNickname;
    
    @Schema(description = "구매자 ID", example = "2")
    private Long buyerId;
    
    @Schema(description = "구매자 닉네임", example = "구매자456")
    private String buyerNickname;
    
    @Schema(description = "연결 수수료 (원)", example = "2100")
    private BigDecimal connectionFee;
    
    @Schema(description = "최종 낙찰가 (원)", example = "70000")
    private BigDecimal finalPrice;
    
    @Schema(description = "연결 상태", example = "ACTIVE")
    private ConnectionStatus status;
    
    @Schema(description = "연결 상태 설명", example = "활성화")
    private String statusDescription;
    
    @Schema(description = "채팅 활성화 시간", example = "2024-08-04T10:30:00")
    private LocalDateTime connectedAt;
    
    @Schema(description = "거래 완료 시간", example = "2024-08-04T15:45:00")
    private LocalDateTime completedAt;
    
    @Schema(description = "연결 서비스 생성 시간", example = "2024-08-04T10:00:00")
    private LocalDateTime createdAt;
    
    public static ConnectionResponse from(ConnectionService connection) {
        return ConnectionResponse.builder()
                .id(connection.getId())
                .auctionId(connection.getAuction().getId())
                .auctionTitle(connection.getAuction().getTitle())
                .sellerId(connection.getSeller().getId())
                .sellerNickname(connection.getSeller().getNickname())
                .buyerId(connection.getBuyer().getId())
                .buyerNickname(connection.getBuyer().getNickname())
                .connectionFee(connection.getConnectionFee())
                .finalPrice(connection.getFinalPrice())
                .status(connection.getStatus())
                .statusDescription(connection.getStatus().getDescription())
                .connectedAt(connection.getConnectedAt())
                .completedAt(connection.getCompletedAt())
                .createdAt(connection.getCreatedAt())
                .build();
    }
}