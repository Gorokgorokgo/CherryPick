package com.cherrypick.app.domain.notification.dto.request;

import com.cherrypick.app.domain.auction.enums.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 키워드 알림 등록 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateKeywordRequest {

    @NotBlank(message = "키워드는 필수입니다.")
    @Size(min = 2, max = 50, message = "키워드는 2~50자 사이여야 합니다.")
    private String keyword;

    /**
     * 특정 카테고리로 제한 (null이면 모든 카테고리)
     */
    private Category category;
}
