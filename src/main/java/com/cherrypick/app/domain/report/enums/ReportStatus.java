package com.cherrypick.app.domain.report.enums;

/**
 * 신고 처리 상태
 */
public enum ReportStatus {
    PENDING("검토 대기"),
    REVIEWED("검토 완료"),
    DISMISSED("기각됨");

    private final String description;

    ReportStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
