package com.cherrypick.app.domain.user.dto.response;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String phoneNumber;
    private String nickname;
    private Long pointBalance;

    // 구매력/판매력 분리
    private Integer buyerLevel;
    private Integer buyerExp;
    private Integer sellerLevel;
    private Integer sellerExp;

    // 프로필 이미지
    private String profileImageUrl;

    // 거래 지역
    private String address;

    // 자기소개
    private String bio;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}