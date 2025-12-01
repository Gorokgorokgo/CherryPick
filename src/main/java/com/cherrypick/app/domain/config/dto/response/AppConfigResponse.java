package com.cherrypick.app.domain.config.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 앱 설정 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppConfigResponse {

    /**
     * 설정 버전 (변경 시 증가)
     */
    private String version;

    /**
     * 동기화 설정
     */
    private SyncConfig sync;

    /**
     * 회로 차단기 설정
     */
    private CircuitBreakerConfig circuitBreaker;

    /**
     * 모니터링 설정
     */
    private MonitoringConfig monitoring;

    /**
     * 기능 플래그
     */
    private FeatureFlags features;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncConfig {
        private Integer syncInterval;
        private Integer autoBackupInterval;
        private Integer debounceDelay;
        private Integer throttleInterval;
        private Integer inactiveInterval;
        private Integer minSyncInterval;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CircuitBreakerConfig {
        private Integer failureThreshold;
        private Integer recoveryTimeout;
        private Integer successThreshold;
        private Integer monitoringWindow;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonitoringConfig {
        private Integer maxStoredEvents;
        private Integer alertCheckInterval;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureFlags {
        /**
         * 위시리스트 동기화 활성화 여부
         */
        private Boolean wishlistSyncEnabled;

        /**
         * 오프라인 모드 활성화 여부
         */
        private Boolean offlineModeEnabled;

        /**
         * 긴급 유지보수 모드 (모든 쓰기 작업 차단)
         */
        private Boolean maintenanceMode;
    }
}
