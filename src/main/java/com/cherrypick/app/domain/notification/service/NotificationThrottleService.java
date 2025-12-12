package com.cherrypick.app.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 알림 Throttling 서비스 (Redis 기반)
 * 동일한 사용자에게 짧은 시간 내 중복 알림 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationThrottleService {

    private final RedisTemplate<String, String> redisTemplate;

    // Throttle 키 패턴
    private static final String OUTBID_THROTTLE_KEY = "notification:outbid:%d:%d"; // userId:auctionId
    private static final String ENDING_SOON_THROTTLE_KEY = "notification:ending_soon:%d:%d:%s"; // userId:auctionId:type
    private static final String KEYWORD_THROTTLE_KEY = "notification:keyword:%d:%d"; // userId:auctionId

    // Throttle 기간 (초)
    private static final long OUTBID_THROTTLE_SECONDS = 60; // 1분
    private static final long ENDING_SOON_THROTTLE_SECONDS = 300; // 5분 (같은 경매에 대해 중복 방지)
    private static final long KEYWORD_THROTTLE_SECONDS = 3600; // 1시간

    /**
     * Outbid 알림 Throttle 확인
     * @param userId 사용자 ID
     * @param auctionId 경매 ID
     * @return true면 알림 발송 가능, false면 throttled
     */
    public boolean canSendOutbidNotification(Long userId, Long auctionId) {
        String key = String.format(OUTBID_THROTTLE_KEY, userId, auctionId);
        return checkAndSetThrottle(key, OUTBID_THROTTLE_SECONDS);
    }

    /**
     * Outbid Throttle 카운트 증가 (그룹 알림용)
     * @param userId 사용자 ID
     * @param auctionId 경매 ID
     * @return 현재까지의 입찰 횟수
     */
    public int incrementOutbidCount(Long userId, Long auctionId) {
        String countKey = String.format(OUTBID_THROTTLE_KEY + ":count", userId, auctionId);
        Long count = redisTemplate.opsForValue().increment(countKey);
        redisTemplate.expire(countKey, Duration.ofSeconds(OUTBID_THROTTLE_SECONDS));
        return count != null ? count.intValue() : 1;
    }

    /**
     * Outbid Throttle 카운트 리셋 (알림 발송 후)
     */
    public void resetOutbidCount(Long userId, Long auctionId) {
        String countKey = String.format(OUTBID_THROTTLE_KEY + ":count", userId, auctionId);
        redisTemplate.delete(countKey);
    }

    /**
     * Outbid Throttle 카운트 조회
     */
    public int getOutbidCount(Long userId, Long auctionId) {
        String countKey = String.format(OUTBID_THROTTLE_KEY + ":count", userId, auctionId);
        String value = redisTemplate.opsForValue().get(countKey);
        return value != null ? Integer.parseInt(value) : 0;
    }

    /**
     * 마감 임박 알림 Throttle 확인
     * @param userId 사용자 ID
     * @param auctionId 경매 ID
     * @param type 알림 유형 (15m/5m)
     * @return true면 알림 발송 가능, false면 throttled
     */
    public boolean canSendEndingSoonNotification(Long userId, Long auctionId, String type) {
        String key = String.format(ENDING_SOON_THROTTLE_KEY, userId, auctionId, type);
        return checkAndSetThrottle(key, ENDING_SOON_THROTTLE_SECONDS);
    }

    /**
     * 키워드 알림 Throttle 확인
     * @param userId 사용자 ID
     * @param auctionId 경매 ID
     * @return true면 알림 발송 가능, false면 throttled
     */
    public boolean canSendKeywordNotification(Long userId, Long auctionId) {
        String key = String.format(KEYWORD_THROTTLE_KEY, userId, auctionId);
        return checkAndSetThrottle(key, KEYWORD_THROTTLE_SECONDS);
    }

    /**
     * Throttle 확인 및 설정
     * @param key Redis 키
     * @param ttlSeconds TTL (초)
     * @return true면 새로 설정됨 (발송 가능), false면 이미 존재 (throttled)
     */
    private boolean checkAndSetThrottle(String key, long ttlSeconds) {
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(ttlSeconds));
        if (isNew != null && isNew) {
            log.debug("Throttle 키 설정: {} (TTL: {}초)", key, ttlSeconds);
            return true;
        }
        log.debug("Throttle 적용됨: {}", key);
        return false;
    }

    /**
     * Throttle 수동 해제 (테스트용)
     */
    public void clearThrottle(String pattern) {
        // 패턴에 해당하는 키 삭제
        var keys = redisTemplate.keys("notification:" + pattern + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Throttle 키 {}개 삭제됨", keys.size());
        }
    }
}
