package com.cherrypick.app.domain.auth.service;

import com.cherrypick.app.domain.auth.entity.SocialAccount;
import com.cherrypick.app.domain.auth.repository.SocialAccountRepository;
import com.cherrypick.app.domain.auth.dto.response.SocialUserInfo;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class OAuthService {

    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // OAuth 설정값들 (환경변수에서 주입)
    @Value("${android_google_client_id}")
    private String googleClientId;
    
    @Value("${GOOGLE_CLIENT_SECRET}")
    private String googleClientSecret;
    
    @Value("${KAKAO_CLIENT_ID}")
    private String kakaoClientId;
    
    @Value("${KAKAO_CLIENT_SECRET}")
    private String kakaoClientSecret;
    
    @Value("${NAVER_CLIENT_ID}")
    private String naverClientId;
    
    @Value("${NAVER_CLIENT_SECRET}")
    private String naverClientSecret;
    
    @Value("${GOOGLE_REDIRECT_URI}")
    private String googleRedirectUri;
    
    @Value("${KAKAO_REDIRECT_URI}")
    private String kakaoRedirectUri;
    
    @Value("${NAVER_REDIRECT_URI}")
    private String naverRedirectUri;

    public OAuthService(SocialAccountRepository socialAccountRepository,
                       UserRepository userRepository,
                       WebClient.Builder webClientBuilder,
                       ObjectMapper objectMapper) {
        this.socialAccountRepository = socialAccountRepository;
        this.userRepository = userRepository;
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public SocialUserInfo getSocialUserInfo(SocialAccount.SocialProvider provider, String code, String redirectUri) {
        try {
            String accessToken = getAccessToken(provider, code, redirectUri);
            return getUserInfoFromProvider(provider, accessToken);
        } catch (Exception e) {
            throw new RuntimeException("소셜 로그인 정보 조회 실패: " + e.getMessage());
        }
    }

    private String getAccessToken(SocialAccount.SocialProvider provider, String code, String redirectUri) throws Exception {
        switch (provider) {
            case GOOGLE:
                return getGoogleAccessToken(code, redirectUri);
            case KAKAO:
                return getKakaoAccessToken(code, redirectUri);
            case NAVER:
                return getNaverAccessToken(code, redirectUri);
            default:
                throw new RuntimeException("지원하지 않는 OAuth 제공자입니다: " + provider);
        }
    }

    private String getGoogleAccessToken(String code, String redirectUri) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("client_id", googleClientId);
        params.put("client_secret", googleClientSecret);
        params.put("code", code);
        params.put("grant_type", "authorization_code");
        params.put("redirect_uri", redirectUri);

        String response = webClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromFormData(convertToFormData(params)))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode jsonNode = objectMapper.readTree(response);
        return jsonNode.get("access_token").asText();
    }

    private String getKakaoAccessToken(String code, String redirectUri) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("client_id", kakaoClientId);
        params.put("client_secret", kakaoClientSecret);
        params.put("code", code);
        params.put("grant_type", "authorization_code");
        params.put("redirect_uri", redirectUri);

        String response = webClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromFormData(convertToFormData(params)))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode jsonNode = objectMapper.readTree(response);
        return jsonNode.get("access_token").asText();
    }

    private String getNaverAccessToken(String code, String redirectUri) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("client_id", naverClientId);
        params.put("client_secret", naverClientSecret);
        params.put("code", code);
        params.put("grant_type", "authorization_code");
        params.put("redirect_uri", redirectUri);

        String response = webClient.post()
                .uri("https://nid.naver.com/oauth2.0/token")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromFormData(convertToFormData(params)))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode jsonNode = objectMapper.readTree(response);
        return jsonNode.get("access_token").asText();
    }

    private SocialUserInfo getUserInfoFromProvider(SocialAccount.SocialProvider provider, String accessToken) throws Exception {
        switch (provider) {
            case GOOGLE:
                return getGoogleUserInfo(accessToken);
            case KAKAO:
                return getKakaoUserInfo(accessToken);
            case NAVER:
                return getNaverUserInfo(accessToken);
            default:
                throw new RuntimeException("지원하지 않는 OAuth 제공자입니다: " + provider);
        }
    }

    private SocialUserInfo getGoogleUserInfo(String accessToken) throws Exception {
        String response = webClient.get()
                .uri("https://www.googleapis.com/oauth2/v2/userinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode userInfo = objectMapper.readTree(response);
        String providerId = userInfo.get("id").asText();
        
        // 기존 사용자 확인
        Optional<SocialAccount> existingSocialAccount = socialAccountRepository
                .findByProviderAndProviderIdAndIsActiveTrue(SocialAccount.SocialProvider.GOOGLE, providerId);
        
        return SocialUserInfo.builder()
                .provider(SocialAccount.SocialProvider.GOOGLE)
                .providerId(providerId)
                .email(userInfo.get("email").asText())
                .name(userInfo.get("name").asText())
                .profileImageUrl(userInfo.has("picture") ? userInfo.get("picture").asText() : null)
                .isExistingUser(existingSocialAccount.isPresent())
                .userId(existingSocialAccount.map(sa -> sa.getUser().getId()).orElse(null))
                .build();
    }

    private SocialUserInfo getKakaoUserInfo(String accessToken) throws Exception {
        String response = webClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode userInfo = objectMapper.readTree(response);
        String providerId = userInfo.get("id").asText();
        JsonNode kakaoAccount = userInfo.get("kakao_account");
        JsonNode profile = kakaoAccount.get("profile");
        
        // 기존 사용자 확인
        Optional<SocialAccount> existingSocialAccount = socialAccountRepository
                .findByProviderAndProviderIdAndIsActiveTrue(SocialAccount.SocialProvider.KAKAO, providerId);

        return SocialUserInfo.builder()
                .provider(SocialAccount.SocialProvider.KAKAO)
                .providerId(providerId)
                .email(kakaoAccount.has("email") ? kakaoAccount.get("email").asText() : null)
                .name(profile.has("nickname") ? profile.get("nickname").asText() : null)
                .profileImageUrl(profile.has("profile_image_url") ? profile.get("profile_image_url").asText() : null)
                .isExistingUser(existingSocialAccount.isPresent())
                .userId(existingSocialAccount.map(sa -> sa.getUser().getId()).orElse(null))
                .build();
    }

    private SocialUserInfo getNaverUserInfo(String accessToken) throws Exception {
        String response = webClient.get()
                .uri("https://openapi.naver.com/v1/nid/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode result = objectMapper.readTree(response);
        JsonNode userInfo = result.get("response");
        String providerId = userInfo.get("id").asText();
        
        // 기존 사용자 확인
        Optional<SocialAccount> existingSocialAccount = socialAccountRepository
                .findByProviderAndProviderIdAndIsActiveTrue(SocialAccount.SocialProvider.NAVER, providerId);

        return SocialUserInfo.builder()
                .provider(SocialAccount.SocialProvider.NAVER)
                .providerId(providerId)
                .email(userInfo.has("email") ? userInfo.get("email").asText() : null)
                .name(userInfo.has("name") ? userInfo.get("name").asText() : null)
                .profileImageUrl(userInfo.has("profile_image") ? userInfo.get("profile_image").asText() : null)
                .isExistingUser(existingSocialAccount.isPresent())
                .userId(existingSocialAccount.map(sa -> sa.getUser().getId()).orElse(null))
                .build();
    }

    @Transactional
    public SocialAccount createSocialAccount(User user, SocialUserInfo socialUserInfo) {
        SocialAccount socialAccount = SocialAccount.builder()
                .user(user)
                .provider(socialUserInfo.getProvider())
                .providerId(socialUserInfo.getProviderId())
                .email(socialUserInfo.getEmail())
                .name(socialUserInfo.getName())
                .profileImageUrl(socialUserInfo.getProfileImageUrl())
                .isActive(true)
                .build();

        return socialAccountRepository.save(socialAccount);
    }

    @Transactional(readOnly = true)
    public Optional<User> findUserBySocialAccount(SocialAccount.SocialProvider provider, String providerId) {
        return socialAccountRepository.findByProviderAndProviderIdAndIsActiveTrue(provider, providerId)
                .map(SocialAccount::getUser);
    }

    // Form data 변환 헬퍼 메서드
    private org.springframework.util.MultiValueMap<String, String> convertToFormData(Map<String, String> params) {
        org.springframework.util.MultiValueMap<String, String> formData = new org.springframework.util.LinkedMultiValueMap<>();
        params.forEach(formData::add);
        return formData;
    }
}