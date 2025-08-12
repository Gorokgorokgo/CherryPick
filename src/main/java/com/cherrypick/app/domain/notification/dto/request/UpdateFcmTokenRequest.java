package com.cherrypick.app.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FCM 토큰 업데이트 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateFcmTokenRequest {

    @NotBlank(message = "FCM 토큰은 필수입니다.")
    private String fcmToken;
}