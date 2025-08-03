package com.cherrypick.app.domain.user.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 경험치 조작 방지 검증 서비스
 * - 같은 사용자 간 거래 빈도 체크
 * - 의심스러운 패턴 탐지
 * - 거래 시간 검증
 * - 금액 기반 조작 탐지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExperienceValidationService {
    
    private final JdbcTemplate jdbcTemplate;
    
    // 조작 방지 임계값
    private static final int MAX_DAILY_TRANSACTIONS_SAME_USER = 3;
    private static final int MIN_TRANSACTION_TIME_MINUTES = 5;
    private static final BigDecimal SUSPICIOUS_LOW_AMOUNT = BigDecimal.valueOf(1000);
    private static final int MAX_CONSECUTIVE_LOW_AMOUNT = 10;
    
    /**
     * 거래 검증 및 경험치 배율 계산
     */
    public ExperienceValidationResult validateTransaction(User buyer, User seller, 
                                                        BigDecimal amount, LocalDateTime transactionTime, 
                                                        Auction auction) {
        
        // 1. 같은 사용자 간 반복 거래 체크
        if (checkSameUserFrequentTrading(buyer.getId(), seller.getId())) {
            return ExperienceValidationResult.blocked("같은 사용자 간 1일 3회 이상 거래");
        }
        
        // 2. 거래 완료 시간 체크 (너무 빨리 완료되면 의심)
        if (checkTransactionTimeToFast(auction.getCreatedAt(), transactionTime)) {
            return ExperienceValidationResult.blocked("거래 완료까지 5분 미만으로 너무 빠름");
        }
        
        // 3. 1,000원 미만 거래는 경험치 90% 감소
        if (amount.compareTo(SUSPICIOUS_LOW_AMOUNT) < 0) {
            log.warn("소액 거래 탐지 - 구매자: {}, 판매자: {}, 금액: {}", 
                buyer.getId(), seller.getId(), amount);
            return ExperienceValidationResult.withMultiplier(0.1, 0.1, "1,000원 미만 소액 거래");
        }
        
        // 4. 연속 소액 거래 패턴 체크
        if (checkConsecutiveLowAmountPattern(buyer.getId(), seller.getId())) {
            return ExperienceValidationResult.withMultiplier(0.3, 0.3, "연속 소액 거래 패턴");
        }
        
        // 5. 같은 사용자 간 거래 빈도에 따른 경험치 감소
        int recentTransactionCount = getRecentTransactionCount(buyer.getId(), seller.getId());
        if (recentTransactionCount >= 2) {
            double multiplier = recentTransactionCount >= 5 ? 0.1 : 0.5;
            return ExperienceValidationResult.withMultiplier(multiplier, multiplier, 
                "같은 상대방과 반복 거래 (최근 " + recentTransactionCount + "회)");
        }
        
        // 모든 검증 통과
        return ExperienceValidationResult.passed();
    }
    
    /**
     * 같은 사용자 간 1일 거래 빈도 체크
     */
    private boolean checkSameUserFrequentTrading(Long buyerId, Long sellerId) {
        String sql = """
            SELECT COUNT(*) FROM auctions 
            WHERE ((seller_id = ? AND winner_id = ?) OR (seller_id = ? AND winner_id = ?))
            AND status = 'COMPLETED'
            AND updated_at >= NOW() - INTERVAL 1 DAY
            """;
        
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, 
            sellerId, buyerId, buyerId, sellerId);
        
        return count != null && count >= MAX_DAILY_TRANSACTIONS_SAME_USER;
    }
    
    /**
     * 거래 완료 시간이 너무 빠른지 체크
     */
    private boolean checkTransactionTimeToFast(LocalDateTime auctionCreated, LocalDateTime transactionTime) {
        long minutesBetween = ChronoUnit.MINUTES.between(auctionCreated, transactionTime);
        return minutesBetween < MIN_TRANSACTION_TIME_MINUTES;
    }
    
    /**
     * 연속 소액 거래 패턴 체크
     */
    private boolean checkConsecutiveLowAmountPattern(Long buyerId, Long sellerId) {
        String sql = """
            SELECT COUNT(*) FROM auctions 
            WHERE ((seller_id = ? AND winner_id = ?) OR (seller_id = ? AND winner_id = ?))
            AND status = 'COMPLETED'
            AND final_price < ?
            AND updated_at >= NOW() - INTERVAL 7 DAY
            ORDER BY updated_at DESC
            LIMIT ?
            """;
        
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, 
            sellerId, buyerId, buyerId, sellerId, 
            SUSPICIOUS_LOW_AMOUNT, MAX_CONSECUTIVE_LOW_AMOUNT);
        
        return count != null && count >= MAX_CONSECUTIVE_LOW_AMOUNT;
    }
    
    /**
     * 최근 같은 사용자 간 거래 횟수 조회
     */
    private int getRecentTransactionCount(Long buyerId, Long sellerId) {
        String sql = """
            SELECT COUNT(*) FROM auctions 
            WHERE ((seller_id = ? AND winner_id = ?) OR (seller_id = ? AND winner_id = ?))
            AND status = 'COMPLETED'
            AND updated_at >= NOW() - INTERVAL 30 DAY
            """;
        
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, 
            sellerId, buyerId, buyerId, sellerId);
        
        return count != null ? count : 0;
    }
    
    /**
     * IP 기반 중복 계정 체크 (추후 구현)
     */
    public boolean checkSameIPMultipleAccounts(String ipAddress) {
        // TODO: IP 기반 중복 계정 탐지 로직 구현
        return false;
    }
    
    /**
     * GPS 위치 정보 체크 (추후 구현)
     */
    public boolean checkGPSLocation(Long userId, String location) {
        // TODO: GPS 위치 정보 기반 검증 로직 구현
        return true;
    }
}

/**
 * 경험치 검증 결과
 */
class ExperienceValidationResult {
    private final boolean blocked;
    private final double buyerMultiplier;
    private final double sellerMultiplier;
    private final String reason;
    
    private ExperienceValidationResult(boolean blocked, double buyerMultiplier, 
                                     double sellerMultiplier, String reason) {
        this.blocked = blocked;
        this.buyerMultiplier = buyerMultiplier;
        this.sellerMultiplier = sellerMultiplier;
        this.reason = reason;
    }
    
    public static ExperienceValidationResult blocked(String reason) {
        return new ExperienceValidationResult(true, 0.0, 0.0, reason);
    }
    
    public static ExperienceValidationResult withMultiplier(double buyerMultiplier, 
                                                          double sellerMultiplier, String reason) {
        return new ExperienceValidationResult(false, buyerMultiplier, sellerMultiplier, reason);
    }
    
    public static ExperienceValidationResult passed() {
        return new ExperienceValidationResult(false, 1.0, 1.0, "정상 거래");
    }
    
    public boolean isBlocked() { return blocked; }
    public double getBuyerMultiplier() { return buyerMultiplier; }
    public double getSellerMultiplier() { return sellerMultiplier; }
    public String getBlockReason() { return reason; }
}