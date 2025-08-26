package com.cherrypick.app.domain.point.service;

import com.cherrypick.app.domain.point.dto.request.ChargePointRequest;
import com.cherrypick.app.domain.point.dto.request.WithdrawPointRequest;
import com.cherrypick.app.domain.point.dto.response.PointTransactionResponse;
import com.cherrypick.app.domain.point.entity.PointTransaction;
import com.cherrypick.app.domain.point.enums.PointTransactionStatus;
import com.cherrypick.app.domain.point.enums.PointTransactionType;
import com.cherrypick.app.domain.point.repository.PointTransactionRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.entity.UserAccount;
import com.cherrypick.app.domain.user.repository.UserAccountRepository;
import com.cherrypick.app.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;

/**
 * PointService 핵심 비즈니스 로직 테스트
 * TDD 방식: 실제 비즈니스 규칙 검증에 집중
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("포인트 서비스 핵심 비즈니스 로직")
class PointServiceTest {
    
    // 테스트 상수 정의 - Magic Number 제거로 가독성 향상
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long ACCOUNT_ID = 1L;
    private static final Long OTHER_ACCOUNT_ID = 2L;
    
    private static final Long INITIAL_BALANCE = 50000L;
    private static final Long CHARGE_AMOUNT = 30000L;
    private static final Long WITHDRAW_AMOUNT = 20000L;
    private static final Long LARGE_CHARGE_AMOUNT = 50000L;
    private static final Long LARGE_WITHDRAW_AMOUNT = 500000L;
    
    private static final Long EXPECTED_BALANCE_AFTER_CHARGE = INITIAL_BALANCE + CHARGE_AMOUNT; // 80000L
    private static final Long EXPECTED_BALANCE_AFTER_WITHDRAW = INITIAL_BALANCE - WITHDRAW_AMOUNT; // 30000L
    private static final Long RICH_USER_BALANCE = 1000000L;
    private static final Long POOR_USER_BALANCE = 10000L;

    @Mock
    private PointTransactionRepository pointTransactionRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserAccountRepository userAccountRepository;
    
    @InjectMocks
    private PointService pointService;
    
    private User validUser;
    private UserAccount validAccount;
    private ChargePointRequest validChargeRequest;
    private WithdrawPointRequest validWithdrawRequest;
    
    @BeforeEach
    void setUp() {
        validUser = User.builder()
                .id(USER_ID)
                .nickname("테스트사용자")
                .email("user@test.com")
                .pointBalance(INITIAL_BALANCE)
                .build();
                
        validAccount = UserAccount.builder()
                .id(ACCOUNT_ID)
                .user(validUser)
                .bankName("국민은행")
                .accountNumber("123-456-789")
                .accountHolder("테스트사용자")
                .isVerified(true)
                .build();
                
        validChargeRequest = new ChargePointRequest();
        validChargeRequest.setAmount(CHARGE_AMOUNT);
        validChargeRequest.setAccountId(ACCOUNT_ID);
        
        validWithdrawRequest = new WithdrawPointRequest();
        validWithdrawRequest.setAmount(WITHDRAW_AMOUNT);
        validWithdrawRequest.setAccountId(ACCOUNT_ID);
    }
    
    @Nested
    @DisplayName("포인트 충전 - chargePoints()")
    class ChargePointsTest {
        
        @Test
        @DisplayName("성공: 유효한 계좌로 포인트 충전")
        void chargePoints_Success() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(validUser));
            given(userAccountRepository.findById(ACCOUNT_ID)).willReturn(Optional.of(validAccount));
            
            PointTransaction savedTransaction = createTestTransaction(USER_ID, validUser);
            given(pointTransactionRepository.save(any(PointTransaction.class))).willReturn(savedTransaction);
            given(userRepository.save(any(User.class))).willReturn(validUser);
            
            // when
            PointTransactionResponse result = pointService.chargePoints(USER_ID, validChargeRequest);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(CHARGE_AMOUNT));
            assertThat(result.getType()).isEqualTo(PointTransactionType.CHARGE);
            assertThat(result.getStatus()).isEqualTo(PointTransactionStatus.COMPLETED);
            
            // ArgumentCaptor를 활용한 명확한 검증 - 디버깅 개선
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getPointBalance()).isEqualTo(EXPECTED_BALANCE_AFTER_CHARGE);
        }
        
        @Test
        @DisplayName("실패: 존재하지 않는 사용자")
        void chargePoints_UserNotFound() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());
            
            // when & then
            assertThatThrownBy(() -> pointService.chargePoints(999L, validChargeRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
                    
            // 포인트 거래 내역 저장 안 함
            verify(pointTransactionRepository, never()).save(any(PointTransaction.class));
        }
        
        @Test
        @DisplayName("실패: 존재하지 않는 계좌")
        void chargePoints_AccountNotFound() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(validUser));
            given(userAccountRepository.findById(999L)).willReturn(Optional.empty());
            
            ChargePointRequest invalidRequest = new ChargePointRequest();
            invalidRequest.setAmount(30000L);
            invalidRequest.setAccountId(999L);
            
            // when & then
            assertThatThrownBy(() -> pointService.chargePoints(1L, invalidRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("계좌를 찾을 수 없습니다");
                    
            verify(pointTransactionRepository, never()).save(any(PointTransaction.class));
        }
        
        @Test
        @DisplayName("보안: 다른 사용자 계좌 사용 방지")
        void chargePoints_PreventOtherUserAccount() {
            // given
            User otherUser = User.builder()
                    .id(OTHER_USER_ID)
                    .nickname("다른사용자")
                    .build();
                    
            UserAccount otherUserAccount = UserAccount.builder()
                    .id(OTHER_ACCOUNT_ID)
                    .user(otherUser) // 다른 사용자의 계좌
                    .bankName("신한은행")
                    .build();
            
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(validUser));
            given(userAccountRepository.findById(OTHER_ACCOUNT_ID)).willReturn(Optional.of(otherUserAccount));
            
            ChargePointRequest maliciousRequest = new ChargePointRequest();
            maliciousRequest.setAmount(CHARGE_AMOUNT);
            maliciousRequest.setAccountId(OTHER_ACCOUNT_ID); // 다른 사용자 계좌 ID
            
            // when & then
            assertThatThrownBy(() -> pointService.chargePoints(USER_ID, maliciousRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("본인의 계좌만 사용할 수 있습니다");
                    
            // 보안 위반으로 거래 처리 안 함
            verify(pointTransactionRepository, never()).save(any(PointTransaction.class));
            verify(userRepository, never()).save(any(User.class));
        }
        
        @Test
        @DisplayName("비즈니스 규칙: 잔액 정확한 계산")
        void chargePoints_CorrectBalanceCalculation() {
            // given - 기존 잔액이 100만원인 사용자
            User richUser = User.builder()
                    .id(USER_ID)
                    .pointBalance(RICH_USER_BALANCE)
                    .build();
                    
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(richUser));
            given(userAccountRepository.findById(ACCOUNT_ID)).willReturn(Optional.of(validAccount));
            
            PointTransaction savedTransaction = createTestTransaction(USER_ID, richUser);
            given(pointTransactionRepository.save(any(PointTransaction.class))).willReturn(savedTransaction);
            given(userRepository.save(any(User.class))).willReturn(richUser);
            
            ChargePointRequest largeRequest = new ChargePointRequest();
            largeRequest.setAmount(LARGE_CHARGE_AMOUNT);
            largeRequest.setAccountId(ACCOUNT_ID);
            
            // when
            pointService.chargePoints(USER_ID, largeRequest);
            
            // then - ArgumentCaptor로 명확한 잔액 검증 (1,000,000 + 50,000)
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            Long expectedBalance = RICH_USER_BALANCE + LARGE_CHARGE_AMOUNT;
            assertThat(savedUser.getPointBalance()).isEqualTo(expectedBalance);
        }
    }
    
    @Nested
    @DisplayName("포인트 출금 - withdrawPoints()")
    class WithdrawPointsTest {
        
        @Test
        @DisplayName("성공: 충분한 잔액으로 포인트 출금")
        void withdrawPoints_Success() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(validUser));
            given(userAccountRepository.findById(ACCOUNT_ID)).willReturn(Optional.of(validAccount));
            
            PointTransaction savedTransaction = createWithdrawTransaction(USER_ID, validUser);
            given(pointTransactionRepository.save(any(PointTransaction.class))).willReturn(savedTransaction);
            given(userRepository.save(any(User.class))).willReturn(validUser);
            
            // when
            PointTransactionResponse result = pointService.withdrawPoints(USER_ID, validWithdrawRequest);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(-WITHDRAW_AMOUNT)); // 음수로 표시
            assertThat(result.getType()).isEqualTo(PointTransactionType.WITHDRAW);
            assertThat(result.getStatus()).isEqualTo(PointTransactionStatus.COMPLETED);
            
            // ArgumentCaptor를 활용한 명확한 검증 - 디버깅 개선
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getPointBalance()).isEqualTo(EXPECTED_BALANCE_AFTER_WITHDRAW);
        }
        
        @Test
        @DisplayName("실패: 잔액 부족으로 출금 불가")
        void withdrawPoints_InsufficientBalance() {
            // given - 잔액보다 많은 금액 출금 시도
            User poorUser = User.builder()
                    .id(USER_ID)
                    .pointBalance(POOR_USER_BALANCE) // 20,000원 출금하려는데 잔액 10,000원
                    .build();
                    
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(poorUser));
            
            // when & then
            assertThatThrownBy(() -> pointService.withdrawPoints(USER_ID, validWithdrawRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("출금 가능한 포인트가 부족합니다");
                    
            // 출금 거래 내역 저장 안 함
            verify(pointTransactionRepository, never()).save(any(PointTransaction.class));
            verify(userRepository, never()).save(any(User.class));
        }
        
        @Test
        @DisplayName("보안: 다른 사용자 계좌로 출금 방지")
        void withdrawPoints_PreventOtherUserAccount() {
            // given
            User otherUser = User.builder()
                    .id(2L)
                    .nickname("다른사용자")
                    .build();
                    
            UserAccount otherUserAccount = UserAccount.builder()
                    .id(2L)
                    .user(otherUser) // 다른 사용자의 계좌
                    .bankName("신한은행")
                    .build();
            
            given(userRepository.findById(1L)).willReturn(Optional.of(validUser));
            given(userAccountRepository.findById(2L)).willReturn(Optional.of(otherUserAccount));
            
            WithdrawPointRequest maliciousRequest = new WithdrawPointRequest();
            maliciousRequest.setAmount(20000L);
            maliciousRequest.setAccountId(2L); // 다른 사용자 계좌 ID
            
            // when & then
            assertThatThrownBy(() -> pointService.withdrawPoints(1L, maliciousRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("본인의 계좌로만 출금할 수 있습니다");
                    
            // 보안 위반으로 거래 처리 안 함
            verify(pointTransactionRepository, never()).save(any(PointTransaction.class));
            verify(userRepository, never()).save(any(User.class));
        }
        
        @Test
        @DisplayName("비즈니스 규칙: 정확한 잔액 계산")
        void withdrawPoints_CorrectBalanceCalculation() {
            // given - 100만원 잔액에서 50만원 출금
            User richUser = User.builder()
                    .id(1L)
                    .pointBalance(1000000L)
                    .build();
                    
            given(userRepository.findById(1L)).willReturn(Optional.of(richUser));
            given(userAccountRepository.findById(1L)).willReturn(Optional.of(validAccount));
            
            PointTransaction savedTransaction = createWithdrawTransaction(1L, richUser);
            given(pointTransactionRepository.save(any(PointTransaction.class))).willReturn(savedTransaction);
            given(userRepository.save(any(User.class))).willReturn(richUser);
            
            WithdrawPointRequest largeRequest = new WithdrawPointRequest();
            largeRequest.setAmount(500000L);
            largeRequest.setAccountId(1L);
            
            // when
            pointService.withdrawPoints(1L, largeRequest);
            
            // then - 비즈니스 로직 검증: 정확한 잔액 계산 (1,000,000 - 500,000)
            verify(userRepository).save(argThat(user -> 
                user.getPointBalance().equals(500000L)
            ));
        }
    }
    
    // 테스트 헬퍼 메서드 - 상수 활용으로 가독성 향상
    private PointTransaction createTestTransaction(Long id, User user) {
        PointTransaction transaction = PointTransaction.builder()
                .user(user)
                .type(PointTransactionType.CHARGE)
                .amount(BigDecimal.valueOf(CHARGE_AMOUNT))
                .balanceAfter(BigDecimal.valueOf(EXPECTED_BALANCE_AFTER_CHARGE))
                .relatedType("ACCOUNT")
                .relatedId(ACCOUNT_ID)
                .description(String.format("국민은행에서 %,d원 충전", CHARGE_AMOUNT))
                .status(PointTransactionStatus.COMPLETED)
                .build();
                
        // Spring ReflectionTestUtils로 안전한 ID 설정
        ReflectionTestUtils.setField(transaction, "id", id);
        return transaction;
    }
    
    // 출금 테스트용 헬퍼 메서드 - 상수 활용으로 가독성 향상
    private PointTransaction createWithdrawTransaction(Long id, User user) {
        PointTransaction transaction = PointTransaction.builder()
                .user(user)
                .type(PointTransactionType.WITHDRAW)
                .amount(BigDecimal.valueOf(-WITHDRAW_AMOUNT)) // 출금은 음수로 표시
                .balanceAfter(BigDecimal.valueOf(EXPECTED_BALANCE_AFTER_WITHDRAW))
                .relatedType("ACCOUNT")
                .relatedId(ACCOUNT_ID)
                .description(String.format("국민은행으로 %,d원 출금", WITHDRAW_AMOUNT))
                .status(PointTransactionStatus.COMPLETED)
                .build();
                
        // Spring ReflectionTestUtils로 안전한 ID 설정
        ReflectionTestUtils.setField(transaction, "id", id);
        return transaction;
    }
}