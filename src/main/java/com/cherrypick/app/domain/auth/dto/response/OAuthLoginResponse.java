package com.cherrypick.app.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "OAuth 로그인 응답")
public class OAuthLoginResponse {

    @Schema(description = "성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 데이터")
    private OAuthLoginData data;

    @Schema(description = "에러 메시지 (실패 시)")
    private String error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "OAuth 로그인 데이터")
    public static class OAuthLoginData {

        @Schema(description = "JWT 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        private String token;

        @Schema(description = "사용자 ID", example = "1")
        private Long userId;

        @Schema(description = "전화번호 (선택적)", example = "01012345678")
        private String phoneNumber;

        @Schema(description = "닉네임", example = "체리유저")
        private String nickname;

        @Schema(description = "프로필 이미지 URL (선택적)", example = "https://example.com/profile.jpg")
        private String profileImageUrl;

        @Schema(description = "주소 (선택적)", example = "서울시 강남구")
        private String address;

        @Schema(description = "자기소개 (선택적)", example = "안녕하세요!")
        private String bio;

        @Schema(description = "메시지", example = "로그인 성공")
        private String message;
    }

    public static OAuthLoginResponse success(OAuthLoginData data) {
        return OAuthLoginResponse.builder()
                .success(true)
                .data(data)
                .build();
    }

    public static OAuthLoginResponse error(String errorMessage) {
        return OAuthLoginResponse.builder()
                .success(false)
                .error(errorMessage)
                .build();
    }
}
