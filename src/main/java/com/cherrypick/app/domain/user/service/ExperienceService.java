package com.cherrypick.app.domain.user.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.user.dto.response.LevelProgressResponse;
import com.cherrypick.app.domain.user.dto.response.UserLevelInfoResponse;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 경험치 계산 및 지급 서비스
 * - 거래 금액 기반 경험치 계산 (기본 80 EXP + 보너스)
 * - 조작 방지 검증
 * - 소액 거래 활성화 보너스
 * - 현실적인 레벨링 시스템 (Lv 50: 1-2년, Lv 100: 12-15년)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExperienceService {
    
    private final UserRepository userRepository;
    private final ExperienceValidationService validationService;
    private final ExperienceBonusService bonusService;
    
    // 기본 경험치 (조작 방지를 위해 입찰 참여 경험치 제거)
    private static final int BASE_TRANSACTION_EXP = 80; // 거래 완료시 기본 경험치
    private static final int AUCTION_WIN_EXP = 80; // 낙찰 성공시 경험치
    
    /**
     * 사용자 친화적인 레벨업 테이블 
     * Lv 50: 27,000 EXP (활발한 유저 기준 10개월)
     * Lv 80: 85,000 EXP (활발한 유저 기준 2.5년) 
     * Lv 100: 180,000 EXP (활발한 유저 기준 6.6년)
     */
    private static final int[] LEVEL_THRESHOLDS = {
        // 🟢 초보 구간 (Lv 1-20): 빠른 성장으로 동기부여 (1-2개월)
        0, 100, 250, 450, 700, 1000, 1350, 1750, 2200, 2700, // Lv 1-10
        3250, 3850, 4500, 5200, 5950, 6750, 7600, 8500, 9450, 10450, // Lv 11-20
        
        // 🟡 성장 구간 (Lv 21-40): 안정적 증가 (3-6개월)
        11500, 12600, 13750, 14950, 16200, 17500, 18850, 20250, 21700, 23200, // Lv 21-30
        24750, 26350, 27000, 27700, 28450, 29250, 30100, 31000, 31950, 32950, // Lv 31-40
        
        // 🟠 숙련 구간 (Lv 41-60): 의미있는 진전 (6개월-1.5년)
        34000, 35100, 36250, 37450, 38700, 40000, 41350, 42750, 44200, 45700, // Lv 41-50
        47250, 48900, 50650, 52500, 54450, 56500, 58650, 60900, 63250, 65700, // Lv 51-60
        
        // 🔴 고수 구간 (Lv 61-80): 도전적인 증가 (1.5-3년)
        68250, 71000, 73950, 77100, 80450, 84000, 87750, 91700, 95850, 100200, // Lv 61-70
        104750, 109600, 114750, 120200, 125950, 132000, 138350, 145000, 151950, 159200, // Lv 71-80
        
        // 🟣 마스터 구간 (Lv 81-95): 엘리트 전용 (3-5년)
        166750, 174700, 183050, 191800, 200950, 210500, 220450, 230800, 241550, 252700, // Lv 81-90
        264250, 276300, 288850, 301900, 315450, // Lv 91-95
        
        // ⚫ 레전드 구간 (Lv 96-100): 최상위 (5-7년)
        329500, 344200, 359500, 375400, 392000 // Lv 96-100
    };
    
    /**
     * 거래 완료시 경험치 지급 (구매자/판매자)
     */
    @Transactional
    public void awardTransactionExperience(Long buyerId, Long sellerId, BigDecimal transactionAmount, 
                                         LocalDateTime transactionTime, Auction auction) {
        User buyer = userRepository.findById(buyerId)
            .orElseThrow(() -> new IllegalArgumentException("구매자를 찾을 수 없습니다."));
        User seller = userRepository.findById(sellerId)
            .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));
        
        // 조작 방지 검증
        ExperienceValidationResult validation = validationService.validateTransaction(
            buyer, seller, transactionAmount, transactionTime, auction);
        
        if (validation.isBlocked()) {
            log.warn("거래 차단됨 - 구매자: {}, 판매자: {}, 사유: {}", 
                buyerId, sellerId, validation.getBlockReason());
            return;
        }
        
        // 기본 경험치 계산 (80 EXP + 금액별 보너스)
        int buyerBaseExp = calculateTransactionExperience(transactionAmount, validation.getBuyerMultiplier());
        int sellerBaseExp = calculateTransactionExperience(transactionAmount, validation.getSellerMultiplier());
        
        // 소액 거래 활성화 보너스 계산
        int buyerBonusExp = bonusService.calculateBonusExperience(buyer, transactionAmount, auction);
        int sellerBonusExp = bonusService.calculateBonusExperience(seller, transactionAmount, auction);
        
        // 총 경험치 계산
        int totalBuyerExp = buyerBaseExp + buyerBonusExp;
        int totalSellerExp = sellerBaseExp + sellerBonusExp;
        
        // 경험치 지급
        awardBuyerExperience(buyer, totalBuyerExp);
        awardSellerExperience(seller, totalSellerExp);
        
        log.info("거래 완료 경험치 지급 - 구매자: {}(기본{}+보너스{}={} EXP), 판매자: {}(기본{}+보너스{}={} EXP)", 
            buyerId, buyerBaseExp, buyerBonusExp, totalBuyerExp, 
            sellerId, sellerBaseExp, sellerBonusExp, totalSellerExp);
    }
    
    /**
     * 거래 금액 기반 경험치 계산
     * 기본 80 EXP + 금액별 보너스
     */
    private int calculateTransactionExperience(BigDecimal amount, double multiplier) {
        int baseExp = BASE_TRANSACTION_EXP; // 모든 거래 기본 80 EXP
        int bonusExp = 0;
        
        // 금액별 보너스 계산
        if (amount.compareTo(BigDecimal.valueOf(10000)) >= 0 && 
            amount.compareTo(BigDecimal.valueOf(50000)) < 0) {
            bonusExp = 10; // 10,000원~49,999원: +10 EXP 보너스
        } else if (amount.compareTo(BigDecimal.valueOf(50000)) >= 0 && 
                   amount.compareTo(BigDecimal.valueOf(100000)) < 0) {
            bonusExp = 25; // 50,000원~99,999원: +25 EXP 보너스
        } else if (amount.compareTo(BigDecimal.valueOf(100000)) >= 0 && 
                   amount.compareTo(BigDecimal.valueOf(500000)) < 0) {
            bonusExp = 40; // 100,000원~499,999원: +40 EXP 보너스
        } else if (amount.compareTo(BigDecimal.valueOf(500000)) >= 0) {
            bonusExp = 50; // 500,000원 이상: +50 EXP 보너스
        }
        
        int totalExp = baseExp + bonusExp;
        
        // 조작 방지 가중치 적용
        return (int) (totalExp * multiplier);
    }
    
    /**
     * 구매자 경험치 지급
     */
    @Transactional
    public void awardBuyerExperience(User buyer, int experience) {
        int newExp = buyer.getBuyerExp() + experience;
        buyer.setBuyerExp(newExp);
        
        // 레벨업 체크
        checkBuyerLevelUp(buyer);
        
        userRepository.save(buyer);
    }
    
    /**
     * 판매자 경험치 지급
     */
    @Transactional
    public void awardSellerExperience(User seller, int experience) {
        int newExp = seller.getSellerExp() + experience;
        seller.setSellerExp(newExp);
        
        // 레벨업 체크
        checkSellerLevelUp(seller);
        
        userRepository.save(seller);
    }
    
    /**
     * 현실적인 레벨업 체크 (구매자)
     */
    private void checkBuyerLevelUp(User user) {
        int currentLevel = user.getBuyerLevel();
        int currentExp = user.getBuyerExp();
        int requiredExp = getRequiredExperienceForLevel(currentLevel + 1);
        
        if (currentExp >= requiredExp && currentLevel < 100) {
            user.setBuyerLevel(currentLevel + 1);
            log.info("🎉 구매자 레벨업! 사용자: {}, 새 레벨: {}", user.getId(), currentLevel + 1);
        }
    }
    
    /**
     * 현실적인 레벨업 체크 (판매자)
     */
    private void checkSellerLevelUp(User user) {
        int currentLevel = user.getSellerLevel();
        int currentExp = user.getSellerExp();
        int requiredExp = getRequiredExperienceForLevel(currentLevel + 1);
        
        if (currentExp >= requiredExp && currentLevel < 100) {
            user.setSellerLevel(currentLevel + 1);
            log.info("🎉 판매자 레벨업! 사용자: {}, 새 레벨: {}", user.getId(), currentLevel + 1);
        }
    }
    
    /**
     * 현실적인 레벨업 테이블 조회
     */
    private int getRequiredExperienceForLevel(int level) {
        if (level <= 1) return 0;
        if (level > LEVEL_THRESHOLDS.length) return Integer.MAX_VALUE;
        return LEVEL_THRESHOLDS[level - 1];
    }
    
    /**
     * 낙찰 성공 경험치 지급
     */
    @Transactional
    public void awardAuctionWinExperience(Long buyerId, Long sellerId) {
        User buyer = userRepository.findById(buyerId)
            .orElseThrow(() -> new IllegalArgumentException("구매자를 찾을 수 없습니다."));
        User seller = userRepository.findById(sellerId)
            .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));
        
        awardBuyerExperience(buyer, AUCTION_WIN_EXP);
        awardSellerExperience(seller, AUCTION_WIN_EXP);
    }
    
    /**
     * 구매자 레벨 진행률 조회 (심리적 배려)
     */
    public LevelProgressResponse getBuyerLevelProgress(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        int currentLevel = user.getBuyerLevel();
        int currentExp = user.getBuyerExp();
        int requiredExp = getRequiredExperienceForLevel(currentLevel + 1);
        
        return LevelProgressResponse.createForLevel(currentLevel, currentExp, requiredExp);
    }
    
    /**
     * 판매자 레벨 진행률 조회 (심리적 배려)
     */
    public LevelProgressResponse getSellerLevelProgress(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        int currentLevel = user.getSellerLevel();
        int currentExp = user.getSellerExp();
        int requiredExp = getRequiredExperienceForLevel(currentLevel + 1);
        
        return LevelProgressResponse.createForLevel(currentLevel, currentExp, requiredExp);
    }
    
    /**
     * 종합 레벨 정보 조회
     */
    public UserLevelInfoResponse getUserLevelInfo(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        LevelProgressResponse buyerProgress = getBuyerLevelProgress(userId);
        LevelProgressResponse sellerProgress = getSellerLevelProgress(userId);

        return UserLevelInfoResponse.create(userId, buyerProgress, sellerProgress);
    }

    /**
     * 후기 작성 보너스 경험치 지급
     *
     * @param userId 후기 작성자 ID
     */
    @Transactional
    public void awardReviewBonus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        int bonusExp = 10; // 후기 작성 보너스: +10 EXP

        // 구매자와 판매자 경험치 모두에 보너스 지급 (후기는 거래 행위이므로)
        int newBuyerExp = user.getBuyerExp() + bonusExp;
        int newSellerExp = user.getSellerExp() + bonusExp;

        user.setBuyerExp(newBuyerExp);
        user.setSellerExp(newSellerExp);

        // 레벨업 체크
        checkBuyerLevelUp(user);
        checkSellerLevelUp(user);

        userRepository.save(user);

        log.info("후기 작성 보너스 경험치 지급 - userId: {}, bonus: {} EXP", userId, bonusExp);
    }
}