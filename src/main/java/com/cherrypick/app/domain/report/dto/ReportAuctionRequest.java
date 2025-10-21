package com.cherrypick.app.domain.report.dto;

import com.cherrypick.app.common.exception.BusinessException;
import com.cherrypick.app.common.exception.ErrorCode;
import com.cherrypick.app.domain.report.enums.ReportReason;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 경매 신고 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportAuctionRequest {

    @NotNull(message = "신고 사유를 선택해주세요.")
    private ReportReason reason;

    private String description;

    /**
     * 요청 검증
     * - OTHER 선택 시 description 필수
     * - description 길이 제한 (500자)
     */
    public void validate() {
        // OTHER 선택 시 description 필수
        if (reason == ReportReason.OTHER) {
            if (description == null || description.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.REPORT_DESCRIPTION_REQUIRED);
            }
        }

        // description 길이 제한
        if (description != null && description.length() > 500) {
            throw new BusinessException(ErrorCode.REPORT_DESCRIPTION_TOO_LONG);
        }
    }
}
