package com.cherrypick.app.domain.auth.service;

import com.cherrypick.app.config.OAuthConfig;
import com.cherrypick.app.domain.auth.entity.SocialAccount;
import com.cherrypick.app.domain.auth.exception.InvalidStateException;
import com.cherrypick.app.domain.auth.exception.OAuthException;
import com.cherrypick.app.domain.auth.repository.SocialAccountRepository;
import com.cherrypick.app.domain.auth.dto.response.SocialUserInfo;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class OAuthService {

    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final OAuthClientService oAuthClientService;
    private final StateService stateService;
    private final OAuthConfig oAuthConfig;

    public String generateOAuthState() {
        return stateService.generateState();
    }

    public SocialUserInfo getSocialUserInfo(SocialAccount.SocialProvider provider, String code, String state, String redirectUri) {
        try {
            // State 검증
            if (!stateService.validateAndRemoveState(state)) {
                throw new InvalidStateException(provider.name(), state);
            }

            String accessToken = getAccessToken(provider, code, redirectUri);
            return getUserInfoFromProvider(provider, accessToken);
        } catch (OAuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("소셜 로그인 정보 조회 실패 - provider: {}, error: {}", provider, e.getMessage());
            throw new OAuthException(provider.name(), "USER_INFO_ERROR", "소셜 로그인 정보 조회 실패: " + e.getMessage(), e);
        }
    }

    // 테스트/하위호환용 오버로드: redirectUri 미지정시 서버 설정 기본값 사용
    public SocialUserInfo getSocialUserInfo(SocialAccount.SocialProvider provider, String code, String state) {
        String defaultRedirectUri = switch (provider) {
            case GOOGLE -> oAuthConfig.getGoogleRedirectUri();
            case KAKAO -> oAuthConfig.getKakaoRedirectUri();
            case NAVER -> oAuthConfig.getNaverRedirectUri();
        };
        return getSocialUserInfo(provider, code, state, defaultRedirectUri);
    }

    private String getAccessToken(SocialAccount.SocialProvider provider, String code, String redirectUri) {
        Map<String, String> tokenRequest = new HashMap<>();
        tokenRequest.put("code", code);
        tokenRequest.put("grant_type", "authorization_code");
        tokenRequest.put("redirect_uri", redirectUri);

        switch (provider) {
            case GOOGLE:
                tokenRequest.put("client_id", oAuthConfig.getAndroidGoogleClientId());
                tokenRequest.put("client_secret", oAuthConfig.getGoogleClientSecret());
                return oAuthClientService.getAccessToken("GOOGLE", OAuthConfig.Urls.GOOGLE_TOKEN_URL, tokenRequest);
            case KAKAO:
                tokenRequest.put("client_id", oAuthConfig.getKakaoRestApiKey());
                tokenRequest.put("client_secret", oAuthConfig.getKakaoClientSecret());
                return oAuthClientService.getAccessToken("KAKAO", OAuthConfig.Urls.KAKAO_TOKEN_URL, tokenRequest);
            case NAVER:
                tokenRequest.put("client_id", oAuthConfig.getNaverClientId());
                tokenRequest.put("client_secret", oAuthConfig.getNaverClientSecret());
                return oAuthClientService.getAccessToken("NAVER", OAuthConfig.Urls.NAVER_TOKEN_URL, tokenRequest);
            default:
                throw new OAuthException(provider.name(), "UNSUPPORTED_PROVIDER", "지원하지 않는 OAuth 제공자입니다: " + provider);
        }
    }

    private SocialUserInfo getUserInfoFromProvider(SocialAccount.SocialProvider provider, String accessToken) {
        switch (provider) {
            case GOOGLE:
                return getGoogleUserInfo(accessToken);
            case KAKAO:
                return getKakaoUserInfo(accessToken);
            case NAVER:
                return getNaverUserInfo(accessToken);
            default:
                throw new OAuthException(provider.name(), "UNSUPPORTED_PROVIDER", "지원하지 않는 OAuth 제공자입니다: " + provider);
        }
    }

    private SocialUserInfo getGoogleUserInfo(String accessToken) {
        // 1. 토큰 검증 (Audience Check)
        validateGoogleToken(accessToken);

        Map<String, Object> userInfo = oAuthClientService.getUserInfo("GOOGLE", 
            OAuthConfig.Urls.GOOGLE_USER_INFO_URL, accessToken);

        String providerId = userInfo.get("sub").toString();
        
        // 기존 사용자 확인
        Optional<SocialAccount> existingSocialAccount = socialAccountRepository
                .findByProviderAndProviderIdAndIsActiveTrue(SocialAccount.SocialProvider.GOOGLE, providerId);
        
        return SocialUserInfo.builder()
                .provider(SocialAccount.SocialProvider.GOOGLE)
                .providerId(providerId)
                .email((String) userInfo.get("email"))
                .name((String) userInfo.get("name"))
                .profileImageUrl((String) userInfo.get("picture"))
                .isExistingUser(existingSocialAccount.isPresent())
                .userId(existingSocialAccount.map(sa -> sa.getUser().getId()).orElse(null))
                .build();
    }

    private void validateGoogleToken(String accessToken) {
        try {
            // Google Token Info API 호출
            Map<String, Object> tokenInfo = oAuthClientService.getTokenInfo("GOOGLE", 
                "https://oauth2.googleapis.com/tokeninfo?access_token=" + accessToken);
            
            String aud = (String) tokenInfo.get("aud");
            
            // 앱의 Client ID와 일치하는지 확인 (여러 Client ID를 지원해야 할 수도 있음)
            // 현재는 Android Client ID만 확인하지만, 필요 시 Web Client ID 등도 확인하도록 확장 가능
            if (aud == null || !aud.equals(oAuthConfig.getAndroidGoogleClientId())) {
                // Web Client ID도 허용 (하이브리드 앱 환경 고려)
                if (oAuthConfig.getGoogleClientId() != null && aud.equals(oAuthConfig.getGoogleClientId())) {
                    return;
                }
                
                log.warn("Google Access Token Audience 불일치 - expected: {}, actual: {}", 
                    oAuthConfig.getAndroidGoogleClientId(), aud);
                throw new OAuthException("GOOGLE", "INVALID_TOKEN_AUDIENCE", "유효하지 않은 토큰입니다 (Audience 불일치)");
            }
        } catch (Exception e) {
            log.error("Google Token 검증 실패", e);
            throw new OAuthException("GOOGLE", "TOKEN_VALIDATION_ERROR", "토큰 검증 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    private SocialUserInfo getKakaoUserInfo(String accessToken) {
        Map<String, Object> userInfo = oAuthClientService.getKakaoUserInfo("KAKAO",
            OAuthConfig.Urls.KAKAO_USER_INFO_URL, accessToken);

        String providerId = userInfo.get("id").toString();
        @SuppressWarnings("unchecked")
        Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");
        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        
        // 기존 사용자 확인
        Optional<SocialAccount> existingSocialAccount = socialAccountRepository
                .findByProviderAndProviderIdAndIsActiveTrue(SocialAccount.SocialProvider.KAKAO, providerId);

        return SocialUserInfo.builder()
                .provider(SocialAccount.SocialProvider.KAKAO)
                .providerId(providerId)
                .email((String) kakaoAccount.get("email"))
                .name(profile != null ? (String) profile.get("nickname") : null)
                .profileImageUrl(profile != null ? (String) profile.get("profile_image_url") : null)
                .isExistingUser(existingSocialAccount.isPresent())
                .userId(existingSocialAccount.map(sa -> sa.getUser().getId()).orElse(null))
                .build();
    }

    private SocialUserInfo getNaverUserInfo(String accessToken) {
        Map<String, Object> result = oAuthClientService.getUserInfo("NAVER",
            OAuthConfig.Urls.NAVER_USER_INFO_URL, accessToken);

        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) result.get("response");
        String providerId = (String) userInfo.get("id");
        
        // 기존 사용자 확인
        Optional<SocialAccount> existingSocialAccount = socialAccountRepository
                .findByProviderAndProviderIdAndIsActiveTrue(SocialAccount.SocialProvider.NAVER, providerId);

        return SocialUserInfo.builder()
                .provider(SocialAccount.SocialProvider.NAVER)
                .providerId(providerId)
                .email((String) userInfo.get("email"))
                .name((String) userInfo.get("name"))
                .profileImageUrl((String) userInfo.get("profile_image"))
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

    /**
     * AccessToken으로 직접 사용자 정보 가져오기 (클라이언트가 이미 토큰을 획득한 경우)
     */
    public SocialUserInfo getUserInfoByAccessToken(SocialAccount.SocialProvider provider, String accessToken) {
        try {
            return getUserInfoFromProvider(provider, accessToken);
        } catch (OAuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("소셜 로그인 정보 조회 실패 - provider: {}, error: {}", provider, e.getMessage());
            throw new OAuthException(provider.name(), "USER_INFO_ERROR", "소셜 로그인 정보 조회 실패: " + e.getMessage(), e);
        }
    }
}