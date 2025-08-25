package com.cherrypick.app.domain.auth.service;

import com.cherrypick.app.config.JwtConfig;
import com.cherrypick.app.domain.auth.dto.request.SocialLoginRequest;
import com.cherrypick.app.domain.auth.dto.request.SocialSignupRequest;
import com.cherrypick.app.domain.auth.dto.response.AuthResponse;
import com.cherrypick.app.domain.auth.dto.response.SocialUserInfo;
import com.cherrypick.app.domain.auth.entity.SocialAccount;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SocialAuthServiceTest {

    @Mock
    private OAuthService oAuthService;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private SocialAuthService socialAuthService;

    private SocialLoginRequest socialLoginRequest;
    private SocialSignupRequest socialSignupRequest;
    private SocialUserInfo existingUserInfo;
    private SocialUserInfo newUserInfo;
    private User testUser;

    @BeforeEach
    void setUp() {
        socialLoginRequest = SocialLoginRequest.builder()
                .provider(SocialAccount.SocialProvider.GOOGLE)
                .code("test-code")
                .redirectUri("http://localhost:3000/callback")
                .build();

        socialSignupRequest = SocialSignupRequest.builder()
                .provider(SocialAccount.SocialProvider.GOOGLE)
                .code("test-code")
                .nickname("테스트유저")
                .redirectUri("http://localhost:3000/callback")
                .agreeToTerms(true)
                .agreeToPrivacy(true)
                .build();

        testUser = User.builder()
                .id(1L)
                .email("test@gmail.com")
                .nickname("테스트유저")
                .pointBalance(0L)
                .buyerLevel(1)
                .sellerLevel(1)
                .build();

        existingUserInfo = SocialUserInfo.builder()
                .provider(SocialAccount.SocialProvider.GOOGLE)
                .providerId("google-123")
                .email("test@gmail.com")
                .name("테스트유저")
                .isExistingUser(true)
                .userId(1L)
                .build();

        newUserInfo = SocialUserInfo.builder()
                .provider(SocialAccount.SocialProvider.GOOGLE)
                .providerId("google-456")
                .email("newuser@gmail.com")
                .name("신규유저")
                .isExistingUser(false)
                .build();
    }

    @Test
    @DisplayName("소셜 로그인 성공 - 기존 사용자")
    void socialLogin_Success_ExistingUser() {
        // given
        given(oAuthService.getSocialUserInfo(any(), any(), any())).willReturn(existingUserInfo);
        given(userRepository.findById(anyLong())).willReturn(Optional.of(testUser));
        given(jwtConfig.generateToken(any(), anyLong())).willReturn("test-jwt-token");

        // when
        AuthResponse response = socialAuthService.socialLogin(socialLoginRequest);

        // then
        assertThat(response.getToken()).isEqualTo("test-jwt-token");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getNickname()).isEqualTo("테스트유저");
    }

    @Test
    @DisplayName("소셜 로그인 실패 - 미가입 사용자")
    void socialLogin_Fail_NewUser() {
        // given
        given(oAuthService.getSocialUserInfo(any(), any(), any())).willReturn(newUserInfo);

        // when
        AuthResponse response = socialAuthService.socialLogin(socialLoginRequest);

        // then
        assertThat(response.getToken()).isNull();
        assertThat(response.getMessage()).contains("가입이 필요합니다");
    }

    @Test
    @DisplayName("소셜 회원가입 성공 - 신규 사용자")
    void socialSignup_Success_NewUser() {
        // given
        given(oAuthService.getSocialUserInfo(any(), any(), any())).willReturn(newUserInfo);
        given(userRepository.existsByNickname(any())).willReturn(false);
        given(userRepository.existsByEmail(any())).willReturn(false);
        given(userRepository.save(any(User.class))).willReturn(testUser);
        given(jwtConfig.generateToken(any(), anyLong())).willReturn("test-jwt-token");

        // when
        AuthResponse response = socialAuthService.socialSignup(socialSignupRequest);

        // then
        assertThat(response.getToken()).isEqualTo("test-jwt-token");
        assertThat(response.getMessage()).contains("소셜 회원가입 성공");
        verify(oAuthService).createSocialAccount(any(User.class), any(SocialUserInfo.class));
    }

    @Test
    @DisplayName("소셜 회원가입 실패 - 기존 사용자")
    void socialSignup_Fail_ExistingUser() {
        // given
        given(oAuthService.getSocialUserInfo(any(), any(), any())).willReturn(existingUserInfo);

        // when
        AuthResponse response = socialAuthService.socialSignup(socialSignupRequest);

        // then
        assertThat(response.getToken()).isNull();
        assertThat(response.getMessage()).contains("이미 가입된 소셜 계정");
    }

    @Test
    @DisplayName("소셜 회원가입 실패 - 닉네임 중복")
    void socialSignup_Fail_DuplicateNickname() {
        // given
        given(oAuthService.getSocialUserInfo(any(), any(), any())).willReturn(newUserInfo);
        given(userRepository.existsByNickname(any())).willReturn(true);

        // when
        AuthResponse response = socialAuthService.socialSignup(socialSignupRequest);

        // then
        assertThat(response.getToken()).isNull();
        assertThat(response.getMessage()).contains("이미 사용 중인 닉네임");
    }

    @Test
    @DisplayName("소셜 회원가입 실패 - 약관 미동의")
    void socialSignup_Fail_DisagreeTerms() {
        // given
        SocialSignupRequest invalidRequest = SocialSignupRequest.builder()
                .provider(SocialAccount.SocialProvider.GOOGLE)
                .code("test-code")
                .nickname("테스트유저")
                .agreeToTerms(false)  // 약관 미동의
                .agreeToPrivacy(true)
                .build();

        // when
        AuthResponse response = socialAuthService.socialSignup(invalidRequest);

        // then
        assertThat(response.getToken()).isNull();
        assertThat(response.getMessage()).contains("필수 약관에 동의");
    }
}