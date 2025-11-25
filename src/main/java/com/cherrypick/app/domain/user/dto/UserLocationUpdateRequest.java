package com.cherrypick.app.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 위치 업데이트 요청 DTO
 *
 * 프론트엔드 요청 타입:
 * {
 *   latitude: number;   // 필수
 *   longitude: number;  // 필수
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 위치 업데이트 요청")
public class UserLocationUpdateRequest {

    @NotNull(message = "위도는 필수입니다.")
    @Min(value = 33, message = "위도는 33도 이상이어야 합니다.")
    @Max(value = 43, message = "위도는 43도 이하여야 합니다.")
    @Schema(description = "위도 (대한민국 범위: 33-43)", example = "37.5665", required = true)
    private Double latitude;

    @NotNull(message = "경도는 필수입니다.")
    @Min(value = 124, message = "경도는 124도 이상이어야 합니다.")
    @Max(value = 132, message = "경도는 132도 이하여야 합니다.")
    @Schema(description = "경도 (대한민국 범위: 124-132)", example = "126.9780", required = true)
    private Double longitude;
}
