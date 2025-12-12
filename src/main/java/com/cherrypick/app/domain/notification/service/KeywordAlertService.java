package com.cherrypick.app.domain.notification.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.notification.entity.UserKeyword;
import com.cherrypick.app.domain.notification.event.KeywordAlertEvent;
import com.cherrypick.app.domain.notification.repository.UserKeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 키워드 알림 서비스
 * 경매 생성 시 비동기로 키워드 매칭 및 알림 발송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordAlertService {

    private final UserKeywordRepository userKeywordRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationThrottleService throttleService;

    /**
     * 경매 생성 시 키워드 알림 처리 (비동기)
     * API 응답 시간에 영향을 주지 않도록 비동기 처리
     */
    @Async
    @Transactional(readOnly = true)
    public void processKeywordAlerts(Auction auction) {
        log.info("🔔 [키워드 알림 처리 시작] auctionId={}, title={}, category={}",
                auction.getId(), auction.getTitle(), auction.getCategory());

        try {
            // 1. 경매 제목에서 키워드 추출 및 매칭
            List<UserKeyword> matchedKeywords = findMatchingKeywords(
                    auction.getTitle(),
                    auction.getCategory()
            );

            if (matchedKeywords.isEmpty()) {
                log.debug("매칭된 키워드 없음: auctionId={}", auction.getId());
                return;
            }

            log.info("매칭된 키워드 {}개 발견: auctionId={}", matchedKeywords.size(), auction.getId());

            // 2. 중복 사용자 제거 (한 사용자에게 하나의 알림만)
            Set<Long> notifiedUserIds = new HashSet<>();

            for (UserKeyword userKeyword : matchedKeywords) {
                Long userId = userKeyword.getUser().getId();

                // 판매자 본인 제외
                if (userId.equals(auction.getSeller().getId())) {
                    continue;
                }

                // 이미 알림 발송한 사용자 제외
                if (notifiedUserIds.contains(userId)) {
                    continue;
                }

                // Throttle 확인 (같은 경매에 대해 중복 알림 방지)
                if (!throttleService.canSendKeywordNotification(userId, auction.getId())) {
                    log.debug("Throttled: userId={}, auctionId={}", userId, auction.getId());
                    continue;
                }

                // 알림 이벤트 발행
                try {
                    eventPublisher.publishEvent(new KeywordAlertEvent(
                            this,
                            userId,
                            auction.getId(),
                            auction.getTitle(),
                            userKeyword.getKeyword(),
                            auction.getStartPrice().longValue(),
                            auction.getCategory().name()
                    ));
                    notifiedUserIds.add(userId);
                    log.debug("키워드 알림 이벤트 발행: userId={}, keyword={}", userId, userKeyword.getKeyword());
                } catch (Exception e) {
                    log.warn("키워드 알림 이벤트 발행 실패: userId={}, error={}", userId, e.getMessage());
                }
            }

            log.info("키워드 알림 발송 완료: auctionId={}, 알림 대상 {}명", auction.getId(), notifiedUserIds.size());

        } catch (Exception e) {
            log.error("키워드 알림 처리 실패: auctionId={}, error={}", auction.getId(), e.getMessage(), e);
        }
    }

    /**
     * 경매 제목과 매칭되는 사용자 키워드 조회
     */
    private List<UserKeyword> findMatchingKeywords(String auctionTitle, Category category) {
        // 제목을 소문자로 변환하여 검색
        String lowerTitle = auctionTitle.toLowerCase();

        // 활성화된 모든 키워드 중 제목에 포함된 것 조회
        return userKeywordRepository.findMatchingKeywords(lowerTitle, category);
    }

    /**
     * 단일 키워드로 매칭되는 사용자 조회 (테스트용)
     */
    @Transactional(readOnly = true)
    public List<UserKeyword> findUsersWithKeyword(String keyword, Category category) {
        return userKeywordRepository.findByKeywordAndCategory(keyword.toLowerCase(), category);
    }
}
