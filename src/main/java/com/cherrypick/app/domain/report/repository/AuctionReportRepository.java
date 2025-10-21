package com.cherrypick.app.domain.report.repository;

import com.cherrypick.app.domain.report.entity.AuctionReport;
import com.cherrypick.app.domain.report.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 경매 신고 Repository
 */
@Repository
public interface AuctionReportRepository extends JpaRepository<AuctionReport, Long> {

    /**
     * 중복 신고 확인 (동일 사용자가 동일 경매에 중복 신고 방지)
     */
    boolean existsByAuctionIdAndReporterId(Long auctionId, Long reporterId);

    /**
     * 경매 ID와 신고자 ID로 신고 조회
     */
    Optional<AuctionReport> findByAuctionIdAndReporterId(Long auctionId, Long reporterId);

    /**
     * 특정 경매의 모든 신고 조회
     */
    List<AuctionReport> findByAuctionIdOrderByCreatedAtDesc(Long auctionId);

    /**
     * 특정 사용자가 한 모든 신고 조회 (페이징)
     */
    Page<AuctionReport> findByReporterIdOrderByCreatedAtDesc(Long reporterId, Pageable pageable);

    /**
     * 특정 상태의 신고 조회 (관리자용)
     */
    Page<AuctionReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    /**
     * 특정 경매에 대한 신고 개수
     */
    long countByAuctionId(Long auctionId);

    /**
     * 특정 경매의 PENDING 상태 신고 개수
     */
    @Query("SELECT COUNT(r) FROM AuctionReport r WHERE r.auction.id = :auctionId AND r.status = 'PENDING'")
    long countPendingReportsByAuctionId(@Param("auctionId") Long auctionId);
}
