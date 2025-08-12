package com.cherrypick.app.domain.connection.entity;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.common.entity.BaseEntity;
import com.cherrypick.app.domain.connection.enums.ConnectionStatus;
import com.cherrypick.app.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "connection_services")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionService extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Column(name = "connection_fee", nullable = false, precision = 10, scale = 0)
    private BigDecimal connectionFee;

    @Column(name = "final_price", nullable = false, precision = 10, scale = 0)
    private BigDecimal finalPrice;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConnectionStatus status = ConnectionStatus.PENDING;

    @Column(name = "connected_at")
    private LocalDateTime connectedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // === 정적 팩토리 메서드 ===
    
    /**
     * 연결 서비스 생성
     */
    public static ConnectionService createConnection(
            Auction auction, 
            User seller, 
            User buyer, 
            BigDecimal finalPrice,
            BigDecimal connectionFee) {
        
        // 연결 수수료는 외부에서 레벨별로 계산되어 전달됨
        
        return ConnectionService.builder()
                .auction(auction)
                .seller(seller)
                .buyer(buyer)
                .connectionFee(connectionFee)
                .finalPrice(finalPrice)
                .status(ConnectionStatus.PENDING)
                .build();
    }

    // === 비즈니스 메서드 ===
    
    /**
     * 연결 서비스 활성화 (수수료 결제 완료 시)
     */
    public void activateConnection() {
        this.status = ConnectionStatus.ACTIVE;
        this.connectedAt = LocalDateTime.now();
    }
    
    /**
     * 거래 완료 처리
     */
    public void completeTransaction() {
        this.status = ConnectionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * 연결 서비스 취소
     */
    public void cancelConnection() {
        this.status = ConnectionStatus.CANCELLED;
    }
    
    /**
     * 연결 서비스가 활성화되었는지 확인
     */
    public boolean isActive() {
        return this.status == ConnectionStatus.ACTIVE;
    }
    
    /**
     * 거래가 완료되었는지 확인
     */
    public boolean isCompleted() {
        return this.status == ConnectionStatus.COMPLETED;
    }
}