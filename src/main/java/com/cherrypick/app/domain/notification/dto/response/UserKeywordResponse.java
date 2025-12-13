package com.cherrypick.app.domain.notification.dto.response;

import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.notification.entity.UserKeyword;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 키워드 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserKeywordResponse {

    private Long id;
    private String keyword;
    private Category category;
    private String categoryName;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static UserKeywordResponse from(UserKeyword userKeyword) {
        return UserKeywordResponse.builder()
                .id(userKeyword.getId())
                .keyword(userKeyword.getKeyword())
                .category(userKeyword.getCategory())
                .categoryName(userKeyword.getCategory() != null ? userKeyword.getCategory().name() : null)
                .isActive(userKeyword.getIsActive())
                .createdAt(userKeyword.getCreatedAt())
                .build();
    }
}
