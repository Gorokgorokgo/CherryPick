package com.cherrypick.app.domain.location.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 카카오 로컬 API - 좌표 → 행정구역 정보 응답 DTO
 * API: GET /v2/local/geo/coord2regioncode.json
 */
@Data
public class KakaoLocalResponse {

    @JsonProperty("meta")
    private Meta meta;

    @JsonProperty("documents")
    private List<Document> documents;

    @Data
    public static class Meta {
        @JsonProperty("total_count")
        private Integer totalCount;
    }

    @Data
    public static class Document {
        @JsonProperty("region_type")
        private String regionType; // "B" (법정동) 또는 "H" (행정동)

        @JsonProperty("code")
        private String code; // 행정구역 코드

        @JsonProperty("address_name")
        private String addressName; // 전체 주소 (예: "서울특별시 강남구 역삼동")

        @JsonProperty("region_1depth_name")
        private String region1depthName; // 시/도 (예: "서울특별시")

        @JsonProperty("region_2depth_name")
        private String region2depthName; // 시/군/구 (예: "강남구")

        @JsonProperty("region_3depth_name")
        private String region3depthName; // 읍/면/동 (예: "역삼동")

        @JsonProperty("region_4depth_name")
        private String region4depthName; // 리 (예: "")

        @JsonProperty("x")
        private String x; // 경도

        @JsonProperty("y")
        private String y; // 위도
    }
}
