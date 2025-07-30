package com.cherrypick.app.domain.user;

import com.cherrypick.app.domain.user.dto.AccountResponse;
import com.cherrypick.app.domain.user.dto.AddAccountRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAccountService {
    
    private final UserAccountRepository userAccountRepository;
    private final UserRepository userRepository;
    
    private static final int MAX_ACCOUNTS_PER_USER = 5;
    
    /**
     * 사용자 계좌 등록
     * 
     * 비즈니스 로직:
     * 1. 사용자 존재 여부 확인
     * 2. 계좌 등록 개수 제한 검증 (최대 5개)
     * 3. 중복 계좌 등록 방지
     * 4. 첫 번째 계좌는 자동으로 기본 계좌로 설정
     * 5. 기본 계좌 설정 시 기존 기본 계좌 해제
     * 6. 계좌번호 암호화 저장 (보안)
     * 7. 초기 상태는 미인증으로 설정 (추후 계좌 인증 필요)
     * 
     * @param userId 사용자 ID
     * @param request 계좌 등록 요청 정보
     * @return 등록된 계좌 정보 (마스킹된 형태)
     * @throws IllegalArgumentException 사용자 없음, 계좌 개수 초과, 중복 계좌
     */
    @Transactional
    public AccountResponse addAccount(Long userId, AddAccountRequest request) {
        // 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        // 계좌 개수 제한 확인 (최대 5개까지 등록 가능)
        long accountCount = userAccountRepository.countByUserId(userId);
        if (accountCount >= MAX_ACCOUNTS_PER_USER) {
            throw new IllegalArgumentException("계좌는 최대 " + MAX_ACCOUNTS_PER_USER + "개까지 등록 가능합니다.");
        }
        
        // 중복 계좌 확인 (동일한 계좌번호 등록 방지)
        if (userAccountRepository.existsByUserIdAndAccountNumber(userId, request.getAccountNumber())) {
            throw new IllegalArgumentException("이미 등록된 계좌입니다.");
        }
        
        // 첫 번째 계좌인 경우 자동으로 기본 계좌로 설정
        boolean isPrimary = request.getIsPrimary() || accountCount == 0;
        
        // 기본 계좌로 설정하는 경우 기존 기본 계좌 해제
        if (isPrimary) {
            resetPrimaryAccount(userId);
        }
        
        // 계좌 정보 암호화 (보안을 위한 계좌번호 암호화)
        String encryptedAccountNumber = encryptAccountNumber(request.getAccountNumber());
        
        // 새 계좌 엔티티 생성
        UserAccount account = UserAccount.builder()
                .user(user)
                .bankCode(request.getBankCode())
                .bankName(request.getBankName())
                .accountNumber(encryptedAccountNumber)
                .accountHolder(request.getAccountHolder())
                .isPrimary(isPrimary)
                .isVerified(false) // 초기에는 미인증 상태 (추후 1원 인증 등 필요)
                .build();
        
        // 계좌 저장 및 마스킹된 정보 반환
        UserAccount savedAccount = userAccountRepository.save(account);
        return AccountResponse.from(savedAccount);
    }
    
    public List<AccountResponse> getUserAccounts(Long userId) {
        List<UserAccount> accounts = userAccountRepository.findByUserIdOrderByIsPrimaryDescCreatedAtDesc(userId);
        return accounts.stream()
                .map(AccountResponse::from)
                .collect(Collectors.toList());
    }
    
    public AccountResponse getPrimaryAccount(Long userId) {
        UserAccount account = userAccountRepository.findByUserIdAndIsPrimaryTrue(userId)
                .orElseThrow(() -> new IllegalArgumentException("등록된 기본 계좌가 없습니다."));
        return AccountResponse.from(account);
    }
    
    @Transactional
    public void setPrimaryAccount(Long userId, Long accountId) {
        UserAccount account = userAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));
        
        if (!account.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 계좌만 설정할 수 있습니다.");
        }
        
        // 기존 기본 계좌 해제
        resetPrimaryAccount(userId);
        
        // 새로운 기본 계좌 설정
        account.setIsPrimary(true);
        userAccountRepository.save(account);
    }
    
    @Transactional
    public void deleteAccount(Long userId, Long accountId) {
        UserAccount account = userAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));
        
        if (!account.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 계좌만 삭제할 수 있습니다.");
        }
        
        if (account.getIsPrimary()) {
            throw new IllegalArgumentException("기본 계좌는 삭제할 수 없습니다. 다른 계좌를 기본 계좌로 설정 후 삭제해주세요.");
        }
        
        userAccountRepository.delete(account);
    }
    
    private void resetPrimaryAccount(Long userId) {
        userAccountRepository.findByUserIdAndIsPrimaryTrue(userId)
                .ifPresent(account -> {
                    account.setIsPrimary(false);
                    userAccountRepository.save(account);
                });
    }
    
    private String encryptAccountNumber(String accountNumber) {
        // TODO: 실제 환경에서는 AES 암호화 적용
        // 현재는 간단히 Base64 인코딩만 적용
        return java.util.Base64.getEncoder().encodeToString(accountNumber.getBytes());
    }
    
    private String decryptAccountNumber(String encryptedAccountNumber) {
        // TODO: 실제 환경에서는 AES 복호화 적용
        return new String(java.util.Base64.getDecoder().decode(encryptedAccountNumber));
    }
}