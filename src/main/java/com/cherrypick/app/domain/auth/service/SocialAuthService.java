package com.cherrypick.app.domain.auth.service;

import com.cherrypick.app.config.JwtConfig;
import com.cherrypick.app.domain.auth.dto.request.SocialLoginRequest;
import com.cherrypick.app.domain.auth.dto.request.SocialSignupRequest;
import com.cherrypick.app.domain.auth.dto.response.AuthResponse;
import com.cherrypick.app.domain.auth.dto.response.SocialUserInfo;
import com.cherrypick.app.domain.auth.entity.SocialAccount;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SocialAuthService {

    private final OAuthService oAuthService;
    private final UserRepository userRepository;
    private final JwtConfig jwtConfig;

    public SocialAuthService(OAuthService oAuthService, UserRepository userRepository, JwtConfig jwtConfig) {
        this.oAuthService = oAuthService;
        this.userRepository = userRepository;
        this.jwtConfig = jwtConfig;
    }

    public AuthResponse socialLogin(SocialLoginRequest request) {
        try {
            // 1. OAuth 제공자에서 사용자 정보 조회 (State 검증 포함)
            SocialUserInfo socialUserInfo = oAuthService.getSocialUserInfo(
                    request.getProvider(), 
                    request.getCode(),
                    request.getState(),
                    request.getRedirectUri()
            );

            // 2. 기존 연동된 계정 확인
            if (!socialUserInfo.isExistingUser()) {
                return new AuthResponse("가입이 필요합니다. 회원가입을 먼저 진행해주세요.");
            }

            // 3. 사용자 조회
            User user = userRepository.findById(socialUserInfo.getUserId())
                    .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

            // 4. JWT 토큰 생성
            String token = jwtConfig.generateToken(user.getEmail(), user.getId());

            return new AuthResponse(token, user.getId(), user.getEmail(), user.getNickname());

        } catch (Exception e) {
            return new AuthResponse("소셜 로그인 실패: " + e.getMessage());
        }
    }

    public AuthResponse socialSignup(SocialSignupRequest request) {
        try {
            // 1. 약관 동의 확인
            if (!request.getAgreeToTerms() || !request.getAgreeToPrivacy()) {
                return new AuthResponse("필수 약관에 동의해주세요.");
            }

            // 2. OAuth 제공자에서 사용자 정보 조회 (State 검증 포함)
            SocialUserInfo socialUserInfo = oAuthService.getSocialUserInfo(
                    request.getProvider(), 
                    request.getCode(),
                    request.getState(),
                    request.getRedirectUri()
            );

            // 3. 이미 연동된 계정인지 확인
            if (socialUserInfo.isExistingUser()) {
                return new AuthResponse("이미 가입된 소셜 계정입니다. 로그인을 이용해주세요.");
            }

            // 4. 닉네임 중복 확인
            if (userRepository.existsByNickname(request.getNickname())) {
                return new AuthResponse("이미 사용 중인 닉네임입니다.");
            }

            // 5. 이메일 중복 확인 (소셜 계정 이메일이 있는 경우)
            if (socialUserInfo.getEmail() != null && userRepository.existsByEmail(socialUserInfo.getEmail())) {
                return new AuthResponse("이미 가입된 이메일입니다.");
            }

            // 6. 사용자 생성
            User user = User.builder()
                    .nickname(request.getNickname())
                    .email(socialUserInfo.getEmail())
                    .password("") // 소셜 로그인 사용자는 비밀번호 불필요
                    .phoneNumber("") // 소셜 로그인 사용자는 전화번호 불필요 (나중에 추가 가능)
                    .pointBalance(0L)
                    .buyerLevel(1)
                    .buyerExp(0)
                    .sellerLevel(1)
                    .sellerExp(0)
                    .isProfilePublic(true)
                    .isRealNamePublic(false)
                    .isBirthDatePublic(false)
                    .build();

            // 7. 소셜 정보로 프로필 보완
            if (socialUserInfo.getName() != null) {
                user.setRealName(socialUserInfo.getName());
            }
            if (socialUserInfo.getProfileImageUrl() != null) {
                user.setProfileImageUrl(socialUserInfo.getProfileImageUrl());
            }

            User savedUser = userRepository.save(user);

            // 8. 소셜 계정 연동 정보 저장
            oAuthService.createSocialAccount(savedUser, socialUserInfo);

            // 9. JWT 토큰 생성
            String token = jwtConfig.generateToken(savedUser.getEmail(), savedUser.getId());

            return new AuthResponse(token, savedUser.getId(), savedUser.getEmail(), savedUser.getNickname(), "소셜 회원가입 성공");

        } catch (Exception e) {
            return new AuthResponse("소셜 회원가입 실패: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public SocialUserInfo getSocialUserInfo(SocialLoginRequest request) {
        return oAuthService.getSocialUserInfo(request.getProvider(), request.getCode(), request.getState(), request.getRedirectUri());
    }
}