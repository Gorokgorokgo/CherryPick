package com.cherrypick.app.domain.migration.service;

import com.cherrypick.app.domain.migration.entity.UserMigrationState;
import com.cherrypick.app.domain.migration.entity.MigrationConfig;
import com.cherrypick.app.domain.migration.exception.MigrationSecurityException;
import com.cherrypick.app.domain.user.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 마이그레이션 보안 및 검증 서비스
 *
 * 마이그레이션 과정에서의 보안 정책, 검증 규칙, 안전성 검사를 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MigrationSecurityService {

    @Value("${migration.security.max-rollbacks-per-day:3}")
    private int maxRollbacksPerDay;

    @Value("${migration.security.cooling-period-hours:24}")
    private int coolingPeriodHours;

    @Value("${migration.security.max-error-rate:0.05}")
    private double maxErrorRate;

    @Value("${migration.security.emergency-code-length:32}")
    private int emergencyCodeLength;

    @Value("${migration.security.service-token}")
    private String serviceToken;

    // ===============================
    // 1. 사용자 마이그레이션 권한 검증
    // ===============================

    /**
     * 사용자 마이그레이션 적격성 검증
     */
    public void validateUserEligibility(User user, String targetStrategy) {
        log.debug("사용자 마이그레이션 적격성 검증 시작 - 사용자ID: {}, 대상 전략: {}",
                user.getId(), targetStrategy);

        // 1. 기본 사용자 상태 검증
        validateBasicUserStatus(user);

        // 2. 마이그레이션 쿨링 기간 검증
        validateCoolingPeriod(user);

        // 3. 일일 롤백 횟수 제한 검증
        validateDailyRollbackLimit(user);

        // 4. 전략별 특수 조건 검증
        validateStrategySpecificConditions(user, targetStrategy);

        log.debug("✅ 사용자 마이그레이션 적격성 검증 통과 - 사용자ID: {}", user.getId());
    }

    /**
     * 기본 사용자 상태 검증
     */
    private void validateBasicUserStatus(User user) {
        // 계정 상태 검증
        if (!user.isEnabled()) {
            throw new MigrationSecurityException("비활성화된 계정은 마이그레이션할 수 없습니다.");
        }

        // 이메일 인증 상태 검증
        if (!user.isEmailVerified()) {
            throw new MigrationSecurityException("이메일 인증이 완료되지 않은 계정은 마이그레이션할 수 없습니다.");
        }

        // 계정 생성 후 최소 기간 검증 (신규 계정 보호)
        LocalDateTime createdThreshold = LocalDateTime.now().minusDays(1);
        if (user.getCreatedAt().isAfter(createdThreshold)) {
            throw new MigrationSecurityException("계정 생성 후 24시간이 경과해야 마이그레이션할 수 있습니다.");
        }
    }

    /**
     * 마이그레이션 쿨링 기간 검증
     */
    private void validateCoolingPeriod(User user) {
        // TODO: UserMigrationState 조회하여 최근 마이그레이션 시점 확인
        // 현재는 기본 검증만 구현
        log.debug("쿨링 기간 검증 통과 - 사용자ID: {}", user.getId());
    }

    /**
     * 일일 롤백 횟수 제한 검증
     */
    private void validateDailyRollbackLimit(User user) {
        // TODO: 오늘 롤백 횟수 조회
        // 현재는 기본 검증만 구현
        log.debug("일일 롤백 제한 검증 통과 - 사용자ID: {}", user.getId());
    }

    /**
     * 전략별 특수 조건 검증
     */
    private void validateStrategySpecificConditions(User user, String targetStrategy) {
        switch (targetStrategy.toLowerCase()) {
            case "reactquery":
                // React Query 전략 특수 조건
                validateReactQueryEligibility(user);
                break;
            case "hybrid":
                // Hybrid 전략 특수 조건
                validateHybridEligibility(user);
                break;
            default:
                log.debug("알 수 없는 전략에 대한 특수 조건 검증 건너뜀: {}", targetStrategy);
        }
    }

    private void validateReactQueryEligibility(User user) {
        // TODO: React Query 전략 특수 조건 구현
        // 예: 최소 활동 기간, 특정 기능 사용 이력 등
        log.debug("React Query 전략 적격성 검증 통과 - 사용자ID: {}", user.getId());
    }

    private void validateHybridEligibility(User user) {
        // TODO: Hybrid 전략 특수 조건 구현
        // 예: 베타 테스터 여부, 특정 권한 보유 등
        log.debug("Hybrid 전략 적격성 검증 통과 - 사용자ID: {}", user.getId());
    }

    // ===============================
    // 2. 시스템 안전성 검증
    // ===============================

    /**
     * 마이그레이션 시스템 전체 안전성 검사
     */
    public boolean isSystemSafeForMigration() {
        try {
            log.info("마이그레이션 시스템 안전성 검사 시작");

            // 1. 오류율 검사
            if (!checkErrorRate()) {
                log.warn("❌ 오류율 임계치 초과로 마이그레이션 차단");
                return false;
            }

            // 2. 시스템 부하 검사
            if (!checkSystemLoad()) {
                log.warn("❌ 시스템 부하 임계치 초과로 마이그레이션 차단");
                return false;
            }

            // 3. 데이터베이스 연결 상태 검사
            if (!checkDatabaseHealth()) {
                log.warn("❌ 데이터베이스 연결 이상으로 마이그레이션 차단");
                return false;
            }

            // 4. 백업 시스템 가용성 검사
            if (!checkBackupSystemHealth()) {
                log.warn("❌ 백업 시스템 이상으로 마이그레이션 차단");
                return false;
            }

            log.info("✅ 마이그레이션 시스템 안전성 검사 통과");
            return true;

        } catch (Exception e) {
            log.error("❌ 마이그레이션 안전성 검사 중 오류 발생", e);
            return false;
        }
    }

    private boolean checkErrorRate() {
        // TODO: 실제 오류율 계산 구현
        // 현재는 기본값 반환
        double currentErrorRate = 0.01; // 1%
        return currentErrorRate <= maxErrorRate;
    }

    private boolean checkSystemLoad() {
        // TODO: 시스템 부하 확인 구현
        // CPU, 메모리, 스레드풀 상태 등
        return true;
    }

    private boolean checkDatabaseHealth() {
        // TODO: 데이터베이스 연결 풀 상태 확인
        return true;
    }

    private boolean checkBackupSystemHealth() {
        // TODO: 백업 시스템 상태 확인
        return true;
    }

    // ===============================
    // 3. 관리자 권한 및 긴급 상황 처리
    // ===============================

    /**
     * 관리자 권한 검증
     */
    public void validateAdminPermission(User admin, String operation) {
        log.debug("관리자 권한 검증 - 사용자: {}, 작업: {}", admin.getEmail(), operation);

        if (!admin.hasRole("ADMIN")) {
            throw new MigrationSecurityException("관리자 권한이 필요합니다.");
        }

        // 특정 작업에 대한 추가 권한 검증
        validateSpecificAdminOperation(admin, operation);

        log.debug("✅ 관리자 권한 검증 통과 - 사용자: {}", admin.getEmail());
    }

    private void validateSpecificAdminOperation(User admin, String operation) {
        switch (operation.toUpperCase()) {
            case "EMERGENCY_ROLLBACK":
                validateEmergencyRollbackPermission(admin);
                break;
            case "PHASE_CHANGE":
                validatePhaseChangePermission(admin);
                break;
            case "BATCH_MIGRATION":
                validateBatchMigrationPermission(admin);
                break;
        }
    }

    private void validateEmergencyRollbackPermission(User admin) {
        // 긴급 롤백은 최고 관리자만 가능
        if (!admin.hasRole("SUPER_ADMIN")) {
            throw new MigrationSecurityException("긴급 롤백은 최고 관리자 권한이 필요합니다.");
        }
    }

    private void validatePhaseChangePermission(User admin) {
        // 단계 변경 권한 검증 로직
        log.debug("단계 변경 권한 검증 통과 - 관리자: {}", admin.getEmail());
    }

    private void validateBatchMigrationPermission(User admin) {
        // 배치 마이그레이션 권한 검증 로직
        log.debug("배치 마이그레이션 권한 검증 통과 - 관리자: {}", admin.getEmail());
    }

    /**
     * 긴급 상황 확인 코드 생성
     */
    public String generateEmergencyConfirmationCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder(emergencyCodeLength);

        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int i = 0; i < emergencyCodeLength; i++) {
            code.append(characters.charAt(random.nextInt(characters.length())));
        }

        String generatedCode = code.toString();
        log.info("🚨 긴급 상황 확인 코드 생성: {}", generatedCode);

        return generatedCode;
    }

    /**
     * 긴급 상황 확인 코드 검증
     */
    public boolean validateEmergencyConfirmationCode(String providedCode, String expectedCode) {
        if (providedCode == null || expectedCode == null) {
            return false;
        }

        boolean isValid = providedCode.trim().equalsIgnoreCase(expectedCode.trim());

        if (isValid) {
            log.info("✅ 긴급 상황 확인 코드 검증 성공");
        } else {
            log.warn("❌ 긴급 상황 확인 코드 검증 실패 - 제공된 코드: {}", providedCode);
        }

        return isValid;
    }

    // ===============================
    // 4. 서비스 토큰 검증 (내부 API용)
    // ===============================

    /**
     * 서비스 간 통신용 토큰 검증
     */
    public void validateServiceToken(String providedToken) {
        if (providedToken == null || providedToken.trim().isEmpty()) {
            throw new MigrationSecurityException("서비스 토큰이 제공되지 않았습니다.");
        }

        // 실제 운영 환경에서는 더 정교한 토큰 검증 필요
        // JWT, 암호화, 타임스탬프 검증 등
        if (!serviceToken.equals(providedToken.trim())) {
            throw new MigrationSecurityException("유효하지 않은 서비스 토큰입니다.");
        }

        log.debug("✅ 서비스 토큰 검증 성공");
    }

    // ===============================
    // 5. 감사 로깅 (Audit Logging)
    // ===============================

    /**
     * 중요 마이그레이션 이벤트 감사 로깅
     */
    public void auditLog(String eventType, User user, Object details) {
        // TODO: 감사 로그 시스템 연동
        log.info("🔍 AUDIT [{}] - 사용자: {} (ID: {}), 세부사항: {}",
                eventType, user.getEmail(), user.getId(), details);
    }

    /**
     * 보안 이벤트 로깅
     */
    public void securityLog(String eventType, String details, User user) {
        log.warn("🛡️ SECURITY [{}] - 사용자: {} (ID: {}), 세부사항: {}",
                eventType, user != null ? user.getEmail() : "UNKNOWN",
                user != null ? user.getId() : "N/A", details);
    }

    // ===============================
    // 6. 유틸리티 메서드
    // ===============================

    /**
     * 사용자 해시 계산 (일관성 있는 롤아웃을 위해)
     */
    public int calculateUserHash(Long userId) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(userId.toString().getBytes(StandardCharsets.UTF_8));

            // 해시의 처음 4바이트를 int로 변환
            int hashInt = ((hash[0] & 0xFF) << 24) |
                    ((hash[1] & 0xFF) << 16) |
                    ((hash[2] & 0xFF) << 8) |
                    (hash[3] & 0xFF);

            // 0-99 범위로 정규화
            return Math.abs(hashInt) % 100;

        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 해시 알고리즘을 찾을 수 없음", e);
            // 폴백: 단순 모듈로 연산
            return Math.abs(userId.intValue()) % 100;
        }
    }

    /**
     * 시간 기반 검증 (쿨링 기간, 만료 시간 등)
     */
    public boolean isWithinTimeLimit(LocalDateTime baseTime, int limitHours) {
        if (baseTime == null)
            return false;

        LocalDateTime limitTime = baseTime.plus(limitHours, ChronoUnit.HOURS);
        return LocalDateTime.now().isBefore(limitTime);
    }
}