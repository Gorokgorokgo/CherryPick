package com.cherrypick.app.domain.auth.security;

import com.cherrypick.app.domain.auth.entity.SocialAccount;
import com.cherrypick.app.domain.auth.exception.InvalidStateException;
import com.cherrypick.app.domain.auth.service.OAuthService;
import com.cherrypick.app.domain.auth.service.StateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class OAuthSecurityTest {

    @Autowired
    private StateService stateService;

    @Test
    @DisplayName("CSRF 공격 방지 - 유효하지 않은 state로 OAuth 요청 차단")
    void preventCSRFAttack_InvalidState() {
        // given
        String invalidState = "malicious-state-value";

        // when & then
        boolean isValid = stateService.validateAndRemoveState(invalidState);
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("State 재사용 공격 방지 - 한 번 사용된 state 재사용 차단")
    void preventStateReplayAttack() {
        // given
        String state = stateService.generateState();
        stateService.validateAndRemoveState(state); // 첫 번째 사용

        // when & then - 재사용 시도
        boolean isValidOnReuse = stateService.validateAndRemoveState(state);
        assertThat(isValidOnReuse).isFalse();
    }

    @Test
    @DisplayName("계정 잠금 기능 - 5회 실패 시 자동 잠금")
    void accountLocking_AfterFiveFailures() {
        // given
        SocialAccount account = SocialAccount.builder()
                .provider(SocialAccount.SocialProvider.GOOGLE)
                .providerId("test-provider-id")
                .build();

        // when - 5회 실패 기록
        for (int i = 0; i < 5; i++) {
            account.recordFailedLogin();
        }

        // then
        assertThat(account.isAccountLocked()).isTrue();
        assertThat(account.getLoginAttemptCount()).isEqualTo(5);
        assertThat(account.getLockedUntil()).isNotNull();
    }

    @Test
    @DisplayName("State 생성 - 충분한 무작위성 확인")
    void stateGeneration_Randomness() {
        // when
        String state1 = stateService.generateState();
        String state2 = stateService.generateState();
        String state3 = stateService.generateState();

        // then
        assertThat(state1).isNotEqualTo(state2);
        assertThat(state2).isNotEqualTo(state3);
        assertThat(state1).isNotEqualTo(state3);
        assertThat(state1.length()).isGreaterThan(20); // 충분한 길이
    }
}