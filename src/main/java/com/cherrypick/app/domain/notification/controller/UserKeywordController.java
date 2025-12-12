package com.cherrypick.app.domain.notification.controller;

import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.notification.dto.request.CreateKeywordRequest;
import com.cherrypick.app.domain.notification.dto.response.UserKeywordResponse;
import com.cherrypick.app.domain.notification.entity.UserKeyword;
import com.cherrypick.app.domain.notification.repository.UserKeywordRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자 키워드 알림 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications/keywords")
@RequiredArgsConstructor
@Tag(name = "User Keywords", description = "키워드 알림 관리 API")
public class UserKeywordController {

    private final UserKeywordRepository userKeywordRepository;
    private final UserRepository userRepository;

    private static final int MAX_KEYWORDS_PER_USER = 10;

    @Operation(summary = "내 키워드 목록 조회")
    @GetMapping
    public ResponseEntity<List<UserKeywordResponse>> getMyKeywords(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        List<UserKeyword> keywords = userKeywordRepository.findByUserId(userId);

        List<UserKeywordResponse> responses = keywords.stream()
                .map(UserKeywordResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "키워드 추가")
    @PostMapping
    public ResponseEntity<UserKeywordResponse> addKeyword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateKeywordRequest request) {

        Long userId = Long.parseLong(userDetails.getUsername());

        // 키워드 수 제한 확인
        long currentCount = userKeywordRepository.countByUserIdAndIsActiveTrue(userId);
        if (currentCount >= MAX_KEYWORDS_PER_USER) {
            throw new IllegalStateException("키워드는 최대 " + MAX_KEYWORDS_PER_USER + "개까지 등록 가능합니다.");
        }

        // 중복 키워드 확인
        String normalizedKeyword = request.getKeyword().toLowerCase().trim();
        if (userKeywordRepository.findByUserIdAndKeyword(userId, normalizedKeyword).isPresent()) {
            throw new IllegalStateException("이미 등록된 키워드입니다: " + request.getKeyword());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        UserKeyword keyword;
        if (request.getCategory() != null) {
            keyword = UserKeyword.create(user, normalizedKeyword, request.getCategory());
        } else {
            keyword = UserKeyword.create(user, normalizedKeyword);
        }

        UserKeyword savedKeyword = userKeywordRepository.save(keyword);
        log.info("키워드 등록: userId={}, keyword={}, category={}",
                userId, normalizedKeyword, request.getCategory());

        return ResponseEntity.ok(UserKeywordResponse.from(savedKeyword));
    }

    @Operation(summary = "키워드 삭제")
    @DeleteMapping("/{keywordId}")
    public ResponseEntity<Void> deleteKeyword(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long keywordId) {

        Long userId = Long.parseLong(userDetails.getUsername());

        UserKeyword keyword = userKeywordRepository.findById(keywordId)
                .orElseThrow(() -> new IllegalArgumentException("키워드를 찾을 수 없습니다."));

        if (!keyword.getUser().getId().equals(userId)) {
            throw new IllegalStateException("본인의 키워드만 삭제할 수 있습니다.");
        }

        userKeywordRepository.delete(keyword);
        log.info("키워드 삭제: userId={}, keywordId={}", userId, keywordId);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "키워드 활성화/비활성화")
    @PatchMapping("/{keywordId}/toggle")
    public ResponseEntity<UserKeywordResponse> toggleKeyword(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long keywordId) {

        Long userId = Long.parseLong(userDetails.getUsername());

        UserKeyword keyword = userKeywordRepository.findById(keywordId)
                .orElseThrow(() -> new IllegalArgumentException("키워드를 찾을 수 없습니다."));

        if (!keyword.getUser().getId().equals(userId)) {
            throw new IllegalStateException("본인의 키워드만 수정할 수 있습니다.");
        }

        keyword.setActive(!keyword.getIsActive());
        UserKeyword savedKeyword = userKeywordRepository.save(keyword);

        log.info("키워드 상태 변경: userId={}, keywordId={}, isActive={}",
                userId, keywordId, savedKeyword.getIsActive());

        return ResponseEntity.ok(UserKeywordResponse.from(savedKeyword));
    }

    @Operation(summary = "키워드 수정")
    @PutMapping("/{keywordId}")
    public ResponseEntity<UserKeywordResponse> updateKeyword(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long keywordId,
            @Valid @RequestBody CreateKeywordRequest request) {

        Long userId = Long.parseLong(userDetails.getUsername());

        UserKeyword keyword = userKeywordRepository.findById(keywordId)
                .orElseThrow(() -> new IllegalArgumentException("키워드를 찾을 수 없습니다."));

        if (!keyword.getUser().getId().equals(userId)) {
            throw new IllegalStateException("본인의 키워드만 수정할 수 있습니다.");
        }

        String normalizedKeyword = request.getKeyword().toLowerCase().trim();
        keyword.updateKeyword(normalizedKeyword);
        keyword.updateCategory(request.getCategory());

        UserKeyword savedKeyword = userKeywordRepository.save(keyword);
        log.info("키워드 수정: userId={}, keywordId={}, newKeyword={}, category={}",
                userId, keywordId, normalizedKeyword, request.getCategory());

        return ResponseEntity.ok(UserKeywordResponse.from(savedKeyword));
    }
}
