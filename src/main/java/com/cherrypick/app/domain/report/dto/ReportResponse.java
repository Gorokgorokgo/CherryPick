package com.cherrypick.app.domain.report.dto;

import com.cherrypick.app.domain.report.entity.AuctionReport;
import com.cherrypick.app.domain.report.enums.ReportReason;
import com.cherrypick.app.domain.report.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 신고 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {

    private Long reportId;
    private Long auctionId;
    private String auctionTitle;
    private Long reporterId;
    private String reporterNickname;
    private ReportReason reason;
    private String description;
    private ReportStatus status;
    private String adminComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity → DTO 변환
     */
    public static ReportResponse from(AuctionReport report) {
        return ReportResponse.builder()
                .reportId(report.getId())
                .auctionId(report.getAuction().getId())
                .auctionTitle(report.getAuction().getTitle())
                .reporterId(report.getReporter().getId())
                .reporterNickname(report.getReporter().getNickname())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .adminComment(report.getAdminComment())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
