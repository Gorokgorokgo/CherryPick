package com.cherrypick.app.domain.auth.service;

import com.cherrypick.app.config.JwtConfig;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - 회원 탈퇴 테스트")
class AuthServiceTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .phoneNumber("01012345678")
                .nickname("테스트유저")
                .email("test@example.com")
                .password("encodedPassword")
                .pointBalance(0L)
                .buyerLevel(1)
                .buyerExp(0)
                .sellerLevel(1)
                .sellerExp(0)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - Soft Delete로 처리")
    void deleteAccount_Success() {
        // given
        when(authRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(authRepository.save(any(User.class))).thenReturn(testUser);

        // when
        authService.deleteAccount(1L);

        // then
        assertThat(testUser.isDeleted()).isTrue();
        assertThat(testUser.getDeletedAt()).isNotNull();
        assertThat(testUser.getEnabled()).isFalse();
        verify(authRepository, times(1)).save(testUser);
        verify(authRepository, never()).delete(any(User.class)); // Hard delete는 호출되지 않음
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 사용자를 찾을 수 없음")
    void deleteAccount_UserNotFound() {
        // given
        when(authRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.deleteAccount(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");

        verify(authRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 이미 탈퇴한 사용자")
    void deleteAccount_AlreadyDeleted() {
        // given
        testUser.softDelete(); // 이미 탈퇴 처리
        when(authRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // when & then
        assertThatThrownBy(() -> authService.deleteAccount(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 탈퇴한 사용자입니다");

        verify(authRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("회원 복구 성공")
    void restoreAccount_Success() {
        // given
        testUser.softDelete(); // 탈퇴 상태로 설정
        when(authRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(authRepository.save(any(User.class))).thenReturn(testUser);

        // when
        authService.restoreAccount(1L);

        // then
        assertThat(testUser.isDeleted()).isFalse();
        assertThat(testUser.getDeletedAt()).isNull();
        assertThat(testUser.getEnabled()).isTrue();
        verify(authRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("회원 복구 실패 - 탈퇴하지 않은 사용자")
    void restoreAccount_NotDeleted() {
        // given
        when(authRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // when & then
        assertThatThrownBy(() -> authService.restoreAccount(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("탈퇴하지 않은 사용자입니다");

        verify(authRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("탈퇴 후 deletedAt 타임스탬프 확인")
    void deleteAccount_CheckDeletedAtTimestamp() {
        // given
        LocalDateTime beforeDelete = LocalDateTime.now();
        when(authRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(authRepository.save(any(User.class))).thenReturn(testUser);

        // when
        authService.deleteAccount(1L);
        LocalDateTime afterDelete = LocalDateTime.now();

        // then
        assertThat(testUser.getDeletedAt()).isNotNull();
        assertThat(testUser.getDeletedAt()).isBetween(beforeDelete, afterDelete);
    }

    @Test
    @DisplayName("탈퇴 후 enabled 상태 false로 변경 확인")
    void deleteAccount_CheckEnabledStatus() {
        // given
        assertThat(testUser.getEnabled()).isTrue(); // 초기 상태 확인
        when(authRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(authRepository.save(any(User.class))).thenReturn(testUser);

        // when
        authService.deleteAccount(1L);

        // then
        assertThat(testUser.getEnabled()).isFalse();
    }
}
