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
    
    // Authorization Code 방식
    private String code;
    
    // Access Token 방식 (모바일 앱용)
    private String accessToken;
    
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해주세요.")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9_-]+$", message = "닉네임은 한글, 영문, 숫자, 언더스코어, 하이픈만 사용 가능합니다.")
    private String nickname;

    // 거래 지역 (위치기반 플랫폼 필수)
    @NotBlank(message = "거래 지역은 필수입니다.")
    @Size(max = 200, message = "거래 지역은 200자 이하로 입력해주세요.")
    private String address;

    // 자기소개 (선택)
    @Size(max = 500, message = "자기소개는 500자 이하로 입력해주세요.")
    private String bio;

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