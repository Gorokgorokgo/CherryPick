package com.cherrypick.app.domain.user.dto.response;

import com.cherrypick.app.domain.user.entity.ExperienceHistory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 경험치 히스토리 응답 DTO
 * - 사용자 경험치 이력 조회
 */
@Getter
@Builder
public class ExperienceHistoryResponse {

    private Long id;
    private String type;
    private Integer expGained;
    private Integer levelBefore;
    private Integer levelAfter;
    private Boolean isLevelUp;
    private String reason;
    private String reasonDetail;
    private LocalDateTime timestamp;

    /**
     * ExperienceHistory 엔티티로부터 생성
     */
    public static ExperienceHistoryResponse from(ExperienceHistory history) {
        return ExperienceHistoryResponse.builder()
            .id(history.getId())
            .type(history.getType().getDescription())
            .expGained(history.getExpGained())
            .levelBefore(history.getLevelBefore())
            .levelAfter(history.getLevelAfter())
            .isLevelUp(history.getIsLevelUp())
            .reason(history.getReason())
            .reasonDetail(history.getReasonDetail())
            .timestamp(history.getCreatedAt())
            .build();
    }

    /**
     * 리스트 변환
     */
    public static List<ExperienceHistoryResponse> fromList(List<ExperienceHistory> histories) {
        return histories.stream()
            .map(ExperienceHistoryResponse::from)
            .collect(Collectors.toList());
    }
}
