package com.cherrypick.app.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequest {
    
    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^010[0-9]{8}$", message = "올바른 전화번호 형식이 아닙니다.")
    private String phoneNumber;
    
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 12, message = "닉네임은 2~12자의 한글, 영문, 숫자, _,- 조합만 가능합니다.")
    @Pattern(
      regexp = "^[가-힣a-zA-Z0-9_-]{2,12}$",
      message = "닉네임은 2~12자의 한글, 영문, 숫자, _,- 조합만 가능합니다."
    )
    @Schema(example = "cherry_man")
    private String nickname;
    
    @NotBlank(message = "이메일은 필수입니다.")
    @Size(max = 250, message = "이메일은 250자 이하여야 합니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Schema(example = "user@example.com")
    private String email;
    
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 6, max = 20, message = "비밀번호는 6-20자 사이여야 합니다.")
    @Pattern(regexp = "^(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).*$",
             message = "비밀번호는 특수문자를 포함해야 합니다.")
    @Schema(example = "12qw!@QW")
    private String password;

    // === 선택 프로필 정보 (번개장터 스타일 - 최소 필드만) ===

    @Size(max = 200, message = "주소는 200자 이하여야 합니다.")
    @Schema(example = "서울특별시 강남구", description = "거래 지역 (선택사항)")
    private String address;

    @Size(max = 500, message = "자기소개는 500자 이하여야 합니다.")
    @Schema(example = "안전한 거래를 추구하는 신뢰할 수 있는 판매자입니다.", description = "자기소개 (선택사항)")
    private String bio;
}