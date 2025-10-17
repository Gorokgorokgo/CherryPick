package com.cherrypick.app.domain.auction.dto;

import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "경매 수정 요청 - 제목과 설명만 수정 가능 (eBay 정책과 동일)")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAuctionRequest {

    @Schema(description = "경매 제목", example = "수정된 아이폰 15 Pro 256GB", required = false)
    @Size(max = 100, message = "제목은 100자를 넘을 수 없습니다.")
    private String title;

    @Schema(description = "상품 설명 - 추가 정보 제공", example = "상품 설명 수정 및 추가 정보", required = false)
    @Size(max = 2000, message = "상품 설명은 2000자를 넘을 수 없습니다.")
    private String description;

    /**
     * 수정 요청 검증
     * 제목과 설명만 수정 가능하므로 별도 검증 불필요
     */
    public void validate() {
        // 제목이나 설명 중 하나는 있어야 함
        if ((title == null || title.trim().isEmpty()) &&
            (description == null || description.trim().isEmpty())) {
            throw new IllegalArgumentException("수정할 내용이 없습니다. 제목 또는 설명을 입력해주세요.");
        }
    }
}
