package com.cherrypick.app.domain.location.service;

import com.cherrypick.app.common.exception.LocationException;
import com.cherrypick.app.config.OAuthConfig;
import com.cherrypick.app.domain.location.dto.KakaoLocalResponse;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;

/**
 * 위치 기반 서비스
 * - GPS 좌표 → 행정동 변환 (카카오 로컬 API)
 * - Haversine 공식을 이용한 거리 계산
 * - 위치 정보 검증
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private final UserRepository userRepository;
    private final WebClient webClient;

    private final OAuthConfig oAuthConfig;

    // 대한민국 좌표 범위
    private static final double KOREA_MIN_LAT = 33.0;
    private static final double KOREA_MAX_LAT = 43.0;
    private static final double KOREA_MIN_LON = 124.0;
    private static final double KOREA_MAX_LON = 132.0;

    // 지구 반지름 (km)
    private static final double EARTH_RADIUS_KM = 6371.0;

    // 위치 인증 유효 기간 (일)
    private static final int LOCATION_VALIDITY_DAYS = 30;

    /**
     * Haversine 공식을 이용한 두 지점 간 거리 계산
     *
     * @param lat1 시작점 위도
     * @param lon1 시작점 경도
     * @param lat2 종료점 위도
     * @param lon2 종료점 경도
     * @return 거리 (km)
     */
    public double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            throw new IllegalArgumentException("좌표 값은 null일 수 없습니다.");
        }

        // 위도/경도를 라디안으로 변환
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        // Haversine 공식
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * 좌표가 대한민국 범위 내에 있는지 검증
     *
     * @param latitude 위도
     * @param longitude 경도
     * @return 유효 여부
     */
    public boolean isValidKoreanCoordinate(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }

        return latitude >= KOREA_MIN_LAT && latitude <= KOREA_MAX_LAT
                && longitude >= KOREA_MIN_LON && longitude <= KOREA_MAX_LON;
    }

    /**
     * 카카오 로컬 API를 이용하여 좌표를 행정동명으로 변환
     *
     * @param latitude 위도
     * @param longitude 경도
     * @return 행정동명 (예: "서울시 강남구 역삼1동")
     */
    public String convertCoordinateToRegion(Double latitude, Double longitude) {
        if (!isValidKoreanCoordinate(latitude, longitude)) {
            throw new IllegalArgumentException("유효하지 않은 좌표입니다. 대한민국 범위 내의 좌표를 입력해주세요.");
        }

        String kakaoApiKey = oAuthConfig.getKakaoRestApiKey();
        if (kakaoApiKey == null || kakaoApiKey.isEmpty()) {
            log.error("Kakao API Key가 설정되지 않았습니다.");
            throw new LocationException("서버 설정 오류: Kakao API Key가 없습니다.");
        }

        log.info("카카오 로컬 API 요청: lat={}, lon={}, API Key 길이={}", latitude, longitude, kakaoApiKey.length());

        try {
            KakaoLocalResponse response = webClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("dapi.kakao.com")
                            .path("/v2/local/geo/coord2regioncode.json")
                            .queryParam("x", longitude)
                            .queryParam("y", latitude)
                            .build())
                    .header("Authorization", "KakaoAK " + kakaoApiKey)
                    .retrieve()
                    .bodyToMono(KakaoLocalResponse.class)
                    .block();

            if (response != null && response.getDocuments() != null && !response.getDocuments().isEmpty()) {
                KakaoLocalResponse.Document document = response.getDocuments().get(0);

                // 법정동명 반환 (예: "서울특별시 강남구 역삼동")
                // 시/도 + 시/군/구 + 읍/면/동 모두 포함
                return String.format("%s %s %s",
                        document.getRegion1depthName(), // 시/도 (예: 서울특별시)
                        document.getRegion2depthName(), // 시/군/구 (예: 강남구)
                        document.getRegion3depthName()  // 읍/면/동 (예: 역삼1동)
                ).trim();
            }

            log.warn("카카오 API 응답에서 지역 정보를 찾을 수 없습니다. lat={}, lon={}", latitude, longitude);
            throw new LocationException("해당 좌표의 행정동 정보를 찾을 수 없습니다.");

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("카카오 로컬 API 호출 실패: status={}, body={}", e.getStatusCode(), responseBody);
            throw new LocationException("Kakao API 오류: " + responseBody, e);
        } catch (Exception e) {
            log.error("카카오 로컬 API 호출 실패: lat={}, lon={}, error={}", latitude, longitude, e.getMessage());
            
            if (e.getMessage() != null && e.getMessage().contains("403")) {
                throw new LocationException("Kakao API 권한 오류(403): Kakao Developers에서 '로컬' API가 활성화되어 있는지, 또는 REST API 키가 맞는지 확인해주세요.", e);
            }
            
            throw new LocationException("위치 정보 변환 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 사용자 위치 업데이트
     *
     * @param userId 사용자 ID
     * @param latitude 위도
     * @param longitude 경도
     * @return 인증된 지역명
     */
    @Transactional
    public String updateUserLocation(Long userId, Double latitude, Double longitude) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 위치 검증
        if (!isValidKoreanCoordinate(latitude, longitude)) {
            throw new IllegalArgumentException("유효하지 않은 좌표입니다. 대한민국 범위 내의 좌표를 입력해주세요.");
        }


        // 좌표 → 행정동명 변환
        String regionName = convertCoordinateToRegion(latitude, longitude);

        // 사용자 정보 업데이트
        user.setLatitude(latitude);
        user.setLongitude(longitude);
        user.setVerifiedRegion(regionName);
        user.setLocationUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        log.info("사용자 위치 업데이트 완료: userId={}, region={}", userId, regionName);
        return regionName;
    }

    /**
     * 사용자의 현재 위치 정보 조회
     *
     * @param userId 사용자 ID
     * @return 위치 정보 (위도, 경도, 인증된 지역명)
     */
    public LocationInfo getUserLocation(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        boolean isLocationValid = false;
        if (user.getLocationUpdatedAt() != null) {
            // 30일 이내에 인증했는지 확인
            LocalDateTime expirationDate = user.getLocationUpdatedAt().plusDays(LOCATION_VALIDITY_DAYS);
            isLocationValid = LocalDateTime.now().isBefore(expirationDate);
        }

        return LocationInfo.builder()
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .verifiedRegion(user.getVerifiedRegion())
                .locationUpdatedAt(user.getLocationUpdatedAt())
                .isLocationValid(isLocationValid)
                .build();
    }



    /**
     * 위치 정보 DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class LocationInfo {
        private Double latitude;
        private Double longitude;
        private String verifiedRegion;
        private LocalDateTime locationUpdatedAt;
        private boolean isLocationValid;
    }
}
