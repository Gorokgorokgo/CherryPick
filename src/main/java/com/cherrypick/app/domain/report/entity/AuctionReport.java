package com.cherrypick.app.domain.report.entity;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.common.entity.BaseEntity;
import com.cherrypick.app.domain.report.enums.ReportReason;
import com.cherrypick.app.domain.report.enums.ReportStatus;
import com.cherrypick.app.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * 경매 신고 엔티티
 */
@Entity
@Table(name = "auction_reports",
       uniqueConstraints = {
           @UniqueConstraint(
               name = "uk_auction_reporter",
               columnNames = {"auction_id", "reporter_id"}
           )
       },
       indexes = {
           @Index(name = "idx_auction_id", columnList = "auction_id"),
           @Index(name = "idx_reporter_id", columnList = "reporter_id"),
           @Index(name = "idx_status", columnList = "status")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class AuctionReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "admin_comment", columnDefinition = "TEXT")
    private String adminComment;

    // === 정적 팩토리 메서드 ===

    /**
     * 새로운 신고 생성
     */
    public static AuctionReport createReport(
            Auction auction,
            User reporter,
            ReportReason reason,
            String description) {

        return AuctionReport.builder()
                .auction(auction)
                .reporter(reporter)
                .reason(reason)
                .description(description)
                .status(ReportStatus.PENDING)
                .build();
    }

    // === 비즈니스 메서드 ===

    /**
     * 신고 검토 완료 처리
     */
    public void markAsReviewed(String adminComment) {
        this.status = ReportStatus.REVIEWED;
        this.adminComment = adminComment;
    }

    /**
     * 신고 기각 처리
     */
    public void markAsDismissed(String adminComment) {
        this.status = ReportStatus.DISMISSED;
        this.adminComment = adminComment;
    }

    /**
     * 신고 사유와 설명 수정
     */
    public void updateReasonAndDescription(ReportReason reason, String description) {
        if (this.status != ReportStatus.PENDING) {
            throw new IllegalStateException("검토 중이거나 처리된 신고는 수정할 수 없습니다.");
        }
        this.reason = reason;
        this.description = description;
    }
}
