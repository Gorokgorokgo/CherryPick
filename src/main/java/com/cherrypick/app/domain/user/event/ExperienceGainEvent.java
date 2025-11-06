package com.cherrypick.app.domain.user.event;

import com.cherrypick.app.domain.user.dto.response.ExperienceGainResponse;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 경험치 획득 이벤트
 * - 프론트엔드 실시간 알림 트리거
 * - 비동기 알림 처리
 */
@Getter
public class ExperienceGainEvent extends ApplicationEvent {

    private final ExperienceGainResponse experienceGain;
    private final Long userId;

    public ExperienceGainEvent(Object source, ExperienceGainResponse experienceGain, Long userId) {
        super(source);
        this.experienceGain = experienceGain;
        this.userId = userId;
    }
}
