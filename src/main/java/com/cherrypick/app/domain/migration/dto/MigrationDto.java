package com.cherrypick.app.domain.migration.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 마이그레이션 관련 DTO 클래스들
 */
public class MigrationDto {

    // =====================================
    // 마이그레이션 단계 관련 DTO
    // =====================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "마이그레이션 단계 응답")
    public static class MigrationPhaseResponseDto {
        @Schema(description = "현재 마이그레이션 단계", example = "gradual")
        private String currentPhase;

        @Schema(description = "롤아웃 비율", example = "50")
        private Integer percentage;

        @Schema(description = "롤아웃 활성화 여부", example = "true")
        private Boolean rolloutEnabled;

        @Schema(description = "긴급 롤백 여부", example = "false")
        private Boolean emergencyRollback;

        @Schema(description = "단계 변경 시간")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime phaseChangeDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "마이그레이션 단계 변경 요청")
    public static class MigrationPhaseChangeRequestDto {
        @NotBlank(message = "마이그레이션 단계는 필수입니다")
        @Schema(description = "변경할 마이그레이션 단계", example = "majority")
        private String phase;

        @Schema(description = "변경 사유", example = "Gradual phase stable, advancing to majority")
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "마이그레이션 단계 변경 응답")
    public static class MigrationPhaseChangeResponseDto {
        @Schema(description = "이전 단계", example = "gradual")
        private String previousPhase;

        @Schema(description = "새로운 단계", example = "majority")
        private String newPhase;

        @Schema(description = "롤아웃 비율", example = "80")
        private Integer percentage;

        @Schema(description = "영향받는 사용자 수", example = "2500")
        private Long affectedUsers;

        @Schema(description = "예상 마이그레이션 시간", example = "2-4 hours")
        private String estimatedMigrationTime;
    }

    // =====================================
    // 사용자 마이그레이션 관련 DTO
    // =====================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 마이그레이션 전략 응답")
    public static class UserMigrationStrategyResponseDto {
        @Schema(description = "사용자 ID", example = "12345")
        private Long userId;

        @Schema(description = "현재 전략", example = "hybrid")
        private String strategy;

        @Schema(description = "할당된 단계", example = "gradual")
        private String assignedPhase;

        @Schema(description = "마이그레이션 날짜")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime migrationDate;

        @Schema(description = "롤백 가능 여부", example = "true")
        private Boolean rollbackAvailable;

        @Schema(description = "마지막 백업 키", example = "backup_12345_20240310_142000")
        private String lastBackup;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 마이그레이션 요청")
    public static class UserMigrationRequestDto {
        @NotBlank(message = "대상 전략은 필수입니다")
        @Schema(description = "대상 전략", example = "reactQuery")
        private String targetStrategy;

        @Builder.Default
        @Schema(description = "백업 생성 여부", example = "true")
        private Boolean createBackup = true;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 마이그레이션 응답")
    public static class UserMigrationResponseDto {
        @Schema(description = "마이그레이션 ID", example = "migration_12345_20240315")
        private String migrationId;

        @Schema(description = "사용자 ID", example = "12345")
        private Long userId;

        @Schema(description = "적용된 전략", example = "reactQuery")
        private String strategy;

        @Schema(description = "생성된 백업", example = "backup_12345_20240315_103000")
        private String backupCreated;

        @Schema(description = "예상 활성화 시간", example = "Next app restart")
        private String estimatedActivationTime;

        @Schema(description = "롤백 기한")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime rollbackDeadline;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 롤백 요청")
    public static class UserRollbackRequestDto {
        @NotBlank(message = "롤백 사유는 필수입니다")
        @Schema(description = "롤백 사유", example = "Performance issues")
        private String reason;

        @Builder.Default
        @Schema(description = "백업 복원 여부", example = "true")
        private Boolean restoreBackup = true;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 롤백 응답")
    public static class UserRollbackResponseDto {
        @Schema(description = "롤백 ID", example = "rollback_12345_20240315")
        private String rollbackId;

        @Schema(description = "사용자 ID", example = "12345")
        private Long userId;

        @Schema(description = "이전 전략", example = "reactQuery")
        private String previousStrategy;

        @Schema(description = "새로운 전략", example = "legacy")
        private String newStrategy;

        @Schema(description = "복원된 백업", example = "backup_12345_20240310_142000")
        private String backupRestored;

        @Schema(description = "롤백 횟수", example = "1")
        private Integer rollbackCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 마이그레이션 히스토리 응답")
    public static class UserMigrationHistoryResponseDto {
        @Schema(description = "사용자 ID", example = "12345")
        private Long userId;

        @Schema(description = "이벤트 목록")
        private List<MigrationEventDto> events;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "마이그레이션 이벤트")
    public static class MigrationEventDto {
        @Schema(description = "이벤트 ID", example = "event_12345_001")
        private String eventId;

        @Schema(description = "이벤트 타입", example = "MIGRATION_SUCCESS")
        private String type;

        @Schema(description = "변경 전 전략", example = "legacy")
        private String fromStrategy;

        @Schema(description = "변경 후 전략", example = "hybrid")
        private String toStrategy;

        @Schema(description = "이벤트 시간")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime timestamp;

        @Schema(description = "소요 시간 (ms)", example = "3200")
        private Long duration;

        @Schema(description = "백업 키", example = "backup_12345_20240310_142000")
        private String backup;

        @Schema(description = "사유", example = "User reported issues")
        private String reason;
    }

