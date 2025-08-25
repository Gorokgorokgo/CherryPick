package com.cherrypick.app.domain.auth.service;

import com.cherrypick.app.config.JwtConfig;
import com.cherrypick.app.domain.auth.dto.request.PhoneLoginRequest;
import com.cherrypick.app.domain.auth.dto.response.AuthResponse;
import com.cherrypick.app.domain.auth.repository.AuthRepository;
import com.cherrypick.app.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PhoneLoginTest {

    @Mock
    private AuthRepository authRepository;
    
    @Mock
    private JwtConfig jwtConfig;
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @InjectMocks
    private AuthService authService;

    private PhoneLoginRequest phoneLoginRequest;
    private User testUser;
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        phoneLoginRequest = PhoneLoginRequest.builder()
                .phoneNumber("01012345678")
                .verificationCode("123456")
                .build();

        testUser = User.builder()
                .id(1L)
                .phoneNumber("01012345678")
                .email("test@example.com")
                .nickname("테스트유저")
                .build();

        valueOperations = mock(ValueOperations.class);
    }

    @Test
    @DisplayName("전화번호 로그인 성공")
    void phoneLogin_Success() {
        // given
        given(authRepository.findByPhoneNumber("01012345678")).willReturn(Optional.of(testUser));
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("verification:01012345678")).willReturn("123456");
        given(jwtConfig.generateToken(any(), anyLong())).willReturn("jwt-token");

        // when
        AuthResponse response = authService.phoneLogin(phoneLoginRequest);

        // then
        assertThat(response.getToken()).isNotNull();
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getNickname()).isEqualTo("테스트유저");
    }

    @Test
    @DisplayName("미등록 전화번호로 로그인 시도시 실패")
    void phoneLogin_Fail_UnregisteredUser() {
        // given - 아무것도 설정하지 않으면 Optional.empty() 반환됨

        // when
        AuthResponse response = authService.phoneLogin(phoneLoginRequest);

        // then
        assertThat(response.getToken()).isNull();
        assertThat(response.getMessage()).contains("가입되지 않은 전화번호");
    }

    @Test
    @DisplayName("잘못된 인증번호로 로그인 시도시 실패")
    void phoneLogin_Fail_WrongCode() {
        // given
        given(authRepository.findByPhoneNumber("01012345678")).willReturn(Optional.of(testUser));
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("verification:01012345678")).willReturn("999999"); // 다른 코드

        // when
        AuthResponse response = authService.phoneLogin(phoneLoginRequest);

        // then
        assertThat(response.getToken()).isNull();
        assertThat(response.getMessage()).contains("인증번호가 올바르지 않거나 만료");
    }
}