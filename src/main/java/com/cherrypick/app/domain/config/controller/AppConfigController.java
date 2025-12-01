package com.cherrypick.app.domain.config.controller;

import com.cherrypick.app.domain.config.dto.response.AppConfigResponse;
import com.cherrypick.app.domain.config.service.AppConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 앱 설정 제공 컨트롤러
 *
 * APK 하드코딩 대신 서버에서 동적으로 설정값 제공
 * - 앱 시작 시 한 번만 호출
 * - 캐싱하여 네트워크 부하 최소화
 * - 긴급 상황 시 서버에서 즉시 설정 변경 가능
 */
@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*")
@Tag(name = "앱 설정", description = "동적 앱 설정 제공 (APK 하드코딩 방지)")
public class AppConfigController {

    private final AppConfigService appConfigService;

    public AppConfigController(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    @GetMapping("/app")
    @Operation(
        summary = "앱 설정 조회",
        description = """
            앱 시작 시 필요한 동적 설정값을 제공합니다.

            **보안 장점:**
            - APK에 설정값 하드코딩 방지
            - 서버에서 실시간 설정 변경 가능
            - 긴급 상황 시 즉시 대응 (예: 동기화 중단, 간격 조정)

            **사용 방법:**
            - 앱 시작 시 한 번 호출
            - 로컬 스토리지에 캐싱 (24시간)
            - 캐시 만료 또는 앱 재시작 시 재조회

            **응답 예시:**
            ```json
            {
              "version": "1.0",
              "sync": {
                "syncInterval": 30000,
                "autoBackupInterval": 300000,
                "debounceDelay": 5000,
                "throttleInterval": 15000,
                "inactiveInterval": 60000,
                "minSyncInterval": 10000
              },
              "circuitBreaker": {
                "failureThreshold": 3,
                "recoveryTimeout": 120000,
                "successThreshold": 2,
                "monitoringWindow": 20
              },
              "monitoring": {
                "maxStoredEvents": 1000,
                "alertCheckInterval": 60000
              },
              "features": {
                "wishlistSyncEnabled": true,
                "offlineModeEnabled": true
              }
            }
            ```
            """
    )
    @ApiResponse(responseCode = "200", description = "앱 설정 조회 성공")
    public ResponseEntity<AppConfigResponse> getAppConfig() {
        AppConfigResponse config = appConfigService.getAppConfig();
        return ResponseEntity.ok(config);
    }

    @GetMapping("/app/version")
    @Operation(
        summary = "설정 버전 확인",
        description = "클라이언트가 캐시된 설정이 최신인지 확인"
    )
    @ApiResponse(responseCode = "200", description = "설정 버전 반환")
    public ResponseEntity<String> getConfigVersion() {
        String version = appConfigService.getConfigVersion();
        return ResponseEntity.ok(version);
    }
}
