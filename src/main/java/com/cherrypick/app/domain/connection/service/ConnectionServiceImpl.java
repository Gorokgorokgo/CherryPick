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
import com.cherrypick.app.domain.connection.dto.request.PayConnectionFeeRequest;
import com.cherrypick.app.domain.connection.dto.response.ConnectionFeeResponse;
import com.cherrypick.app.domain.connection.dto.response.PaymentResult;
import com.cherrypick.app.domain.point.service.PointService;
import com.cherrypick.app.domain.chat.service.ChatService;
import com.cherrypick.app.domain.notification.service.FcmService;
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
    private final ConnectionFeeCalculator connectionFeeCalculator;
    private final PointService pointService;
    private final ChatService chatService;
    private final FcmService fcmService;
    
    /**
     * 연결 서비스 생성 (스케줄러용 - 간소화된 파라미터)
     * 
     * @param auctionId 경매 ID
     * @param buyerId 구매자 ID (낙찰자)
     * @return 연결 서비스 정보
     */
    @Transactional
    public ConnectionResponse createConnection(Long auctionId, Long buyerId) {
        // 경매 정보 확인
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다."));
        
        // 구매자 정보 확인
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("구매자를 찾을 수 없습니다."));
        
        // 기존 메서드 호출 (판매자 정보와 최종 낙찰가는 경매에서 추출)
        return createConnection(
            auctionId, 
            auction.getSeller().getId(), 
            buyerId, 
            auction.getCurrentPrice()
        );
    }
    
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
        
        // 수수료 계산 (현재 무료, 추후 점진적 인상)
        ConnectionFeeResponse feeInfo = connectionFeeCalculator.calculateConnectionFee(seller, finalPrice);
        BigDecimal connectionFee = feeInfo.getFinalFee();
        
        // 연결 서비스 생성
        ConnectionService connectionService = ConnectionService.createConnection(
                auction, seller, buyer, finalPrice, connectionFee);
        
        ConnectionService savedConnection = connectionServiceRepository.save(connectionService);

        // 판매자에게 연결 서비스 결제 요청 알림 발송 (제거됨 - 경매 종료 시 알림 불필요)
        // fcmService.sendConnectionPaymentRequestNotification(
        //         seller, savedConnection.getId(), auction.getTitle());

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
        
        // 채팅방 활성화
        chatService.activateChatRoom(connection);
        
        // 구매자에게 채팅 활성화 알림 발송
        fcmService.sendChatActivationNotification(
                connection.getBuyer(), connectionId, connection.getAuction().getTitle());
        
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
    
    /**
     * 연결 서비스 수수료 정보 조회
     * 
     * @param connectionId 연결 서비스 ID
     * @param sellerId 판매자 ID (권한 확인용)
     * @return 수수료 계산 정보
     */
    public ConnectionFeeResponse getConnectionFeeInfo(Long connectionId, Long sellerId) {
        ConnectionService connection = connectionServiceRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("연결 서비스를 찾을 수 없습니다."));
        
        // 판매자 권한 확인
        if (!connection.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("수수료 정보 조회 권한이 없습니다.");
        }
        
        // 현재 수수료 정보 계산
        ConnectionFeeResponse feeInfo = connectionFeeCalculator.calculateConnectionFee(
                connection.getSeller(), connection.getFinalPrice());
        
        return ConnectionFeeResponse.builder()
                .connectionId(connectionId)
                .finalPrice(feeInfo.getFinalPrice())
                .baseFeeRate(feeInfo.getBaseFeeRate())
                .baseFee(feeInfo.getBaseFee())
                .discountRate(feeInfo.getDiscountRate())
                .discountAmount(feeInfo.getDiscountAmount())
                .finalFee(feeInfo.getFinalFee())
                .sellerLevel(feeInfo.getSellerLevel())
                .isFreePromotion(feeInfo.getIsFreePromotion())
                .build();
    }
    
    /**
     * 연결 서비스 수수료 결제 처리 (신규)
     * 
     * @param request 결제 요청 정보
     * @param sellerId 판매자 ID
     * @return 결제 결과
     */
    @Transactional
    public PaymentResult payConnectionFee(PayConnectionFeeRequest request, Long sellerId) {
        ConnectionService connection = connectionServiceRepository.findById(request.getConnectionId())
                .orElseThrow(() -> new IllegalArgumentException("연결 서비스를 찾을 수 없습니다."));
        
        // 판매자 권한 확인
        if (!connection.getSeller().getId().equals(sellerId)) {
            return PaymentResult.builder()
                    .connectionId(request.getConnectionId())
                    .success(false)
                    .message("결제 권한이 없습니다.")
                    .errorCode("UNAUTHORIZED")
                    .build();
        }
        
        // 연결 서비스 상태 확인
        if (connection.getStatus() != ConnectionStatus.PENDING) {
            return PaymentResult.builder()
                    .connectionId(request.getConnectionId())
                    .success(false)
                    .message("이미 처리된 연결 서비스입니다.")
                    .errorCode("ALREADY_PROCESSED")
                    .build();
        }
        
        // 수수료 계산 및 검증
        ConnectionFeeResponse feeInfo = connectionFeeCalculator.calculateConnectionFee(
                connection.getSeller(), connection.getFinalPrice());
        
        // 프론트엔드 계산과 서버 계산 일치 확인
        if (!feeInfo.getFinalFee().equals(BigDecimal.valueOf(request.getExpectedFee()))) {
            return PaymentResult.builder()
                    .connectionId(request.getConnectionId())
                    .success(false)
                    .message("수수료 계산이 일치하지 않습니다. 새로고침 후 다시 시도해주세요.")
                    .errorCode("FEE_MISMATCH")
                    .build();
        }
        
        try {
            // 현재 무료 기간이므로 실제 결제 없이 바로 활성화
            if (feeInfo.getIsFreePromotion()) {
                // 무료 기간 - 즉시 활성화
                connection.activateConnection();
                
                // 채팅방 활성화
                chatService.activateChatRoom(connection);
                
                // 구매자에게 채팅 활성화 알림 발송
                fcmService.sendChatActivationNotification(
                        connection.getBuyer(), connection.getId(), connection.getAuction().getTitle());
                
                connectionServiceRepository.save(connection);
                
                return PaymentResult.builder()
                        .connectionId(request.getConnectionId())
                        .success(true)
                        .status(ConnectionStatus.ACTIVE)
                        .chatRoomActivated(true)
                        .connectedAt(connection.getConnectedAt())
                        .message("연결 서비스가 활성화되었습니다. (무료 프로모션)")
                        .build();
            } else {
                // 유료 기간 - 포인트 결제 처리
                // pointService.deductPoints(sellerId, feeInfo.getFinalFee().longValue(), "연결 서비스 수수료");
                
                connection.activateConnection();
                
                // 채팅방 활성화
                chatService.activateChatRoom(connection);
                
                // 구매자에게 채팅 활성화 알림 발송
                fcmService.sendChatActivationNotification(
                        connection.getBuyer(), connection.getId(), connection.getAuction().getTitle());
                
                connectionServiceRepository.save(connection);
                
                return PaymentResult.builder()
                        .connectionId(request.getConnectionId())
                        .success(true)
                        .status(ConnectionStatus.ACTIVE)
                        .chatRoomActivated(true)
                        .connectedAt(connection.getConnectedAt())
                        .message("연결 서비스 결제가 완료되었습니다.")
                        .build();
            }
            
        } catch (Exception e) {
            return PaymentResult.builder()
                    .connectionId(request.getConnectionId())
                    .success(false)
                    .message("결제 처리 중 오류가 발생했습니다: " + e.getMessage())
                    .errorCode("PAYMENT_ERROR")
                    .build();
        }
    }
}