    // =====================================
    // 통계 및 모니터링 관련 DTO
    // =====================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "마이그레이션 통계 응답")
    public static class MigrationStatisticsResponseDto {
        @Schema(description = "현재 단계", example = "gradual")
        private String currentPhase;

        @Schema(description = "전체 사용자 수", example = "10000")
        private Long totalUsers;

        @Schema(description = "전략별 분포")
        private DistributionDto distribution;

        @Schema(description = "메트릭")
        private MetricsDto metrics;

        @Schema(description = "트렌드")
        private TrendsDto trends;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "전략별 분포")
    public static class DistributionDto {
        @Schema(description = "Legacy 사용자 수", example = "5000")
        private Long legacy;

        @Schema(description = "React Query 사용자 수", example = "3000")
        private Long reactQuery;

        @Schema(description = "Hybrid 사용자 수", example = "2000")
        private Long hybrid;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "메트릭")
    public static class MetricsDto {
        @Schema(description = "성공률", example = "95.5")
        private Double successRate;

        @Schema(description = "롤백률", example = "2.1")
        private Double rollbackRate;

        @Schema(description = "오류율", example = "0.8")
        private Double errorRate;

        @Schema(description = "평균 마이그레이션 시간", example = "3.2s")
        private String averageMigrationTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "트렌드")
    public static class TrendsDto {
        @Schema(description = "일일 마이그레이션 수", example = "500")
        private Long dailyMigrations;

        @Schema(description = "주간 증가율", example = "15.2")
        private Double weeklyGrowth;

        @Schema(description = "피크 시간", example = "14")
        private Integer peakUsageHour;
    }

    // =====================================
    // 관리자 및 안전성 관련 DTO
    // =====================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "관리자 대시보드 응답")
    public static class MigrationDashboardResponseDto {
        @Schema(description = "실시간 통계")
        private MigrationStatisticsResponseDto statistics;

        @Schema(description = "안전성 상태")
        private SafetyStatusDto safetyStatus;

        @Schema(description = "최근 이벤트")
        private List<MigrationEventDto> recentEvents;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "안전성 상태")
    public static class SafetyStatusDto {
        @Schema(description = "전체 안전성", example = "SAFE")
        private String overallSafety;

        @Schema(description = "오류율 상태", example = "PASS")
        private String errorRateStatus;

        @Schema(description = "롤백률 상태", example = "WARNING")
        private String rollbackRateStatus;

        @Schema(description = "시스템 부하 상태", example = "PASS")
        private String systemLoadStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "마이그레이션 안전성 검사 응답")
    public static class MigrationSafetyCheckResponseDto {
        @Schema(description = "전체 안전성", example = "SAFE")
        private String overallSafety;

        @Schema(description = "검사 항목들")
        private List<SafetyCheckDto> checks;

        @Schema(description = "권장사항")
        private List<String> recommendations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "안전성 검사 항목")
    public static class SafetyCheckDto {
        @Schema(description = "검사 이름", example = "errorRate")
        private String name;

        @Schema(description = "상태", example = "PASS")
        private String status;

        @Schema(description = "현재 값", example = "0.8")
        private Double value;

        @Schema(description = "임계값", example = "5.0")
        private Double threshold;

        @Schema(description = "메시지", example = "Error rate within acceptable limits")
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "긴급 롤백 요청")
    public static class EmergencyRollbackRequestDto {
        @NotBlank(message = "긴급 롤백 사유는 필수입니다")
        @Schema(description = "긴급 롤백 사유", example = "Critical performance degradation")
        private String reason;

        @NotBlank(message = "확인 코드는 필수입니다")
        @Schema(description = "확인 코드", example = "EMERGENCY_ROLLBACK_CONFIRM")
        private String confirmationCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "긴급 롤백 응답")
    public static class EmergencyRollbackResponseDto {
        @Schema(description = "롤백 ID", example = "emergency_rollback_20240315_143000")
        private String rollbackId;

        @Schema(description = "영향받는 사용자 수", example = "8500")
        private Long affectedUsers;

        @Schema(description = "새로운 단계", example = "legacy")
        private String newPhase;

        @Schema(description = "예상 완료 시간", example = "10-15 minutes")
        private String estimatedCompletionTime;

        @Schema(description = "모니터링 URL", example = "/api/v1/migration/emergency-status/emergency_rollback_20240315_143000")
        private String monitoringUrl;
    }

    // =====================================
    // 내부 API 관련 DTO
    // =====================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 상태 동기화 요청")
    public static class UserStateSyncRequestDto {
        @NotNull(message = "사용자 ID는 필수입니다")
        @Schema(description = "사용자 ID", example = "12345")
        private Long userId;

        @NotBlank(message = "현재 전략은 필수입니다")
        @Schema(description = "현재 전략", example = "hybrid")
        private String currentStrategy;

        @Schema(description = "디바이스 정보")
        private Object deviceInfo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 상태 동기화 응답")
    public static class UserStateSyncResponseDto {
        @Schema(description = "동기화 성공 여부", example = "true")
        private Boolean success;

        @Schema(description = "서버 권장 전략", example = "reactQuery")
        private String recommendedStrategy;

        @Schema(description = "동기화 시간")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime syncTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 적격성 응답")
    public static class UserEligibilityResponseDto {
        @Schema(description = "적격성 여부", example = "true")
        private Boolean eligible;

        @Schema(description = "사용자 해시", example = "42")
        private Integer userHash;

        @Schema(description = "현재 단계", example = "gradual")
        private String currentPhase;

        @Schema(description = "권장 전략", example = "hybrid")
        private String recommendedStrategy;

        @Schema(description = "제한 사항")
        private List<String> restrictions;
    }
}