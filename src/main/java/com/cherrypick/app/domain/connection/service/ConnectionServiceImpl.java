package com.cherrypick.app.domain.connection.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.connection.dto.request.CreateConnectionRequest;
import com.cherrypick.app.domain.connection.dto.response.ConnectionResponse;
import com.cherrypick.app.domain.connection.entity.ConnectionService;
import com.cherrypick.app.domain.connection.enums.ConnectionStatus;
import com.cherrypick.app.domain.connection.repository.ConnectionServiceRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import com.cherrypick.app.domain.user.service.LevelPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConnectionServiceImpl {
    
    private final ConnectionServiceRepository connectionServiceRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final LevelPermissionService levelPermissionService;
    
    /**
     * 연결 서비스 생성 (경매 낙찰 시 자동 생성)
     * 
     * 비즈니스 로직:
     * 1. 경매 낙찰 완료 확인
     * 2. 판매자 레벨별 할인율 조회 (0%~40%)
     * 3. 연결 서비스 엔티티 생성
     * 4. 판매자에게 연결 서비스 결제 안내
     * 
     * @param auctionId 경매 ID
     * @param sellerId 판매자 ID
     * @param buyerId 구매자 ID
     * @param finalPrice 최종 낙찰가
     * @return 연결 서비스 정보
     */
    @Transactional
    public ConnectionResponse createConnection(Long auctionId, Long sellerId, Long buyerId, BigDecimal finalPrice) {
        // 경매 정보 확인
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다."));
        
        // 판매자/구매자 정보 확인
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));
        
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("구매자를 찾을 수 없습니다."));
        
        // 판매자 레벨별 할인율 조회
        double discountRate = levelPermissionService.getConnectionFeeDiscount(sellerId);
        
        // 수수료 계산 (기본 3% + 레벨별 할인 적용)
        BigDecimal baseFeeRate = BigDecimal.valueOf(0.03); // 기본 3%
        BigDecimal baseFee = finalPrice.multiply(baseFeeRate);
        BigDecimal discount = baseFee.multiply(BigDecimal.valueOf(discountRate));
        BigDecimal connectionFee = baseFee.subtract(discount);
        
        // 연결 서비스 생성
        ConnectionService connectionService = ConnectionService.createConnection(
                auction, seller, buyer, finalPrice, connectionFee);
        
        ConnectionService savedConnection = connectionServiceRepository.save(connectionService);
        
        return ConnectionResponse.from(savedConnection);
    }
    
    /**
     * 연결 서비스 결제 처리
     * 
     * 비즈니스 로직:
     * 1. 판매자가 연결 수수료 결제
     * 2. 연결 서비스 활성화
     * 3. 채팅방 생성 및 양방향 연결
     * 4. 양측에 연결 완료 알림
     * 
     * @param connectionId 연결 서비스 ID
     * @param sellerId 판매자 ID
     * @return 연결 서비스 정보
     */
    @Transactional
    public ConnectionResponse processConnectionPayment(Long connectionId, Long sellerId) {
        ConnectionService connection = connectionServiceRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("연결 서비스를 찾을 수 없습니다."));
        
        // 판매자 권한 확인
        if (!connection.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("연결 서비스 결제 권한이 없습니다.");
        }
        
        // 연결 서비스 상태 확인
        if (connection.getStatus() != ConnectionStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 연결 서비스입니다.");
        }
        
        // TODO: 실제 결제 처리 로직 (PG 연동)
        // PaymentResult paymentResult = paymentService.processPayment(sellerId, connection.getConnectionFee());
        // if (!paymentResult.isSuccess()) {
        //     throw new IllegalStateException("결제 처리에 실패했습니다.");
        // }
        
        // 연결 서비스 활성화
        connection.activateConnection();
        
        // TODO: 채팅방 생성 (별도 서비스)
        // chatService.createChatRoom(connection.getSeller(), connection.getBuyer(), connection.getAuction());
        
        // TODO: 양측 알림 발송
        // notificationService.sendConnectionActivatedNotification(connection);
        
        connectionServiceRepository.save(connection);
        
        return ConnectionResponse.from(connection);
    }
    
    /**
     * 내 연결 서비스 목록 조회 (판매자용)
     */
    public Page<ConnectionResponse> getMySellerConnections(Long sellerId, Pageable pageable) {
        Page<ConnectionService> connections = connectionServiceRepository
                .findBySellerIdOrderByCreatedAtDesc(sellerId, pageable);
        
        return connections.map(ConnectionResponse::from);
    }
    
    /**
     * 내 연결 서비스 목록 조회 (구매자용)
     */
    public Page<ConnectionResponse> getMyBuyerConnections(Long buyerId, Pageable pageable) {
        Page<ConnectionService> connections = connectionServiceRepository
                .findByBuyerIdOrderByCreatedAtDesc(buyerId, pageable);
        
        return connections.map(ConnectionResponse::from);
    }
    
    /**
     * 연결 서비스 상세 조회
     */
    public ConnectionResponse getConnectionDetail(Long connectionId, Long userId) {
        ConnectionService connection = connectionServiceRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("연결 서비스를 찾을 수 없습니다."));
        
        // 권한 확인 (판매자 또는 구매자만 조회 가능)
        if (!connection.getSeller().getId().equals(userId) && 
            !connection.getBuyer().getId().equals(userId)) {
            throw new IllegalArgumentException("연결 서비스 조회 권한이 없습니다.");
        }
        
        return ConnectionResponse.from(connection);
    }
    
    /**
     * 거래 완료 처리
     */
    @Transactional
    public ConnectionResponse completeTransaction(Long connectionId, Long userId) {
        ConnectionService connection = connectionServiceRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("연결 서비스를 찾을 수 없습니다."));
        
        // 권한 확인
        if (!connection.getSeller().getId().equals(userId) && 
            !connection.getBuyer().getId().equals(userId)) {
            throw new IllegalArgumentException("거래 완료 처리 권한이 없습니다.");
        }
        
        // 연결 서비스 상태 확인
        if (connection.getStatus() != ConnectionStatus.ACTIVE) {
            throw new IllegalStateException("활성화된 연결 서비스만 완료 처리할 수 있습니다.");
        }
        
        // 거래 완료 처리
        connection.completeTransaction();
        
        // TODO: 양방향 거래 완료 확인 시스템 구현
        // transactionService.processTransactionCompletion(connection);
        
        connectionServiceRepository.save(connection);
        
        return ConnectionResponse.from(connection);
    }
}