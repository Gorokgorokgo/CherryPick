package com.cherrypick.app.domain.user.dto.response;

import com.cherrypick.app.domain.user.entity.ExperienceHistory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 경험치 획득 응답 DTO
 * - 프론트엔드 애니메이션 트리거 데이터
 * - 레벨업 여부 포함
 */
@Getter
@Builder
public class ExperienceGainResponse {

    /**
     * 경험치 타입 (buyer/seller)
     */
    private String type;

    /**
     * 획득한 경험치
     */
    private Integer expGained;

    /**
     * 이전 경험치
     */
    private Integer expBefore;

    /**
     * 변경 후 경험치
     */
    private Integer expAfter;

    /**
     * 이전 레벨
     */
    private Integer levelBefore;

    /**
     * 변경 후 레벨
     */
    private Integer levelAfter;

    /**
     * 레벨업 여부
     */
    private Boolean isLevelUp;

    /**
     * 경험치 획득 사유
     */
    private String reason;

    /**
     * 경험치 획득 사유 상세
     */
    private String reasonDetail;

    /**
     * 현재 레벨 진행률 (0-100%)
     */
    private Double progressPercent;

    /**
     * 다음 레벨까지 남은 경험치
     */
    private Integer expToNextLevel;

    /**
     * 이벤트 발생 시간
     */
    private LocalDateTime timestamp;

    /**
     * ExperienceHistory 엔티티로부터 생성
     */
    public static ExperienceGainResponse from(ExperienceHistory history, Integer requiredExp) {
        int expToNextLevel = Math.max(0, requiredExp - history.getExpAfter());
        double progressPercent = requiredExp > 0
            ? Math.min(100.0, (history.getExpAfter() * 100.0) / requiredExp)
            : 100.0;

        return ExperienceGainResponse.builder()
            .type(history.getType().name().toLowerCase())
            .expGained(history.getExpGained())
            .expBefore(history.getExpBefore())
            .expAfter(history.getExpAfter())
            .levelBefore(history.getLevelBefore())
            .levelAfter(history.getLevelAfter())
            .isLevelUp(history.getIsLevelUp())
            .reason(history.getReason())
            .reasonDetail(history.getReasonDetail())
            .progressPercent(progressPercent)
            .expToNextLevel(expToNextLevel)
            .timestamp(history.getCreatedAt())
            .build();
    }

    /**
     * 간단한 생성자 (즉시 응답용)
     */
    public static ExperienceGainResponse create(
        ExperienceHistory.ExperienceType type,
        int expGained,
        int expBefore,
        int expAfter,
        int levelBefore,
        int levelAfter,
        boolean isLevelUp,
        String reason,
        String reasonDetail,
        int requiredExp
    ) {
        int expToNextLevel = Math.max(0, requiredExp - expAfter);
        double progressPercent = requiredExp > 0
            ? Math.min(100.0, (expAfter * 100.0) / requiredExp)
            : 100.0;

        return ExperienceGainResponse.builder()
            .type(type.name().toLowerCase())
            .expGained(expGained)
            .expBefore(expBefore)
            .expAfter(expAfter)
            .levelBefore(levelBefore)
            .levelAfter(levelAfter)
            .isLevelUp(isLevelUp)
            .reason(reason)
            .reasonDetail(reasonDetail)
            .progressPercent(progressPercent)
            .expToNextLevel(expToNextLevel)
            .timestamp(LocalDateTime.now())
            .build();
    }
}
