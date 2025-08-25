package com.cherrypick.app.domain.auth.dto.request;

import com.cherrypick.app.domain.auth.entity.SocialAccount;
import jakarta.validation.constraints.NotBlank;
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
    
    @NotBlank(message = "OAuth 인증 코드는 필수입니다.")
    private String code;
    
    // 리다이렉트 URI (클라이언트에서 전달)
    private String redirectUri;
    
    // 상태 값 (CSRF 방지용)
    private String state;
}