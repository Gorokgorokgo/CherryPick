package com.cherrypick.app.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class OAuthConfig {

    // Google OAuth 설정
    @Value("${android_google_client_id}")
    private String androidGoogleClientId;
    
    @Value("${GOOGLE_CLIENT_ID}")
    private String googleClientId;
    
    @Value("${GOOGLE_CLIENT_SECRET}")
    private String googleClientSecret;
    
    @Value("${GOOGLE_REDIRECT_URI}")
    private String googleRedirectUri;

    // Kakao OAuth 설정
    @Value("${KAKAO_CLIENT_ID}")
    private String kakaoClientId;
    
    @Value("${KAKAO_CLIENT_SECRET}")
    private String kakaoClientSecret;
    
    @Value("${KAKAO_REDIRECT_URI}")
    private String kakaoRedirectUri;

    // Naver OAuth 설정
    @Value("${NAVER_CLIENT_ID}")
    private String naverClientId;
    
    @Value("${NAVER_CLIENT_SECRET}")
    private String naverClientSecret;
    
    @Value("${NAVER_REDIRECT_URI}")
    private String naverRedirectUri;

    // OAuth URL 설정
    public static class Urls {
        public static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
        public static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
        
        public static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
        public static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
        
        public static final String NAVER_TOKEN_URL = "https://nid.naver.com/oauth2.0/token";
        public static final String NAVER_USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";
    }
}