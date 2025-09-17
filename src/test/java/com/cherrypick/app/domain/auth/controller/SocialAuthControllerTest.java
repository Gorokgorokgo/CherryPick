package com.cherrypick.app.domain.auth.controller;

import com.cherrypick.app.domain.auth.dto.request.SocialLoginRequest;
import com.cherrypick.app.domain.auth.dto.request.SocialSignupRequest;
import com.cherrypick.app.domain.auth.dto.response.AuthResponse;
import com.cherrypick.app.domain.auth.dto.response.SocialUserInfo;
import com.cherrypick.app.domain.auth.entity.SocialAccount;
import com.cherrypick.app.domain.auth.service.SocialAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SocialAuthController.class)
class SocialAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SocialAuthService socialAuthService;

    @Autowired
    private ObjectMapper objectMapper;

    private SocialLoginRequest socialLoginRequest;
    private SocialSignupRequest socialSignupRequest;
    private SocialUserInfo socialUserInfo;
    private AuthResponse successAuthResponse;
    private AuthResponse failAuthResponse;

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

        socialUserInfo = SocialUserInfo.builder()
                .provider(SocialAccount.SocialProvider.GOOGLE)
                .providerId("google-123")
                .email("test@gmail.com")
                .name("테스트유저")
                .isExistingUser(true)
                .userId(1L)
                .build();

        successAuthResponse = new AuthResponse("test-jwt-token", 1L, "test@gmail.com", "테스트유저");
        failAuthResponse = new AuthResponse("가입이 필요합니다.");
    }

    @Test
    @DisplayName("소셜 계정 확인 API 성공")
    void checkSocialAccount_Success() throws Exception {
        // given
        given(socialAuthService.getSocialUserInfo(any())).willReturn(socialUserInfo);

        // when & then
        mockMvc.perform(post("/api/auth/social/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(socialLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("GOOGLE"))
                .andExpect(jsonPath("$.email").value("test@gmail.com"))
                .andExpect(jsonPath("$.isExistingUser").value(true))
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    @DisplayName("소셜 로그인 API 성공")
    void socialLogin_Success() throws Exception {
        // given
        given(socialAuthService.socialLogin(any())).willReturn(successAuthResponse);

        // when & then
        mockMvc.perform(post("/api/auth/social/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(socialLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-jwt-token"))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.email").value("test@gmail.com"))
                .andExpect(jsonPath("$.nickname").value("테스트유저"));
    }

    @Test
    @DisplayName("소셜 로그인 API 실패 - 미가입 사용자")
    void socialLogin_Fail_NewUser() throws Exception {
        // given
        given(socialAuthService.socialLogin(any())).willReturn(failAuthResponse);

        // when & then
        mockMvc.perform(post("/api/auth/social/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(socialLoginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.message").value("가입이 필요합니다."));
    }

    @Test
    @DisplayName("소셜 회원가입 API 성공")
    void socialSignup_Success() throws Exception {
        // given
        given(socialAuthService.socialSignup(any())).willReturn(successAuthResponse);

        // when & then
        mockMvc.perform(post("/api/auth/social/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(socialSignupRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-jwt-token"))
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    @DisplayName("지원하는 소셜 제공자 목록 API")
    void getSupportedProviders() throws Exception {
        // when & then
        mockMvc.perform(get("/api/auth/social/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").value("GOOGLE"))
                .andExpect(jsonPath("$[1]").value("KAKAO"))
                .andExpect(jsonPath("$[2]").value("NAVER"));
    }

    @Test
    @DisplayName("소셜 로그인 요청 검증 실패 - 필수값 누락")
    void socialLogin_ValidationFail() throws Exception {
        // given
        SocialLoginRequest invalidRequest = SocialLoginRequest.builder()
                .provider(null)  // 필수값 누락
                .code("test-code")
                .build();

        // when & then
        mockMvc.perform(post("/api/auth/social/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}