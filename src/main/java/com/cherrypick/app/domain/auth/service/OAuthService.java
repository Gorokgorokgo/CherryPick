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
                tokenRequest.put("client_id", oAuthConfig.getKakaoClientId());
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
        Map<String, Object> userInfo = oAuthClientService.getUserInfo("GOOGLE", 
            OAuthConfig.Urls.GOOGLE_USER_INFO_URL, accessToken);

        String providerId = userInfo.get("id").toString();
        
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
}