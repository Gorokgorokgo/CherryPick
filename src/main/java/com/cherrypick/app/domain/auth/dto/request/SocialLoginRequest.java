package com.cherrypick.app.domain.auth.dto.request;

import com.cherrypick.app.domain.auth.entity.SocialAccount;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialLoginRequest {
    
    @NotNull(message = "소셜 로그인 제공자는 필수입니다.")
    private SocialAccount.SocialProvider provider;
    
    // Authorization Code 방식
    private String code;
    
    // Access Token 방식 (모바일 앱용)
    private String accessToken;
    
    // 리다이렉트 URI (클라이언트에서 전달)
    private String redirectUri;
    
    // 상태 값 (CSRF 방지용)
    private String state;
}