package com.cherrypick.app.domain.user.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 소액 거래 활성화 보너스 서비스
 * - 월 20회 이상 소액 거래 보너스
 * - 다양한 카테고리 거래 보너스
 * - 연속 성공 거래 보너스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExperienceBonusService {
    
    private final JdbcTemplate jdbcTemplate;
    
    // 보너스 기준값
    private static final int MONTHLY_SMALL_TRADE_THRESHOLD = 20;
    private static final int DIVERSE_CATEGORY_THRESHOLD = 3;
    private static final int CONSECUTIVE_TRADE_THRESHOLD = 5;
    private static final BigDecimal SMALL_TRADE_AMOUNT = BigDecimal.valueOf(50000); // 5만원 미만을 소액으로 정의
    
    // 보너스 경험치
    private static final int MONTHLY_SMALL_TRADE_BONUS = 20;
    private static final int DIVERSITY_BONUS = 10;
    private static final int CONSECUTIVE_TRADE_BONUS = 5;
    
    /**
     * 사용자의 보너스 경험치 계산
     */
    public int calculateBonusExperience(User user, BigDecimal transactionAmount, Auction auction) {
        int totalBonus = 0;
        
        // 1. 월 소액 거래 보너스 체크
        if (isEligibleForMonthlySmallTradeBonus(user.getId(), transactionAmount)) {
            totalBonus += MONTHLY_SMALL_TRADE_BONUS;
            log.info("월 소액 거래 보너스 적용 - 사용자: {}, +{} EXP", user.getId(), MONTHLY_SMALL_TRADE_BONUS);
        }
        
        // 2. 다양한 카테고리 거래 보너스 체크
        if (isEligibleForDiversityBonus(user.getId(), auction)) {
            totalBonus += DIVERSITY_BONUS;
            log.info("다양한 카테고리 거래 보너스 적용 - 사용자: {}, +{} EXP", user.getId(), DIVERSITY_BONUS);
        }
        
        // 3. 연속 성공 거래 보너스 체크
        if (isEligibleForConsecutiveTradeBonus(user.getId())) {
            totalBonus += CONSECUTIVE_TRADE_BONUS;
            log.info("연속 성공 거래 보너스 적용 - 사용자: {}, +{} EXP", user.getId(), CONSECUTIVE_TRADE_BONUS);
        }
        
        return totalBonus;
    }
    
    /**
     * 월 20회 이상 소액 거래 보너스 체크
     */
    private boolean isEligibleForMonthlySmallTradeBonus(Long userId, BigDecimal transactionAmount) {
        // 현재 거래가 소액 거래인지 확인
        if (transactionAmount.compareTo(SMALL_TRADE_AMOUNT) >= 0) {
            return false; // 5만원 이상은 소액 거래가 아님
        }
        
        // 이번 달 소액 거래 횟수 조회
        String sql = """
            SELECT COUNT(*) FROM auctions a
            JOIN transactions t ON a.id = t.auction_id
            WHERE (a.seller_id = ? OR a.winner_id = ?)
            AND t.status = 'COMPLETED'
            AND t.final_price < ?
            AND t.completed_at >= DATE_TRUNC('month', CURRENT_DATE)
            AND t.completed_at < DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month'
            """;
        
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, 
            userId, userId, SMALL_TRADE_AMOUNT);
        
        // 이번 거래가 20번째 소액 거래인지 확인
        return count != null && count == MONTHLY_SMALL_TRADE_THRESHOLD - 1;
    }
    
    /**
     * 다양한 카테고리 거래 보너스 체크
     */
    private boolean isEligibleForDiversityBonus(Long userId, Auction auction) {
        // 최근 30일간 거래한 카테고리 수 조회
        String sql = """
            SELECT COUNT(DISTINCT a.category) FROM auctions a
            JOIN transactions t ON a.id = t.auction_id
            WHERE (a.seller_id = ? OR a.winner_id = ?)
            AND t.status = 'COMPLETED'
            AND t.completed_at >= CURRENT_DATE - INTERVAL '30 days'
            """;
        
        Integer categoryCount = jdbcTemplate.queryForObject(sql, Integer.class, userId, userId);
        
        // 3개 이상 카테고리에서 거래했고, 현재 거래가 새로운 카테고리인지 확인
        if (categoryCount != null && categoryCount >= DIVERSE_CATEGORY_THRESHOLD) {
            // 현재 카테고리로 최근 거래한 적이 있는지 확인
            String recentCategorySql = """
                SELECT COUNT(*) FROM auctions a
                JOIN transactions t ON a.id = t.auction_id
                WHERE (a.seller_id = ? OR a.winner_id = ?)
                AND t.status = 'COMPLETED'
                AND a.category = ?
                AND t.completed_at >= CURRENT_DATE - INTERVAL '7 days'
                """;
            
            Integer recentCategoryCount = jdbcTemplate.queryForObject(recentCategorySql, Integer.class, 
                userId, userId, auction.getCategory().name());
            
            // 최근 7일간 이 카테고리로 거래한 적이 없다면 다양성 보너스 지급
            return recentCategoryCount != null && recentCategoryCount == 0;
        }
        
        return false;
    }
    
    /**
     * 연속 성공 거래 보너스 체크 (5회 연속)
     */
    private boolean isEligibleForConsecutiveTradeBonus(Long userId) {
        // 최근 거래 5개의 상태 조회 (현재 거래 포함)
        String sql = """
            SELECT COUNT(*) FROM (
                SELECT t.status FROM auctions a
                JOIN transactions t ON a.id = t.auction_id
                WHERE (a.seller_id = ? OR a.winner_id = ?)
                AND t.status = 'COMPLETED'
                ORDER BY t.completed_at DESC
                LIMIT ?
            ) recent_transactions
            """;
        
        Integer consecutiveCount = jdbcTemplate.queryForObject(sql, Integer.class, 
            userId, userId, CONSECUTIVE_TRADE_THRESHOLD);
        
        // 5회 연속 성공 거래 달성시 보너스 지급
        return consecutiveCount != null && consecutiveCount == CONSECUTIVE_TRADE_THRESHOLD;
    }
    
    /**
     * 사용자별 월간 통계 조회 (관리용)
     */
    public MonthlyBonusStats getMonthlyBonusStats(Long userId) {
        // 이번 달 소액 거래 수
        String smallTradeSql = """
            SELECT COUNT(*) FROM auctions a
            JOIN transactions t ON a.id = t.auction_id
            WHERE (a.seller_id = ? OR a.winner_id = ?)
            AND t.status = 'COMPLETED'
            AND t.final_price < ?
            AND t.completed_at >= DATE_TRUNC('month', CURRENT_DATE)
            """;
        
        Integer smallTradeCount = jdbcTemplate.queryForObject(smallTradeSql, Integer.class, 
            userId, userId, SMALL_TRADE_AMOUNT);
        
        // 최근 30일간 카테고리 수
        String categorySql = """
            SELECT COUNT(DISTINCT a.category) FROM auctions a
            JOIN transactions t ON a.id = t.auction_id
            WHERE (a.seller_id = ? OR a.winner_id = ?)
            AND t.status = 'COMPLETED'
            AND t.completed_at >= CURRENT_DATE - INTERVAL '30 days'
            """;
        
        Integer categoryCount = jdbcTemplate.queryForObject(categorySql, Integer.class, userId, userId);
        
        // 최근 연속 성공 거래 수
        String consecutiveSql = """
            SELECT COUNT(*) FROM (
                SELECT t.status FROM auctions a
                JOIN transactions t ON a.id = t.auction_id
                WHERE (a.seller_id = ? OR a.winner_id = ?)
                AND t.status = 'COMPLETED'
                ORDER BY t.completed_at DESC
                LIMIT 10
            ) recent_transactions
            """;
        
        Integer consecutiveCount = jdbcTemplate.queryForObject(consecutiveSql, Integer.class, userId, userId);
        
        return new MonthlyBonusStats(
            smallTradeCount != null ? smallTradeCount : 0,
            categoryCount != null ? categoryCount : 0,
            consecutiveCount != null ? consecutiveCount : 0
        );
    }
}

/**
 * 월간 보너스 통계
 */
class MonthlyBonusStats {
    private final int smallTradeCount;
    private final int categoryCount;
    private final int consecutiveTradeCount;
    
    public MonthlyBonusStats(int smallTradeCount, int categoryCount, int consecutiveTradeCount) {
        this.smallTradeCount = smallTradeCount;
        this.categoryCount = categoryCount;
        this.consecutiveTradeCount = consecutiveTradeCount;
    }
    
    public int getSmallTradeCount() { return smallTradeCount; }
    public int getCategoryCount() { return categoryCount; }
    public int getConsecutiveTradeCount() { return consecutiveTradeCount; }
    
    public boolean isEligibleForSmallTradeBonus() {
        return smallTradeCount >= 20;
    }
    
    public boolean isEligibleForDiversityBonus() {
        return categoryCount >= 3;
    }
    
    public boolean isEligibleForConsecutiveBonus() {
        return consecutiveTradeCount >= 5;
    }
}