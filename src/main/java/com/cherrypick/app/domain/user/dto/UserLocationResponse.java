package com.cherrypick.app.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 위치 정보 응답 DTO
 *
 * 프론트엔드 응답 타입:
 * {
 *   latitude: number | null;
 *   longitude: number | null;
 *   verifiedRegion: string | null;     // 예: "서울시 강남구 역삼1동"
 *   regionCode: string | null;          // 예: "1168010100" (행정구역 코드)
 *   locationUpdatedAt: string | null;   // ISO 8601 형식
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 위치 정보 응답")
public class UserLocationResponse {

    @Schema(description = "위도", example = "37.5665", nullable = true)
    private Double latitude;

    @Schema(description = "경도", example = "126.9780", nullable = true)
    private Double longitude;

    @Schema(description = "인증된 행정동명", example = "서울특별시 강남구 역삼1동", nullable = true)
    private String verifiedRegion;

    @Schema(description = "행정구역 코드", example = "1168010100", nullable = true)
    private String regionCode;

    @Schema(description = "위치 업데이트 시각 (ISO 8601)", example = "2025-11-12T15:30:00", nullable = true)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime locationUpdatedAt;

    /**
     * User 엔티티로부터 LocationResponse 생성
     */
    public static UserLocationResponse from(com.cherrypick.app.domain.user.entity.User user) {
        if (user == null) {
            return null;
        }

        return UserLocationResponse.builder()
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .verifiedRegion(user.getVerifiedRegion())
                .regionCode(extractRegionCode(user.getVerifiedRegion()))
                .locationUpdatedAt(user.getLocationUpdatedAt())
                .build();
    }

    /**
     * verifiedRegion에서 간단한 지역 코드 추출 (임시)
     * TODO: 실제로는 카카오 API 응답의 region code를 User 엔티티에 저장하는 것이 좋음
     */
    private static String extractRegionCode(String verifiedRegion) {
        if (verifiedRegion == null) {
            return null;
        }

        // 간단한 매핑 (추후 개선)
        if (verifiedRegion.startsWith("서울")) {
            return "11";
        } else if (verifiedRegion.startsWith("부산")) {
            return "26";
        } else if (verifiedRegion.startsWith("대구")) {
            return "27";
        } else if (verifiedRegion.startsWith("인천")) {
            return "28";
        } else if (verifiedRegion.startsWith("광주")) {
            return "29";
        } else if (verifiedRegion.startsWith("대전")) {
            return "30";
        } else if (verifiedRegion.startsWith("울산")) {
            return "31";
        } else if (verifiedRegion.startsWith("세종")) {
            return "36";
        } else if (verifiedRegion.startsWith("경기")) {
            return "41";
        }

        return "99"; // 기타
    }
}
