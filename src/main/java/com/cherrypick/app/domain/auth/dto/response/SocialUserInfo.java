package com.cherrypick.app.domain.auth.dto.response;

import com.cherrypick.app.domain.auth.entity.SocialAccount;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialUserInfo {
    
    private SocialAccount.SocialProvider provider;
    private String providerId;
    private String email;
    private String name;
    private String profileImageUrl;
    
    // 이미 가입된 사용자인지 여부
    private boolean isExistingUser;
    
    // 가입된 사용자라면 해당 사용자의 ID
    private Long userId;
}