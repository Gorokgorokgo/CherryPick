package com.cherrypick.app.domain.report.service;

import com.cherrypick.app.common.exception.BusinessException;
import com.cherrypick.app.common.exception.ErrorCode;
import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.report.dto.ReportAuctionRequest;
import com.cherrypick.app.domain.report.dto.ReportResponse;
import com.cherrypick.app.domain.report.entity.AuctionReport;
import com.cherrypick.app.domain.report.enums.ReportReason;
import com.cherrypick.app.domain.report.enums.ReportStatus;
import com.cherrypick.app.domain.report.repository.AuctionReportRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * ReportService 단위 테스트 (TDD)
 *
 * 테스트 우선순위:
 * 1. 비즈니스 로직 검증
 * 2. 예외 상황 처리
 * 3. 데이터 일관성 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("경매 신고 서비스 테스트")
class ReportServiceTest {

    @InjectMocks
    private ReportService reportService;

    @Mock
    private AuctionReportRepository reportRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private UserRepository userRepository;

    private User seller;
    private User reporter;
    private Auction auction;

    @BeforeEach
    void setUp() {
        // 판매자 생성
        seller = User.builder()
                .id(1L)
                .nickname("판매자")
                .email("seller@test.com")
                .phoneNumber("01012345678")
                .build();

        // 신고자 생성
        reporter = User.builder()
                .id(2L)
                .nickname("신고자")
                .email("reporter@test.com")
                .phoneNumber("01087654321")
                .build();

        // 경매 생성
        auction = Auction.createAuction(
                seller,
                "테스트 경매",
                "테스트 설명",
                Category.ELECTRONICS,
                new BigDecimal("10000"),
                new BigDecimal("50000"),
                new BigDecimal("30000"),
                24,
                RegionScope.NATIONWIDE,
                null,
                "전국"
        );
        // Reflection으로 ID 설정 (테스트용)
        try {
            java.lang.reflect.Field idField = Auction.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(auction, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("신고 생성 성공")
    void reportAuction_Success() {
        // Given
        ReportAuctionRequest request = new ReportAuctionRequest(
                ReportReason.SPAM,
                "스팸 게시물입니다."
        );

        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(userRepository.findById(2L)).willReturn(Optional.of(reporter));
        given(reportRepository.existsByAuctionIdAndReporterId(1L, 2L)).willReturn(false);

        AuctionReport savedReport = AuctionReport.createReport(
                auction, reporter, request.getReason(), request.getDescription()
        );
        given(reportRepository.save(any(AuctionReport.class))).willReturn(savedReport);

        // When
        ReportResponse response = reportService.reportAuction(1L, 2L, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAuctionId()).isEqualTo(1L);
        assertThat(response.getReporterId()).isEqualTo(2L);
        assertThat(response.getReason()).isEqualTo(ReportReason.SPAM);
        assertThat(response.getStatus()).isEqualTo(ReportStatus.PENDING);

        verify(reportRepository).save(any(AuctionReport.class));
    }

    @Test
    @DisplayName("본인 경매 신고 불가")
    void reportAuction_SelfReportNotAllowed() {
        // Given
        ReportAuctionRequest request = new ReportAuctionRequest(
                ReportReason.SPAM,
                "스팸 게시물입니다."
        );

        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(userRepository.findById(1L)).willReturn(Optional.of(seller));

        // When & Then
        assertThatThrownBy(() -> reportService.reportAuction(1L, 1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SELF_REPORT_NOT_ALLOWED);
    }

    @Test
    @DisplayName("중복 신고 불가")
    void reportAuction_DuplicateReportNotAllowed() {
        // Given
        ReportAuctionRequest request = new ReportAuctionRequest(
                ReportReason.FRAUD,
                "사기 의심"
        );

        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(userRepository.findById(2L)).willReturn(Optional.of(reporter));
        given(reportRepository.existsByAuctionIdAndReporterId(1L, 2L)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> reportService.reportAuction(1L, 2L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_REPORT);
    }

    @Test
    @DisplayName("OTHER 사유 선택 시 description 필수")
    void reportAuction_OtherReasonRequiresDescription() {
        // Given
        ReportAuctionRequest request = new ReportAuctionRequest(
                ReportReason.OTHER,
                null  // description 없음
        );

        // When & Then
        assertThatThrownBy(() -> request.validate())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REPORT_DESCRIPTION_REQUIRED);
    }

    @Test
    @DisplayName("description 500자 초과 불가")
    void reportAuction_DescriptionTooLong() {
        // Given
        String longDescription = "a".repeat(501);
        ReportAuctionRequest request = new ReportAuctionRequest(
                ReportReason.SPAM,
                longDescription
        );

        // When & Then
        assertThatThrownBy(() -> request.validate())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REPORT_DESCRIPTION_TOO_LONG);
    }

    @Test
    @DisplayName("존재하지 않는 경매 신고 불가")
    void reportAuction_AuctionNotFound() {
        // Given
        ReportAuctionRequest request = new ReportAuctionRequest(
                ReportReason.SPAM,
                "스팸"
        );

        given(auctionRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reportService.reportAuction(999L, 2L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUCTION_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 신고 불가")
    void reportAuction_UserNotFound() {
        // Given
        ReportAuctionRequest request = new ReportAuctionRequest(
                ReportReason.SPAM,
                "스팸"
        );

        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reportService.reportAuction(1L, 999L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }
}
