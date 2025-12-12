package com.cherrypick.app.domain.notification.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.auction.repository.AuctionBookmarkRepository;
import com.cherrypick.app.domain.bid.repository.BidRepository;
import com.cherrypick.app.domain.notification.event.AuctionEndingSoonEvent;
import com.cherrypick.app.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 경매 마감 임박 알림 스케줄러
 * 15분 전, 5분 전 관심 경매(찜/입찰한) 사용자에게 알림 발송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EndingSoonNotificationScheduler {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AuctionBookmarkRepository bookmarkRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    /**
     * 15분 전 마감 임박 알림 스케줄러
     * 매 분 실행하여 13-17분 후 종료 경매 조회 (스케줄러 지연 대비 범위 확대)
     */
    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    @Transactional(readOnly = true)
    public void processEndingSoon15MinNotifications() {
        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);
        // 스케줄러 지연이나 실행 시간 차이로 인한 누락 방지를 위해 범위 확대 (1분 -> 4분)
        // 중복 발송은 NotificationEventListener의 ThrottleService에서 방지됨
        LocalDateTime targetStart = now.plusMinutes(13);
        LocalDateTime targetEnd = now.plusMinutes(17);

        processEndingSoonNotifications(targetStart, targetEnd, 15);
    }

    /**
     * 5분 전 마감 임박 알림 스케줄러
     * 매 분 실행하여 3-7분 후 종료 경매 조회 (스케줄러 지연 대비 범위 확대)
     */
    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    @Transactional(readOnly = true)
    public void processEndingSoon5MinNotifications() {
        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);
        // 스케줄러 지연이나 실행 시간 차이로 인한 누락 방지를 위해 범위 확대 (1분 -> 4분)
        // 중복 발송은 NotificationEventListener의 ThrottleService에서 방지됨
        LocalDateTime targetStart = now.plusMinutes(3);
        LocalDateTime targetEnd = now.plusMinutes(7);

        processEndingSoonNotifications(targetStart, targetEnd, 5);
    }

    /**
     * 마감 임박 알림 처리
     */
    private void processEndingSoonNotifications(LocalDateTime startTime, LocalDateTime endTime, int minutesRemaining) {
        log.info("📢 마감 임박 알림 스케줄러 실행: {}분 전, 범위: {} ~ {}",
                minutesRemaining, startTime, endTime);

        // 해당 시간대에 종료되는 활성 경매 조회
        // Repository 쿼리: endAt BETWEEN :now AND :endTime
        Page<Auction> endingSoonAuctions = auctionRepository.findEndingSoon(
                startTime, endTime, PageRequest.of(0, 100));

        if (endingSoonAuctions.isEmpty()) {
            log.debug("마감 임박 경매 없음 ({}분 전)", minutesRemaining);
            return;
        }

        log.info("마감 임박 경매 {}개 발견 ({}분 전)", endingSoonAuctions.getTotalElements(), minutesRemaining);

        for (Auction auction : endingSoonAuctions) {
            try {
                notifyInterestedUsers(auction, minutesRemaining);
            } catch (Exception e) {
                log.error("마감 임박 알림 처리 실패: auctionId={}, error={}",
                        auction.getId(), e.getMessage());
            }
        }
    }

    /**
     * 관심 사용자에게 알림 발송 (입찰자 + 찜한 사용자)
     */
    private void notifyInterestedUsers(Auction auction, int minutesRemaining) {
        Set<Long> notifiedUserIds = new HashSet<>();

        // 1. 입찰자들 조회
        List<User> bidders = bidRepository.findDistinctBiddersByAuctionId(auction.getId());
        for (User bidder : bidders) {
            // 판매자 제외
            if (!bidder.getId().equals(auction.getSeller().getId())) {
                notifiedUserIds.add(bidder.getId());
            }
        }

        // 2. 찜한 사용자들 조회
        List<Long> bookmarkedUserIds = bookmarkRepository.findUserIdsByAuctionId(auction.getId());
        for (Long userId : bookmarkedUserIds) {
            // 판매자 제외
            if (!userId.equals(auction.getSeller().getId())) {
                notifiedUserIds.add(userId);
            }
        }

        if (notifiedUserIds.isEmpty()) {
            log.debug("관심 사용자 없음: auctionId={}", auction.getId());
            return;
        }

        log.info("마감 임박 알림 발송: auctionId={}, {}분 전, 대상 {}명",
                auction.getId(), minutesRemaining, notifiedUserIds.size());

        // 3. 각 사용자에게 알림 이벤트 발행
        for (Long userId : notifiedUserIds) {
            try {
                eventPublisher.publishEvent(new AuctionEndingSoonEvent(
                        this,
                        userId,
                        auction.getId(),
                        auction.getTitle(),
                        auction.getCurrentPrice().longValue(),
                        minutesRemaining
                ));
            } catch (Exception e) {
                log.warn("마감 임박 알림 이벤트 발행 실패: userId={}, auctionId={}, error={}",
                        userId, auction.getId(), e.getMessage());
            }
        }
    }
}
