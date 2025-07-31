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
    private Integer level;
    private Long experience;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}