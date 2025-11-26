package com.cherrypick.app.domain.auth.service;

import com.cherrypick.app.config.JwtConfig;
import com.cherrypick.app.domain.auth.dto.request.OAuthLoginRequest;
import com.cherrypick.app.domain.auth.dto.response.OAuthLoginResponse;
import com.cherrypick.app.domain.auth.dto.response.SocialUserInfo;
import com.cherrypick.app.domain.auth.entity.SocialAccount;
import com.cherrypick.app.domain.auth.exception.OAuthException;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class OAuthAuthService {

    private final OAuthService oAuthService;
    private final UserRepository userRepository;
    private final JwtConfig jwtConfig;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * OAuth 로그인 (기존 사용자 로그인 또는 신규 사용자 자동 회원가입)
     */
    public OAuthLoginResponse oauthLogin(OAuthLoginRequest request) {
        try {
            // 1. Provider 검증 및 변환
            SocialAccount.SocialProvider provider = parseProvider(request.getProvider());

            // 2. AccessToken으로 사용자 정보 조회
            SocialUserInfo socialUserInfo = oAuthService.getUserInfoByAccessToken(provider, request.getAccessToken());

            // 3. 기존 사용자 확인
            if (socialUserInfo.isExistingUser()) {
                // 기존 사용자 로그인
                return loginExistingUser(socialUserInfo);
            } else {
                // 신규 사용자 자동 회원가입
                return signupNewUser(socialUserInfo);
            }

        } catch (OAuthException e) {
            log.error("OAuth 로그인 실패 - provider: {}, error: {}", request.getProvider(), e.getMessage());
            return OAuthLoginResponse.error(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("잘못된 provider: {}", request.getProvider());
            return OAuthLoginResponse.error("지원하지 않는 OAuth 제공자입니다: " + request.getProvider());
        } catch (Exception e) {
            log.error("OAuth 로그인 중 예상치 못한 오류 발생", e);
            return OAuthLoginResponse.error("OAuth 로그인 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 기존 사용자 로그인
     */
    private OAuthLoginResponse loginExistingUser(SocialUserInfo socialUserInfo) {
        try {
            User user = userRepository.findById(socialUserInfo.getUserId())
                    .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

            // 탈퇴한 사용자 확인
            if (user.isDeleted()) {
                return OAuthLoginResponse.error("탈퇴한 계정입니다.");
            }

            // JWT 토큰 생성
            String email = user.getEmail() != null ? user.getEmail() : generateEmailFromProviderId(socialUserInfo);
            String token = jwtConfig.generateToken(email, user.getId());

            // 응답 생성
            OAuthLoginResponse.OAuthLoginData data = OAuthLoginResponse.OAuthLoginData.builder()
                    .token(token)
                    .userId(user.getId())
                    .phoneNumber(user.getPhoneNumber())
                    .nickname(user.getNickname())
                    .profileImageUrl(user.getProfileImageUrl())
                    .address(user.getAddress())
                    .bio(user.getBio())
                    .message("로그인 성공")
                    .build();

            return OAuthLoginResponse.success(data);

        } catch (Exception e) {
            log.error("기존 사용자 로그인 실패", e);
            throw new RuntimeException("로그인 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 신규 사용자 자동 회원가입
     */
    private OAuthLoginResponse signupNewUser(SocialUserInfo socialUserInfo) {
        try {
            // 1. 이메일 설정 (없으면 provider_id 형식으로 생성)
            String email = socialUserInfo.getEmail();
            if (email == null || email.isEmpty()) {
                email = generateEmailFromProviderId(socialUserInfo);
            }

            // 2. 이메일 중복 확인
            if (userRepository.existsByEmail(email)) {
                return OAuthLoginResponse.error("이미 가입된 이메일입니다.");
            }

            // 3. 닉네임 생성 (소셜 이름 또는 자동 생성)
            String nickname = generateNickname(socialUserInfo);

            // 4. 사용자 생성
            User user = User.builder()
                    .email(email)
                    .nickname(nickname)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString())) // 랜덤 비밀번호 (OAuth 사용자는 사용 안 함)
                    .phoneNumber(null) // OAuth 사용자는 전화번호 선택적
                    .profileImageUrl(socialUserInfo.getProfileImageUrl())
                    .pointBalance(0L)
                    .buyerLevel(1)
                    .buyerExp(0)
                    .sellerLevel(1)
                    .sellerExp(0)
                    .enabled(true)
                    .emailVerified(true) // OAuth로 인증된 것으로 간주
                    .build();

            User savedUser = userRepository.save(user);

            // 5. 소셜 계정 연동 정보 저장
            oAuthService.createSocialAccount(savedUser, socialUserInfo);

            // 6. JWT 토큰 생성
            String token = jwtConfig.generateToken(savedUser.getEmail(), savedUser.getId());

            // 7. 응답 생성
            OAuthLoginResponse.OAuthLoginData data = OAuthLoginResponse.OAuthLoginData.builder()
                    .token(token)
                    .userId(savedUser.getId())
                    .phoneNumber(savedUser.getPhoneNumber())
                    .nickname(savedUser.getNickname())
                    .profileImageUrl(savedUser.getProfileImageUrl())
                    .address(savedUser.getAddress())
                    .bio(savedUser.getBio())
                    .message("로그인 성공")
                    .build();

            return OAuthLoginResponse.success(data);

        } catch (Exception e) {
            log.error("신규 사용자 회원가입 실패", e);
            throw new RuntimeException("회원가입 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * Provider 문자열을 SocialProvider enum으로 변환
     */
    private SocialAccount.SocialProvider parseProvider(String providerStr) {
        try {
            return SocialAccount.SocialProvider.valueOf(providerStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 OAuth 제공자입니다: " + providerStr);
        }
    }

    /**
     * Provider ID로부터 이메일 생성 (이메일이 없는 경우)
     */
    private String generateEmailFromProviderId(SocialUserInfo socialUserInfo) {
        String provider = socialUserInfo.getProvider().name().toLowerCase();
        String providerId = socialUserInfo.getProviderId();
        return provider + "_" + providerId + "@cherrypick.oauth";
    }

    /**
     * 닉네임 생성 (중복 시 숫자 추가)
     */
    private String generateNickname(SocialUserInfo socialUserInfo) {
        String baseName = socialUserInfo.getName();

        // 이름이 없으면 provider 기반으로 생성
        if (baseName == null || baseName.isEmpty()) {
            baseName = socialUserInfo.getProvider().name() + "유저";
        }

        // 닉네임 중복 확인 및 자동 생성
        String nickname = baseName;
        int suffix = 1;
        while (userRepository.existsByNickname(nickname)) {
            nickname = baseName + suffix;
            suffix++;
        }

        return nickname;
    }
}
