package com.cherrypick.app.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OAuth 로그인 요청")
public class OAuthLoginRequest {

    @NotBlank(message = "액세스 토큰은 필수입니다.")
    @Schema(description = "OAuth provider로부터 받은 액세스 토큰", example = "ya29.a0AfH6SMBx...", required = true)
    private String accessToken;

    @NotNull(message = "Provider는 필수입니다.")
    @Schema(description = "OAuth Provider (google, kakao)", example = "google", required = true, allowableValues = {"google", "kakao"})
    private String provider;
}
