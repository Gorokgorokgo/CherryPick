package com.cherrypick.app.domain.report.enums;

/**
 * 경매 신고 사유
 */
public enum ReportReason {
    INAPPROPRIATE_CONTENT("부적절한 콘텐츠"),
    SPAM("스팸/도배"),
    FRAUD("사기/허위매물"),
    DUPLICATE_POSTING("중복 게시"),
    PROHIBITED_ITEM("판매금지 품목"),
    COPYRIGHT_VIOLATION("저작권 침해"),
    OTHER("기타");

    private final String description;

    ReportReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
