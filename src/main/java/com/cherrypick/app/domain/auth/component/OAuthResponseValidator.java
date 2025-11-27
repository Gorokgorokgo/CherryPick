package com.cherrypick.app.domain.auth.component;

import com.cherrypick.app.domain.auth.exception.OAuthTokenException;
import com.cherrypick.app.domain.auth.exception.OAuthUserInfoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
@Slf4j
public class OAuthResponseValidator {

    public void validateTokenResponse(String provider, ResponseEntity<Map<String, Object>> response) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new OAuthTokenException(provider, 
                String.format("%s 토큰 요청 실패: %s", provider, response.getStatusCode()));
        }

        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new OAuthTokenException(provider, 
                String.format("%s 토큰 응답이 비어있습니다", provider));
        }

        if (body.containsKey("error")) {
            String error = (String) body.get("error");
            String errorDescription = (String) body.get("error_description");
            throw new OAuthTokenException(provider,
                String.format("%s 토큰 오류: %s - %s", provider, error, errorDescription));
        }

        String accessToken = (String) body.get("access_token");
        if (!StringUtils.hasText(accessToken)) {
            throw new OAuthTokenException(provider, 
                String.format("%s access_token이 없습니다", provider));
        }

        log.debug("{} 토큰 응답 검증 완료", provider);
    }

    public void validateUserInfoResponse(String provider, ResponseEntity<Map<String, Object>> response) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new OAuthUserInfoException(provider,
                String.format("%s 사용자 정보 요청 실패: %s", provider, response.getStatusCode()));
        }

        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new OAuthUserInfoException(provider,
                String.format("%s 사용자 정보 응답이 비어있습니다", provider));
        }

        if (body.containsKey("error")) {
            String error = (String) body.get("error");
            throw new OAuthUserInfoException(provider,
                String.format("%s 사용자 정보 오류: %s", provider, error));
        }

        // 공통 필수 필드 검증
        validateRequiredUserFields(provider, body);

        log.debug("{} 사용자 정보 응답 검증 완료", provider);
    }

    private void validateRequiredUserFields(String provider, Map<String, Object> userInfo) {
        switch (provider.toUpperCase()) {
            case "GOOGLE":
                validateGoogleUserInfo(userInfo);
                break;
            case "KAKAO":
                validateKakaoUserInfo(userInfo);
                break;
            case "NAVER":
                validateNaverUserInfo(userInfo);
                break;
            default:
                log.warn("알 수 없는 OAuth 프로바이더: {}", provider);
        }
    }

    private void validateGoogleUserInfo(Map<String, Object> userInfo) {
        // Google API 버전에 따라 id 또는 sub 필드 사용
        String sub = (String) userInfo.get("sub");
        String id = (String) userInfo.get("id");
        
        if (!StringUtils.hasText(sub) && !StringUtils.hasText(id)) {
            throw new OAuthUserInfoException("GOOGLE", "Google sub/id(사용자 ID)가 없습니다");
        }
        if (!StringUtils.hasText((String) userInfo.get("email"))) {
            throw new OAuthUserInfoException("GOOGLE", "Google 이메일이 없습니다");
        }
    }

    private void validateKakaoUserInfo(Map<String, Object> userInfo) {
        if (userInfo.get("id") == null) {
            throw new OAuthUserInfoException("KAKAO", "Kakao 사용자 ID가 없습니다");
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");
        if (kakaoAccount == null) {
            throw new OAuthUserInfoException("KAKAO", "Kakao 계정 정보가 없습니다");
        }
    }

    private void validateNaverUserInfo(Map<String, Object> userInfo) {
        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) userInfo.get("response");
        if (response == null) {
            throw new OAuthUserInfoException("NAVER", "Naver 응답 정보가 없습니다");
        }
        
        if (!StringUtils.hasText((String) response.get("id"))) {
            throw new OAuthUserInfoException("NAVER", "Naver 사용자 ID가 없습니다");
        }
    }
}