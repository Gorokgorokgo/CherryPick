package com.cherrypick.app.domain.report.service;

import com.cherrypick.app.common.exception.BusinessException;
import com.cherrypick.app.common.exception.ErrorCode;
import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.report.dto.ReportAuctionRequest;
import com.cherrypick.app.domain.report.dto.ReportResponse;
import com.cherrypick.app.domain.report.entity.AuctionReport;
import com.cherrypick.app.domain.report.enums.ReportStatus;
import com.cherrypick.app.domain.report.repository.AuctionReportRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 경매 신고 서비스
 *
 * 비즈니스 로직:
 * 1. 신고 접수 및 검증
 * 2. 중복 신고 방지
 * 3. 본인 경매 신고 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final AuctionReportRepository reportRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    /**
     * 경매 신고 접수
     *
     * @param auctionId 경매 ID
     * @param reporterId 신고자 ID
     * @param request 신고 요청 정보
     * @return 신고 응답
     * @throws BusinessException 본인 경매 신고, 중복 신고, 경매/사용자 없음
     */
    @Transactional
    public ReportResponse reportAuction(Long auctionId, Long reporterId, ReportAuctionRequest request) {
        // 1. 요청 검증
        request.validate();

        // 2. 경매 조회
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        // 3. 신고자 조회
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 4. 본인 경매 신고 방지
        if (auction.getSeller().getId().equals(reporterId)) {
            throw new BusinessException(ErrorCode.SELF_REPORT_NOT_ALLOWED);
        }

        // 5. 중복 신고 방지
        if (reportRepository.existsByAuctionIdAndReporterId(auctionId, reporterId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_REPORT);
        }

        // 6. 신고 생성 및 저장
        AuctionReport report = AuctionReport.createReport(
                auction,
                reporter,
                request.getReason(),
                request.getDescription()
        );

        AuctionReport savedReport = reportRepository.save(report);

        log.info("경매 신고 접수 완료: 경매ID={}, 신고자ID={}, 사유={}",
                auctionId, reporterId, request.getReason());

        return ReportResponse.from(savedReport);
    }

    /**
     * 내가 한 신고 목록 조회
     *
     * @param reporterId 신고자 ID
     * @param pageable 페이징 정보
     * @return 신고 목록
     */
    public Page<ReportResponse> getMyReports(Long reporterId, Pageable pageable) {
        Page<AuctionReport> reports = reportRepository.findByReporterIdOrderByCreatedAtDesc(
                reporterId, pageable
        );

        return reports.map(ReportResponse::from);
    }

    /**
     * 특정 경매의 신고 수 조회
     *
     * @param auctionId 경매 ID
     * @return 신고 수
     */
    public long getReportCount(Long auctionId) {
        return reportRepository.countByAuctionId(auctionId);
    }

    /**
     * 특정 경매의 대기 중인 신고 수 조회
     *
     * @param auctionId 경매 ID
     * @return 대기 중인 신고 수
     */
    public long getPendingReportCount(Long auctionId) {
        return reportRepository.countPendingReportsByAuctionId(auctionId);
    }

    /**
     * 신고 상세 조회
     *
     * @param reportId 신고 ID
     * @return 신고 응답
     * @throws BusinessException 신고 없음
     */
    public ReportResponse getReportDetail(Long reportId) {
        AuctionReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));

        return ReportResponse.from(report);
    }

    /**
     * 관리자: 특정 상태의 신고 목록 조회
     *
     * @param status 신고 상태
     * @param pageable 페이징 정보
     * @return 신고 목록
     */
    public Page<ReportResponse> getReportsByStatus(ReportStatus status, Pageable pageable) {
        Page<AuctionReport> reports = reportRepository.findByStatusOrderByCreatedAtDesc(
                status, pageable
        );

        return reports.map(ReportResponse::from);
    }

    /**
     * 관리자: 신고 검토 완료 처리
     *
     * @param reportId 신고 ID
     * @param adminComment 관리자 코멘트
     * @return 신고 응답
     * @throws BusinessException 신고 없음
     */
    @Transactional
    public ReportResponse markAsReviewed(Long reportId, String adminComment) {
        AuctionReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));

        report.markAsReviewed(adminComment);
        AuctionReport updatedReport = reportRepository.save(report);

        log.info("신고 검토 완료: 신고ID={}", reportId);

        return ReportResponse.from(updatedReport);
    }

    /**
     * 관리자: 신고 기각 처리
     *
     * @param reportId 신고 ID
     * @param adminComment 관리자 코멘트
     * @return 신고 응답
     * @throws BusinessException 신고 없음
     */
    @Transactional
    public ReportResponse markAsDismissed(Long reportId, String adminComment) {
        AuctionReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));

        report.markAsDismissed(adminComment);
        AuctionReport updatedReport = reportRepository.save(report);

        log.info("신고 기각: 신고ID={}", reportId);

        return ReportResponse.from(updatedReport);
    }
}
