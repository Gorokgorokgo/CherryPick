package com.cherrypick.app.domain.qna.controller;

import com.cherrypick.app.domain.qna.dto.request.CreateAnswerRequest;
import com.cherrypick.app.domain.qna.dto.request.CreateQuestionRequest;
import com.cherrypick.app.domain.qna.dto.request.UpdateAnswerRequest;
import com.cherrypick.app.domain.qna.dto.request.UpdateQuestionRequest;
import com.cherrypick.app.domain.qna.dto.response.AnswerResponse;
import com.cherrypick.app.domain.qna.dto.response.QuestionResponse;
import com.cherrypick.app.domain.qna.service.QnaService;
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

@Tag(name = "7단계 - Q&A 관리", description = "경매 상품 문의 및 답변 관리 | 판매자-구매자 소통")
@RestController
@RequestMapping("/api/auctions/{auctionId}/qna")
@RequiredArgsConstructor
public class QnaController {

    private final QnaService qnaService;
    private final UserService userService;

    @Operation(summary = "질문 등록", description = "특정 경매에 질문을 등록합니다. 자신의 경매에는 질문할 수 없으며, 경매 종료 30분 전부터는 질문이 제한됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "질문 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (자신의 경매, 종료된 경매 등)"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/questions")
    public ResponseEntity<QuestionResponse> createQuestion(
            @PathVariable Long auctionId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateQuestionRequest request) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        QuestionResponse response = qnaService.createQuestion(auctionId, userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "질문 수정", description = "작성한 질문을 수정합니다. 답변이 달린 후에는 수정할 수 없습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "질문 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (답변 달린 질문, 작성자 불일치 등)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음")
    })
    @PutMapping("/questions/{questionId}")
    public ResponseEntity<QuestionResponse> updateQuestion(
            @PathVariable Long auctionId,
            @PathVariable Long questionId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateQuestionRequest request) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        QuestionResponse response = qnaService.updateQuestion(questionId, userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "질문 삭제", description = "작성한 질문을 삭제합니다. 답변이 달린 후에는 삭제할 수 없습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "질문 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (답변 달린 질문, 작성자 불일치 등)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음")
    })
    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable Long auctionId,
            @PathVariable Long questionId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        qnaService.deleteQuestion(questionId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "답변 등록", description = "판매자가 질문에 답변을 등록합니다. 해당 경매의 판매자만 답변할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "답변 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (판매자 아님, 이미 답변 존재 등)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음")
    })
    @PostMapping("/questions/{questionId}/answer")
    public ResponseEntity<AnswerResponse> createAnswer(
            @PathVariable Long auctionId,
            @PathVariable Long questionId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateAnswerRequest request) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        AnswerResponse response = qnaService.createAnswer(questionId, userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "답변 수정", description = "판매자가 작성한 답변을 수정합니다. 경매 진행 중에만 수정 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "답변 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (작성자 불일치, 경매 종료 등)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "답변을 찾을 수 없음")
    })
    @PutMapping("/answers/{answerId}")
    public ResponseEntity<AnswerResponse> updateAnswer(
            @PathVariable Long auctionId,
            @PathVariable Long answerId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateAnswerRequest request) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        AnswerResponse response = qnaService.updateAnswer(answerId, userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "답변 삭제", description = "판매자가 작성한 답변을 삭제합니다. 경매 진행 중에만 삭제 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "답변 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (작성자 불일치, 경매 종료 등)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "답변을 찾을 수 없음")
    })
    @DeleteMapping("/answers/{answerId}")
    public ResponseEntity<Void> deleteAnswer(
            @PathVariable Long auctionId,
            @PathVariable Long answerId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        qnaService.deleteAnswer(answerId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Q&A 목록 조회", description = "특정 경매의 Q&A 목록을 페이지네이션으로 조회합니다. 질문과 답변이 함께 조회됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Q&A 목록 조회 성공"),
            @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음")
    })
    @GetMapping
    public ResponseEntity<Page<QuestionResponse>> getQnaList(
            @PathVariable Long auctionId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Long currentUserId = null;
        if (userDetails != null) {
            currentUserId = userService.getUserIdByEmail(userDetails.getUsername());
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<QuestionResponse> response = qnaService.getQnaByAuction(auctionId, currentUserId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Q&A 통계 조회", description = "특정 경매의 Q&A 통계 정보를 조회합니다. (총 질문 수, 답변되지 않은 질문 수 등)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Q&A 통계 조회 성공"),
            @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음")
    })
    @GetMapping("/statistics")
    public ResponseEntity<QnaService.QnaStatistics> getQnaStatistics(
            @PathVariable Long auctionId) {
        
        QnaService.QnaStatistics statistics = qnaService.getQnaStatistics(auctionId);
        return ResponseEntity.ok(statistics);
    }
}