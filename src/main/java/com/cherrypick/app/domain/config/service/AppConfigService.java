package com.cherrypick.app.domain.config.service;

import com.cherrypick.app.domain.config.dto.response.AppConfigResponse;
import org.springframework.stereotype.Service;

/**
 * 앱 설정 서비스
 *
 * 동적 설정값 제공을 통한 보안 강화:
 * 1. APK에 민감한 설정 하드코딩 방지
 * 2. 서버에서 실시간 설정 변경 가능
 * 3. 긴급 상황 대응 (기능 비활성화, 간격 조정 등)
 */
@Service
public class AppConfigService {

    // 설정 버전 (변경 시 증가)
    private static final String CONFIG_VERSION = "1.0.0";

    /**
     * 앱 설정 조회
     *
     * @return 앱 설정
     */
    public AppConfigResponse getAppConfig() {
        return AppConfigResponse.builder()
                .version(CONFIG_VERSION)
                .sync(buildSyncConfig())
                .circuitBreaker(buildCircuitBreakerConfig())
                .monitoring(buildMonitoringConfig())
                .features(buildFeatureFlags())
                .build();
    }

    /**
     * 설정 버전 조회
     */
    public String getConfigVersion() {
        return CONFIG_VERSION;
    }

    /**
     * 동기화 설정 생성
     */
    private AppConfigResponse.SyncConfig buildSyncConfig() {
        return AppConfigResponse.SyncConfig.builder()
                .syncInterval(30000)              // 30초
                .autoBackupInterval(300000)       // 5분
                .debounceDelay(5000)             // 5초
                .throttleInterval(15000)         // 15초
                .inactiveInterval(60000)         // 60초
                .minSyncInterval(10000)          // 10초
                .build();
    }

    /**
     * 회로 차단기 설정 생성
     */
    private AppConfigResponse.CircuitBreakerConfig buildCircuitBreakerConfig() {
        return AppConfigResponse.CircuitBreakerConfig.builder()
                .failureThreshold(3)             // 3회 연속 실패
                .recoveryTimeout(120000)         // 2분
                .successThreshold(2)             // 2회 성공
                .monitoringWindow(20)            // 20회 요청
                .build();
    }

    /**
     * 모니터링 설정 생성
     */
    private AppConfigResponse.MonitoringConfig buildMonitoringConfig() {
        return AppConfigResponse.MonitoringConfig.builder()
                .maxStoredEvents(1000)           // 최대 1000개 이벤트
                .alertCheckInterval(60000)       // 1분
                .build();
    }

    /**
     * 기능 플래그 생성
     */
    private AppConfigResponse.FeatureFlags buildFeatureFlags() {
        return AppConfigResponse.FeatureFlags.builder()
                .wishlistSyncEnabled(true)       // 위시리스트 동기화 활성화
                .offlineModeEnabled(true)        // 오프라인 모드 활성화
                .maintenanceMode(false)          // 유지보수 모드 비활성화
                .build();
    }

    /**
     * 긴급 유지보수 모드 활성화 예시
     *
     * 서버 문제 발생 시:
     * 1. maintenanceMode = true 설정
     * 2. 모든 클라이언트가 다음 설정 조회 시 쓰기 작업 중단
     * 3. 문제 해결 후 maintenanceMode = false로 복구
     */
}
