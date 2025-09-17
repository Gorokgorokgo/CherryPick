package com.cherrypick.app.domain.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StateServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private StateService stateService;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Test
    @DisplayName("OAuth state 생성 성공")
    void generateState_Success() {
        // when
        String state = stateService.generateState();

        // then
        assertThat(state).isNotNull();
        assertThat(state).isNotEmpty();
        assertThat(state.length()).isGreaterThan(20);
        verify(valueOperations).set(eq("oauth:state:" + state), eq("valid"), eq(10L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("유효한 state 검증 성공")
    void validateAndRemoveState_Success() {
        // given
        String state = "validTestState123";
        given(valueOperations.get("oauth:state:" + state)).willReturn("valid");

        // when
        boolean isValid = stateService.validateAndRemoveState(state);

        // then
        assertThat(isValid).isTrue();
        verify(redisTemplate).delete("oauth:state:" + state);
    }

    @Test
    @DisplayName("만료된 state 검증 실패")
    void validateAndRemoveState_Fail_Expired() {
        // given
        String state = "expiredState123";
        given(valueOperations.get("oauth:state:" + state)).willReturn(null);

        // when
        boolean isValid = stateService.validateAndRemoveState(state);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("빈 state 검증 실패")
    void validateAndRemoveState_Fail_Empty() {
        // when
        boolean isValid = stateService.validateAndRemoveState("");

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("null state 검증 실패")
    void validateAndRemoveState_Fail_Null() {
        // when
        boolean isValid = stateService.validateAndRemoveState(null);

        // then
        assertThat(isValid).isFalse();
    }
}