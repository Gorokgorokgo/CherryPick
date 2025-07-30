package com.cherrypick.app.domain.point.service;

import com.cherrypick.app.domain.point.dto.*;
import com.cherrypick.app.domain.point.repository.PointTransactionRepository;
import com.cherrypick.app.domain.point.entity.PointTransaction;
import com.cherrypick.app.domain.point.enums.PointTransactionType;
import com.cherrypick.app.domain.point.enums.PointTransactionStatus;
import com.cherrypick.app.domain.user.User;
import com.cherrypick.app.domain.user.UserAccountRepository;
import com.cherrypick.app.domain.user.UserRepository;
import com.cherrypick.app.domain.user.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {
    
    private final PointTransactionRepository pointTransactionRepository;
    private final UserRepository userRepository;
    private final UserAccountRepository userAccountRepository;
    
    /**
     * 포인트 충전 (당근페이 방식)
     * 
     * 비즈니스 로직:
     * 1. 충전 금액 유효성 검증 (1,000원 단위, 최대 1,000,000원)
     * 2. 사용자 존재 여부 확인
     * 3. 본인 계좌 소유권 확인 (보안)
     * 4. 계좌 인증 상태 확인 (인증된 계좌만 사용 가능)
     * 5. 외부 결제 API 연동하여 실제 결제 처리
     * 6. 결제 성공 시 사용자 포인트 잔액 증가
     * 7. 포인트 거래 내역 저장 (추적 가능하도록)
     * 
     * @param userId 충전할 사용자 ID
     * @param request 충전 요청 정보 (금액, 계좌 ID)
     * @return 충전 거래 내역
     * @throws IllegalArgumentException 유효하지 않은 요청 (사용자, 계좌, 금액)
     * @throws RuntimeException 결제 처리 실패
     */
    @Transactional
    public PointTransactionResponse chargePoints(Long userId, ChargePointRequest request) {
        // 충전 금액 유효성 검증 (1,000원 단위, 최대 한도 등)
        request.validateAmount();
        
        // 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        // 계좌 존재 및 소유권 확인
        UserAccount account = userAccountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));
        
        // 본인 계좌인지 확인 (보안 - 다른 사람 계좌 사용 방지)
        if (!account.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 계좌만 사용할 수 있습니다.");
        }
        
        // 계좌 인증 상태 확인 (1원 인증 등 완료된 계좌만 사용)
        if (!account.getIsVerified()) {
            throw new IllegalArgumentException("인증된 계좌만 사용할 수 있습니다.");
        }
        
        // 외부 결제 API 연동 (토스페이, 카카오페이, 네이버페이 등)
        boolean paymentSuccess = processPayment(account, request.getAmount());
        
        if (!paymentSuccess) {
            throw new RuntimeException("결제 처리에 실패했습니다.");
        }
        
        // 포인트 충전 (사용자 잔액 증가)
        Long currentBalance = user.getPointBalance();
        Long newBalance = currentBalance + request.getAmount();
        
        user.setPointBalance(newBalance);
        userRepository.save(user);
        
        // 포인트 거래 내역 저장 (감사 추적용)
        PointTransaction transaction = PointTransaction.builder()
                .user(user)
                .type(PointTransactionType.CHARGE)
                .amount(BigDecimal.valueOf(request.getAmount()))
                .balanceAfter(BigDecimal.valueOf(newBalance))
                .relatedType("ACCOUNT") // 계좌 연관 거래
                .relatedId(account.getId())
                .description(String.format("%s에서 %,d원 충전", account.getBankName(), request.getAmount()))
                .status(PointTransactionStatus.COMPLETED)
                .build();
        
        PointTransaction savedTransaction = pointTransactionRepository.save(transaction);
        return PointTransactionResponse.from(savedTransaction);
    }
    
    @Transactional
    public PointTransactionResponse withdrawPoints(Long userId, WithdrawPointRequest request) {
        request.validateAmount();
        
        // 사용자 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        // 잔액 확인
        if (user.getPointBalance() < request.getAmount()) {
            throw new IllegalArgumentException("출금 가능한 포인트가 부족합니다.");
        }
        
        // 계좌 확인 (본인 계좌인지)
        UserAccount account = userAccountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));
        
        if (!account.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 계좌로만 출금할 수 있습니다.");
        }
        
        if (!account.getIsVerified()) {
            throw new IllegalArgumentException("인증된 계좌로만 출금할 수 있습니다.");
        }
        
        // TODO: 실제 송금 API 연동
        boolean transferSuccess = processTransfer(account, request.getAmount());
        
        if (!transferSuccess) {
            throw new RuntimeException("송금 처리에 실패했습니다.");
        }
        
        // 포인트 차감
        Long currentBalance = user.getPointBalance();
        Long newBalance = currentBalance - request.getAmount();
        
        user.setPointBalance(newBalance);
        userRepository.save(user);
        
        // 포인트 거래 내역 저장
        PointTransaction transaction = PointTransaction.builder()
                .user(user)
                .type(PointTransactionType.WITHDRAW)
                .amount(BigDecimal.valueOf(-request.getAmount()))
                .balanceAfter(BigDecimal.valueOf(newBalance))
                .relatedType("ACCOUNT")
                .relatedId(account.getId())
                .description(String.format("%s로 %,d원 출금", account.getBankName(), request.getAmount()))
                .status(PointTransactionStatus.COMPLETED)
                .build();
        
        PointTransaction savedTransaction = pointTransactionRepository.save(transaction);
        return PointTransactionResponse.from(savedTransaction);
    }
    
    public PointBalanceResponse getPointBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        // TODO: 잠긴 포인트 금액 계산 (입찰 중인 포인트)
        Long lockedAmount = 0L; // calculateLockedAmount(userId);
        
        return PointBalanceResponse.of(userId, user.getPointBalance(), lockedAmount);
    }
    
    public Page<PointTransactionResponse> getPointTransactionHistory(Long userId, Pageable pageable) {
        Page<PointTransaction> transactions = pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return transactions.map(PointTransactionResponse::from);
    }
    
    // 내부 메서드들
    private boolean processPayment(UserAccount account, Long amount) {
        // TODO: 실제 결제 API 연동
        // 현재는 항상 성공으로 처리
        try {
            Thread.sleep(1000); // 결제 처리 시뮬레이션
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    private boolean processTransfer(UserAccount account, Long amount) {
        // TODO: 실제 송금 API 연동
        // 현재는 항상 성공으로 처리
        try {
            Thread.sleep(1000); // 송금 처리 시뮬레이션
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}