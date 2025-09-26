package com.cherrypick.app.domain.migration.service;

import com.cherrypick.app.domain.migration.dto.MigrationDto.*;
import com.cherrypick.app.domain.user.entity.User;
import org.springframework.stereotype.Service;

/**
 * 마이그레이션 서비스 인터페이스
 * 임시로 기본 구현만 제공
 */
@Service
public class MigrationService {

    // 임시 구현 - 실제로는 인터페이스와 구현체로 분리

    public MigrationPhaseResponseDto getCurrentPhase() {
        return MigrationPhaseResponseDto.builder()
                .currentPhase("legacy")
                .percentage(0)
                .rolloutEnabled(false)
                .emergencyRollback(false)
                .build();
    }

    public MigrationPhaseChangeResponseDto changePhase(MigrationPhaseChangeRequestDto request, User admin) {
        return MigrationPhaseChangeResponseDto.builder()
                .previousPhase("legacy")
                .newPhase(request.getPhase())
                .percentage(50)
                .affectedUsers(0L)
                .estimatedMigrationTime("즉시")
                .build();
    }

    public UserMigrationStrategyResponseDto getUserStrategy(Long userId) {
        return UserMigrationStrategyResponseDto.builder()
                .userId(userId)
                .strategy("legacy")
                .assignedPhase("legacy")
                .rollbackAvailable(false)
                .build();
    }

    public UserMigrationResponseDto migrateUser(Long userId, UserMigrationRequestDto request) {
        return UserMigrationResponseDto.builder()
                .migrationId("migration_" + userId + "_" + System.currentTimeMillis())
                .userId(userId)
                .strategy(request.getTargetStrategy())
                .estimatedActivationTime("앱 재시작 후")
                .build();
    }

    public UserRollbackResponseDto rollbackUser(Long userId, UserRollbackRequestDto request) {
        return UserRollbackResponseDto.builder()
                .rollbackId("rollback_" + userId + "_" + System.currentTimeMillis())
                .userId(userId)
                .previousStrategy("reactQuery")
                .newStrategy("legacy")
                .rollbackCount(1)
                .build();
    }

    public UserMigrationHistoryResponseDto getUserHistory(Long userId, int page, int size) {
        return UserMigrationHistoryResponseDto.builder()
                .userId(userId)
                .events(java.util.Collections.emptyList())
                .build();
    }

    public MigrationStatisticsResponseDto getStatistics(String period, String phase) {
        return MigrationStatisticsResponseDto.builder()
                .currentPhase("legacy")
                .totalUsers(1L)
                .distribution(DistributionDto.builder()
                        .legacy(1L)
                        .reactQuery(0L)
                        .hybrid(0L)
                        .build())
                .metrics(MetricsDto.builder()
                        .successRate(100.0)
                        .rollbackRate(0.0)
                        .errorRate(0.0)
                        .averageMigrationTime("0s")
                        .build())
                .build();
    }

    public MigrationDashboardResponseDto getDashboardData() {
        return MigrationDashboardResponseDto.builder()
                .statistics(getStatistics("daily", null))
                .safetyStatus(SafetyStatusDto.builder()
                        .overallSafety("SAFE")
                        .errorRateStatus("PASS")
                        .rollbackRateStatus("PASS")
                        .systemLoadStatus("PASS")
                        .build())
                .recentEvents(java.util.Collections.emptyList())
                .build();
    }

    public MigrationSafetyCheckResponseDto performSafetyCheck() {
        return MigrationSafetyCheckResponseDto.builder()
                .overallSafety("SAFE")
                .checks(java.util.Collections.emptyList())
                .recommendations(java.util.Collections.emptyList())
                .build();
    }

    public EmergencyRollbackResponseDto emergencyRollback(EmergencyRollbackRequestDto request, User admin) {
        return EmergencyRollbackResponseDto.builder()
                .rollbackId("emergency_" + System.currentTimeMillis())
                .affectedUsers(0L)
                .newPhase("legacy")
                .estimatedCompletionTime("즉시")
                .build();
    }

    public UserStateSyncResponseDto syncUserState(UserStateSyncRequestDto request) {
        return UserStateSyncResponseDto.builder()
                .success(true)
                .recommendedStrategy("legacy")
                .build();
    }

    public UserEligibilityResponseDto checkUserEligibility(Long userId) {
        return UserEligibilityResponseDto.builder()
                .eligible(true)
                .userHash(userId.intValue() % 100)
                .currentPhase("legacy")
                .recommendedStrategy("legacy")
                .restrictions(java.util.Collections.emptyList())
                .build();
    }

    public void validateServiceToken(String serviceToken) {
        // 임시 구현 - 실제로는 검증 로직 필요
        if (!"valid-service-token".equals(serviceToken)) {
            throw new RuntimeException("Invalid service token");
        }
    }
}