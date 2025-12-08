package com.cherrypick.app.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 10, message = "닉네임은 2-10자 사이여야 합니다.")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 사용 가능합니다.")
    private String nickname;

    // 프로필 이미지
    private String profileImageUrl;

    // 거래 지역
    @Size(max = 200, message = "주소는 200자 이하여야 합니다.")
    private String address;

    // 자기소개
    @Size(max = 500, message = "자기소개는 500자 이하여야 합니다.")
    private String bio;
}