package com.cherrypick.app.domain.report.controller;

import com.cherrypick.app.domain.report.dto.ReportAuctionRequest;
import com.cherrypick.app.domain.report.dto.ReportResponse;
import com.cherrypick.app.domain.report.service.ReportService;
import com.cherrypick.app.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 경매 신고 API 컨트롤러
 */
@Tag(name = "경매 신고", description = "경매 신고 및 관리 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;

    @Operation(summary = "경매 신고",
               description = """
                   경매를 신고합니다.

                   **신고 사유:**
                   - INAPPROPRIATE_CONTENT: 부적절한 콘텐츠
                   - SPAM: 스팸/도배
                   - FRAUD: 사기/허위매물
                   - DUPLICATE_POSTING: 중복 게시
                   - PROHIBITED_ITEM: 판매금지 품목
                   - COPYRIGHT_VIOLATION: 저작권 침해
                   - OTHER: 기타 (상세 설명 필수)

                   **주의사항:**
                   - 본인의 경매는 신고할 수 없습니다
                   - 동일 경매에 대해 중복 신고할 수 없습니다
                   - OTHER 선택 시 description 필수 (500자 이내)

                   **사용 예시:**
                   ```json
                   {
                     "reason": "SPAM",
                     "description": "같은 내용을 반복해서 게시합니다."
                   }
                   ```
                   """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "신고 접수 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (OTHER 사유 시 description 누락, 500자 초과)"),
            @ApiResponse(responseCode = "403", description = "본인 경매 신고 불가"),
            @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 신고한 경매"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/auctions/{auctionId}/reports")
    public ResponseEntity<ReportResponse> reportAuction(
            @Parameter(description = "경매 ID") @PathVariable Long auctionId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReportAuctionRequest request) {

        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        ReportResponse response = reportService.reportAuction(auctionId, userId, request);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 신고 내역 조회",
               description = "로그인한 사용자가 신고한 내역을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/reports/my")
    public ResponseEntity<Page<ReportResponse>> getMyReports(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        Pageable pageable = PageRequest.of(page, size);
        Page<ReportResponse> reports = reportService.getMyReports(userId, pageable);

        return ResponseEntity.ok(reports);
    }

    @Operation(summary = "신고 상세 조회",
               description = "특정 신고의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "신고를 찾을 수 없음")
    })
    @GetMapping("/reports/{reportId}")
    public ResponseEntity<ReportResponse> getReportDetail(
            @Parameter(description = "신고 ID") @PathVariable Long reportId) {

        ReportResponse response = reportService.getReportDetail(reportId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "경매의 신고 수 조회",
               description = "특정 경매에 접수된 신고 수를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/auctions/{auctionId}/reports/count")
    public ResponseEntity<Map<String, Long>> getReportCount(
            @Parameter(description = "경매 ID") @PathVariable Long auctionId) {

        long count = reportService.getReportCount(auctionId);
        return ResponseEntity.ok(Map.of("reportCount", count));
    }
}
