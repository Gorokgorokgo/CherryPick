package com.cherrypick.app.domain.auth.dto.request;

import com.cherrypick.app.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

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

    // === 선택 프로필 정보 ===
    
    @Size(max = 50, message = "실명은 50자 이하여야 합니다.")
    @Schema(example = "홍길동", description = "실명 (선택사항)")
    private String realName;
    
    @Schema(example = "1990-01-01", description = "생년월일 (선택사항)")
    private LocalDate birthDate;
    
    @Schema(example = "MALE", description = "성별 (MALE/FEMALE/OTHER, 선택사항)")
    private User.Gender gender;
    
    @Size(max = 200, message = "주소는 200자 이하여야 합니다.")
    @Schema(example = "서울특별시 강남구", description = "주소 (선택사항)")
    private String address;
    
    @Size(max = 20, message = "우편번호는 20자 이하여야 합니다.")
    @Pattern(regexp = "^\\d{5}$", message = "우편번호는 5자리 숫자여야 합니다.")
    @Schema(example = "06234", description = "우편번호 (선택사항)")
    private String zipCode;
    
    @Size(max = 500, message = "자기소개는 500자 이하여야 합니다.")
    @Schema(example = "안전한 거래를 추구하는 신뢰할 수 있는 판매자입니다.", description = "자기소개 (선택사항)")
    private String bio;
    
    // === 프로필 공개 설정 (기본값은 서버에서 처리) ===
    
    @Schema(example = "true", description = "프로필 공개 여부 (기본: true)")
    private Boolean isProfilePublic;
    
    @Schema(example = "false", description = "실명 공개 여부 (기본: false)")
    private Boolean isRealNamePublic;
    
    @Schema(example = "false", description = "생년월일 공개 여부 (기본: false)")
    private Boolean isBirthDatePublic;
}