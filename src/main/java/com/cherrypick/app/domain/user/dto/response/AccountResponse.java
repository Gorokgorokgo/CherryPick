package com.cherrypick.app.domain.user.dto.response;

import com.cherrypick.app.domain.user.entity.UserAccount;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AccountResponse {
    
    private Long id;
    private String bankCode;
    private String bankName;
    private String maskedAccountNumber; // 마스킹된 계좌번호
    private String accountHolder;
    private Boolean isVerified;
    private Boolean isPrimary;
    private LocalDateTime createdAt;
    
    public static AccountResponse from(UserAccount account) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setBankCode(account.getBankCode());
        response.setBankName(account.getBankName());
        response.setMaskedAccountNumber(maskAccountNumber(account.getAccountNumber()));
        response.setAccountHolder(account.getAccountHolder());
        response.setIsVerified(account.getIsVerified());
        response.setIsPrimary(account.getIsPrimary());
        response.setCreatedAt(account.getCreatedAt());
        return response;
    }
    
    private static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return accountNumber;
        }
        
        int length = accountNumber.length();
        StringBuilder masked = new StringBuilder();
        
        // 앞 2자리 표시
        masked.append(accountNumber.substring(0, 2));
        
        // 중간을 *로 마스킹
        for (int i = 2; i < length - 2; i++) {
            masked.append("*");
        }
        
        // 뒤 2자리 표시
        masked.append(accountNumber.substring(length - 2));
        
        return masked.toString();
    }
}