package com.cherrypick.app.domain.user.dto.response;

import com.cherrypick.app.domain.user.entity.User;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String phoneNumber;
    private String nickname;
    private Long pointBalance;
    private Integer level;
    private Long experience;
    
    // 프로필 이미지
    private String profileImageUrl;
    
    // 추가 개인정보
    private String realName;
    private LocalDate birthDate;
    private User.Gender gender;
    private String address;
    private String zipCode;
    private String bio;
    
    // 프로필 공개 설정
    private Boolean isProfilePublic;
    private Boolean isRealNamePublic;
    private Boolean isBirthDatePublic;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}