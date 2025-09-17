package com.cherrypick.app.domain.auth.dto.request;

import com.cherrypick.app.domain.auth.entity.SocialAccount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialSignupRequest {
    
    @NotNull(message = "소셜 로그인 제공자는 필수입니다.")
    private SocialAccount.SocialProvider provider;
    
    @NotBlank(message = "OAuth 인증 코드는 필수입니다.")
    private String code;
    
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해주세요.")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9_-]+$", message = "닉네임은 한글, 영문, 숫자, 언더스코어, 하이픈만 사용 가능합니다.")
    private String nickname;
    
    // 리다이렉트 URI (클라이언트에서 전달)
    private String redirectUri;
    
    // 상태 값 (CSRF 방지용)
    private String state;
    
    // 약관 동의
    @NotNull(message = "이용약관 동의는 필수입니다.")
    private Boolean agreeToTerms;
    
    @NotNull(message = "개인정보처리방침 동의는 필수입니다.")
    private Boolean agreeToPrivacy;
}