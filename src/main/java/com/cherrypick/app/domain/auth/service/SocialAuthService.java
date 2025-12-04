package com.cherrypick.app.domain.auth.service;

import com.cherrypick.app.config.JwtConfig;
import com.cherrypick.app.domain.auth.dto.request.SocialLoginRequest;
import com.cherrypick.app.domain.auth.dto.request.SocialSignupRequest;
import com.cherrypick.app.domain.auth.dto.response.AuthResponse;
import com.cherrypick.app.domain.auth.dto.response.SocialUserInfo;
import com.cherrypick.app.domain.auth.entity.SocialAccount;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import com.cherrypick.app.domain.notification.event.AccountRestoredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class SocialAuthService {

    private final OAuthService oAuthService;
    private final UserRepository userRepository;
    private final JwtConfig jwtConfig;
    private final ApplicationEventPublisher eventPublisher;

    public SocialAuthService(OAuthService oAuthService, UserRepository userRepository, JwtConfig jwtConfig, ApplicationEventPublisher eventPublisher) {
        this.oAuthService = oAuthService;
        this.userRepository = userRepository;
        this.jwtConfig = jwtConfig;
        this.eventPublisher = eventPublisher;
    }

    public AuthResponse socialLogin(SocialLoginRequest request) {
        try {
            SocialUserInfo socialUserInfo;

            // Access Token이 있는 경우 (모바일 앱)
            if (request.getAccessToken() != null && !request.getAccessToken().isEmpty()) {
                socialUserInfo = oAuthService.getUserInfoByAccessToken(
                        request.getProvider(),
                        request.getAccessToken()
                );
            } 
            // Authorization Code가 있는 경우 (웹)
            else {
                // 1. OAuth 제공자에서 사용자 정보 조회 (State 검증 포함)
                socialUserInfo = oAuthService.getSocialUserInfo(
                        request.getProvider(), 
                        request.getCode(),
                        request.getState(),
                        request.getRedirectUri()
                );
            }

            // 2. 기존 연동된 계정 확인 및 자동 회원가입
            if (!socialUserInfo.isExistingUser()) {
                // 자동 회원가입 로직 시작
                log.info("신규 소셜 사용자 자동 가입 진행: email={}", socialUserInfo.getEmail());

                // 이메일 중복 체크 (다른 소셜이나 일반 가입으로 이미 존재하는지)
                if (socialUserInfo.getEmail() != null && userRepository.existsByEmail(socialUserInfo.getEmail())) {
                    // 정책 결정: 이미 존재하는 이메일이면, 해당 계정에 연동할지 아니면 에러를 낼지.
                    // 편의상 자동 연동을 시도하거나, 에러 메시지를 명확히 줍니다.
                    // 여기서는 기존 계정이 있으면 소셜 계정을 연결하고 로그인 처리합니다.
                    User existingUser = userRepository.findByEmail(socialUserInfo.getEmail())
                            .orElseThrow(() -> new RuntimeException("사용자 조회 오류"));
                    
                    // 소셜 계정 연동
                    oAuthService.createSocialAccount(existingUser, socialUserInfo);
                    
                    // 기존 유저로 로그인 진행
                    socialUserInfo.setUserId(existingUser.getId());
                    socialUserInfo.setExistingUser(true);
                } else {
                    // 완전 신규 가입
                    String nickname = socialUserInfo.getName();
                    if (nickname == null || nickname.isEmpty()) {
                        nickname = "User_" + System.currentTimeMillis();
                    }

                    // 닉네임 중복 처리 (랜덤 접미사 추가)
                    int retryCount = 0;
                    String originalNickname = nickname;
                    while (userRepository.existsByNickname(nickname)) {
                        nickname = originalNickname + "_" + (int)(Math.random() * 10000);
                        retryCount++;
                        if (retryCount > 5) break; // 무한 루프 방지
                    }

                    User.UserBuilder userBuilder = User.builder()
                            .nickname(nickname)
                            .email(socialUserInfo.getEmail())
                            .password("") // 소셜 로그인 사용자는 비밀번호 불필요
                            .phoneNumber(null) // 소셜 로그인 사용자는 전화번호 선택
                            .pointBalance(0L)
                            .buyerLevel(1)
                            .buyerExp(0)
                            .sellerLevel(1)
                            .sellerExp(0);

                    if (socialUserInfo.getProfileImageUrl() != null) {
                        userBuilder.profileImageUrl(socialUserInfo.getProfileImageUrl());
                    }

                    User newUser = userBuilder.build();
                    User savedUser = userRepository.save(newUser);

                    // 소셜 계정 정보 저장
                    oAuthService.createSocialAccount(savedUser, socialUserInfo);
                    
                    // 생성된 ID 설정
                    socialUserInfo.setUserId(savedUser.getId());
                }
            }

            // 3. 사용자 조회
            User user = userRepository.findById(socialUserInfo.getUserId())
                    .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

            // 정지된 사용자 체크 (자동 복구 불가)
            if (user.getRole() == User.Role.BANNED) {
                return new AuthResponse("운영 정책 위반으로 이용이 정지된 계정입니다.");
            }

            boolean isRestored = false;

            // 탈퇴한 사용자 자동 복구 (자진 탈퇴만 해당)
            if (user.isDeleted() || !user.isEnabled()) {
                log.info("탈퇴한 사용자 자동 복구: userId={}", user.getId());
                user.restore();
                userRepository.save(user);
                isRestored = true;
                
                // 복구 알림 이벤트 발행
                eventPublisher.publishEvent(new AccountRestoredEvent(this, user.getId()));
            }

            // 4. JWT 토큰 생성
            String token = jwtConfig.generateToken(user.getEmail(), user.getId());

            log.info("Social Login User Region: {}", user.getVerifiedRegion());

            String message = isRestored ? "계정이 복구되었습니다. 다시 오신 것을 환영합니다!" : "로그인 성공";

            return new AuthResponse(
                token, 
                user.getId(), 
                user.getEmail(), 
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getAddress(),
                user.getVerifiedRegion(),
                user.getBio(),
                message
            );

        } catch (Exception e) {
            log.error("소셜 로그인 처리 중 예외 발생", e);
            return new AuthResponse("소셜 로그인 실패: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public SocialUserInfo getSocialUserInfo(SocialLoginRequest request) {
        return oAuthService.getSocialUserInfo(request.getProvider(), request.getCode(), request.getState(), request.getRedirectUri());
    }
}