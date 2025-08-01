package com.cherrypick.app.domain.user.dto.request;

import com.cherrypick.app.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 10, message = "닉네임은 2-10자 사이여야 합니다.")
    private String nickname;
    
    // 프로필 이미지
    private String profileImageUrl;
    
    // 추가 개인정보
    @Size(max = 50, message = "실명은 50자 이하여야 합니다.")
    private String realName;
    
    private LocalDate birthDate;
    
    private User.Gender gender;
    
    @Size(max = 200, message = "주소는 200자 이하여야 합니다.")
    private String address;
    
    @Size(max = 20, message = "우편번호는 20자 이하여야 합니다.")
    private String zipCode;
    
    @Size(max = 500, message = "자기소개는 500자 이하여야 합니다.")
    private String bio;
    
    // 프로필 공개 설정
    private Boolean isProfilePublic;
    private Boolean isRealNamePublic;
    private Boolean isBirthDatePublic;
}