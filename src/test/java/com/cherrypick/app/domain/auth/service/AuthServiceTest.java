package com.cherrypick.app.domain.auth.service;

import com.cherrypick.app.config.JwtConfig;
import com.cherrypick.app.domain.auth.repository.AuthRepository;
import com.cherrypick.app.domain.auth.dto.request.SignupRequest;
import com.cherrypick.app.domain.auth.dto.response.AuthResponse;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("회원가입 - 기본 정보만으로 성공")
    void signup_BasicInfo_Success() {
        // Given
        SignupRequest request = new SignupRequest();
        request.setPhoneNumber("01012345678");
        request.setNickname("testUser");
        request.setEmail("test@example.com");
        request.setPassword("password123!");

        // Mock 설정
        when(valueOperations.get("verified:01012345678")).thenReturn("true");
        when(authRepository.findByPhoneNumber("01012345678")).thenReturn(Optional.empty());
        when(authRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(authRepository.findByNickname("testUser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123!")).thenReturn("encoded_password");
        when(jwtConfig.generateToken(anyString(), anyLong())).thenReturn("jwt_token");

        User savedUser = User.builder()
                .id(1L)
                .phoneNumber("01012345678")
                .nickname("testUser")
                .email("test@example.com")
                .password("encoded_password")
                .build();
        when(authRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        AuthResponse response = authService.signup(request);

        // Then
        assertNotNull(response);
        assertEquals("jwt_token", response.getToken());
        assertEquals("testUser", response.getNickname());
        verify(authRepository).save(any(User.class));
        verify(redisTemplate).delete("verified:01012345678");
    }

    @Test
    @DisplayName("회원가입 - 추가 프로필 정보 포함하여 성공")
    void signup_WithAdditionalProfile_Success() {
        // Given
        SignupRequest request = new SignupRequest();
        request.setPhoneNumber("01012345678");
        request.setNickname("testUser");
        request.setEmail("test@example.com");
        request.setPassword("password123!");
        
        // 추가 프로필 정보
        request.setRealName("홍길동");
        request.setBirthDate(LocalDate.of(1990, 1, 1));
        request.setGender(User.Gender.MALE);
        request.setAddress("서울특별시 강남구");
        request.setZipCode("06234");
        request.setBio("안전한 거래를 추구합니다.");
        request.setIsProfilePublic(true);
        request.setIsRealNamePublic(false);
        request.setIsBirthDatePublic(false);

        // Mock 설정
        when(valueOperations.get("verified:01012345678")).thenReturn("true");
        when(authRepository.findByPhoneNumber("01012345678")).thenReturn(Optional.empty());
        when(authRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(authRepository.findByNickname("testUser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123!")).thenReturn("encoded_password");
        when(jwtConfig.generateToken(anyString(), anyLong())).thenReturn("jwt_token");

        User savedUser = User.builder()
                .id(1L)
                .phoneNumber("01012345678")
                .nickname("testUser")
                .email("test@example.com")
                .password("encoded_password")
                .realName("홍길동")
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender(User.Gender.MALE)
                .address("서울특별시 강남구")
                .zipCode("06234")
                .bio("안전한 거래를 추구합니다.")
                .isProfilePublic(true)
                .isRealNamePublic(false)
                .isBirthDatePublic(false)
                .build();
        when(authRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        AuthResponse response = authService.signup(request);

        // Then
        assertNotNull(response);
        assertEquals("jwt_token", response.getToken());
        assertEquals("testUser", response.getNickname());
        verify(authRepository).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 - 전화번호 인증 미완료시 실패")
    void signup_NotVerified_Failure() {
        // Given
        SignupRequest request = new SignupRequest();
        request.setPhoneNumber("01012345678");
        request.setNickname("testUser");
        request.setEmail("test@example.com");
        request.setPassword("password123!");

        when(valueOperations.get("verified:01012345678")).thenReturn(null);

        // When
        AuthResponse response = authService.signup(request);

        // Then
        assertNotNull(response);
        assertNull(response.getToken());
        assertEquals("전화번호 인증이 필요합니다.", response.getMessage());
        verify(authRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 - 중복 이메일로 실패")
    void signup_DuplicateEmail_Failure() {
        // Given
        SignupRequest request = new SignupRequest();
        request.setPhoneNumber("01012345678");
        request.setNickname("testUser");
        request.setEmail("test@example.com");
        request.setPassword("password123!");

        when(valueOperations.get("verified:01012345678")).thenReturn("true");
        when(authRepository.findByPhoneNumber("01012345678")).thenReturn(Optional.empty());
        when(authRepository.findByEmail("test@example.com")).thenReturn(Optional.of(new User()));

        // When
        AuthResponse response = authService.signup(request);

        // Then
        assertNotNull(response);
        assertNull(response.getToken());
        assertEquals("이미 가입된 이메일입니다.", response.getMessage());
        verify(authRepository, never()).save(any(User.class));
    }
}