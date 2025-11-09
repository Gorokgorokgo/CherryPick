package com.cherrypick.app.domain.user.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.user.dto.response.ExperienceGainResponse;
import com.cherrypick.app.domain.user.dto.response.ExperienceHistoryResponse;
import com.cherrypick.app.domain.user.dto.response.LevelProgressResponse;
import com.cherrypick.app.domain.user.dto.response.UserLevelInfoResponse;
import com.cherrypick.app.domain.user.entity.ExperienceHistory;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.event.ExperienceGainEvent;
import com.cherrypick.app.domain.user.repository.ExperienceHistoryRepository;
import com.cherrypick.app.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final ExperienceHistoryRepository experienceHistoryRepository;
    private final ExperienceValidationService validationService;
    private final ExperienceBonusService bonusService;
    private final ApplicationEventPublisher eventPublisher;
    
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
     * 거래 완료 경험치 지급 결과
     */
    public static class TransactionExperienceResult {
        private final ExperienceGainResponse buyerExperience;
        private final ExperienceGainResponse sellerExperience;

        public TransactionExperienceResult(ExperienceGainResponse buyerExperience, ExperienceGainResponse sellerExperience) {
            this.buyerExperience = buyerExperience;
            this.sellerExperience = sellerExperience;
        }

        public ExperienceGainResponse getBuyerExperience() {
            return buyerExperience;
        }

        public ExperienceGainResponse getSellerExperience() {
            return sellerExperience;
        }
    }

    /**
     * 거래 완료시 경험치 지급 (구매자/판매자)
     */
    @Transactional
    public TransactionExperienceResult awardTransactionExperience(Long buyerId, Long sellerId, BigDecimal transactionAmount,
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
            return null;
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

        // 경험치 지급 및 응답 수집
        ExperienceGainResponse buyerResponse = awardBuyerExperienceWithReason(buyer, totalBuyerExp, "거래 완료", null);
        ExperienceGainResponse sellerResponse = awardSellerExperienceWithReason(seller, totalSellerExp, "거래 완료", null);

        log.info("거래 완료 경험치 지급 - 구매자: {}(기본{}+보너스{}={} EXP), 판매자: {}(기본{}+보너스{}={} EXP)",
            buyerId, buyerBaseExp, buyerBonusExp, totalBuyerExp,
            sellerId, sellerBaseExp, sellerBonusExp, totalSellerExp);

        return new TransactionExperienceResult(buyerResponse, sellerResponse);
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
        awardBuyerExperienceWithReason(buyer, experience, "거래 완료", null);
    }

    /**
     * 구매자 경험치 지급 (사유 포함)
     */
    @Transactional
    public ExperienceGainResponse awardBuyerExperienceWithReason(User buyer, int experience, String reason, String reasonDetail) {
        int expBefore = buyer.getBuyerExp();
        int levelBefore = buyer.getBuyerLevel();

        int newExp = expBefore + experience;
        buyer.setBuyerExp(newExp);

        // 레벨업 체크
        checkBuyerLevelUp(buyer);

        int levelAfter = buyer.getBuyerLevel();
        boolean isLevelUp = levelAfter > levelBefore;

        userRepository.save(buyer);

        // 히스토리 저장
        ExperienceHistory history = ExperienceHistory.builder()
            .user(buyer)
            .type(ExperienceHistory.ExperienceType.BUYER)
            .expGained(experience)
            .expBefore(expBefore)
            .expAfter(newExp)
            .levelBefore(levelBefore)
            .levelAfter(levelAfter)
            .isLevelUp(isLevelUp)
            .reason(reason)
            .reasonDetail(reasonDetail)
            .notificationSent(false)
            .build();

        experienceHistoryRepository.save(history);

        // 응답 생성
        int requiredExp = getRequiredExperienceForLevel(levelAfter + 1);
        ExperienceGainResponse response = ExperienceGainResponse.create(
            ExperienceHistory.ExperienceType.BUYER,
            experience,
            expBefore,
            newExp,
            levelBefore,
            levelAfter,
            isLevelUp,
            reason,
            reasonDetail,
            requiredExp
        );

        // 이벤트 발행 (비동기 알림 처리)
        eventPublisher.publishEvent(new ExperienceGainEvent(this, response, buyer.getId()));

        return response;
    }
    
    /**
     * 판매자 경험치 지급
     */
    @Transactional
    public void awardSellerExperience(User seller, int experience) {
        awardSellerExperienceWithReason(seller, experience, "거래 완료", null);
    }

    /**
     * 판매자 경험치 지급 (사유 포함)
     */
    @Transactional
    public ExperienceGainResponse awardSellerExperienceWithReason(User seller, int experience, String reason, String reasonDetail) {
        int expBefore = seller.getSellerExp();
        int levelBefore = seller.getSellerLevel();

        int newExp = expBefore + experience;
        seller.setSellerExp(newExp);

        // 레벨업 체크
        checkSellerLevelUp(seller);

        int levelAfter = seller.getSellerLevel();
        boolean isLevelUp = levelAfter > levelBefore;

        userRepository.save(seller);

        // 히스토리 저장
        ExperienceHistory history = ExperienceHistory.builder()
            .user(seller)
            .type(ExperienceHistory.ExperienceType.SELLER)
            .expGained(experience)
            .expBefore(expBefore)
            .expAfter(newExp)
            .levelBefore(levelBefore)
            .levelAfter(levelAfter)
            .isLevelUp(isLevelUp)
            .reason(reason)
            .reasonDetail(reasonDetail)
            .notificationSent(false)
            .build();

        experienceHistoryRepository.save(history);

        // 응답 생성
        int requiredExp = getRequiredExperienceForLevel(levelAfter + 1);
        ExperienceGainResponse response = ExperienceGainResponse.create(
            ExperienceHistory.ExperienceType.SELLER,
            experience,
            expBefore,
            newExp,
            levelBefore,
            levelAfter,
            isLevelUp,
            reason,
            reasonDetail,
            requiredExp
        );

        // 이벤트 발행 (비동기 알림 처리)
        eventPublisher.publishEvent(new ExperienceGainEvent(this, response, seller.getId()));

        return response;
    }

    /**
     * 낙찰 성공 경험치 지급
     *
     * @param winnerId 낙찰자 ID
     * @param auction 경매 정보
     * @return 경험치 획득 응답
     */
    @Transactional
    public ExperienceGainResponse awardAuctionWinExperience(Long winnerId, Auction auction) {
        User winner = userRepository.findById(winnerId)
            .orElseThrow(() -> new IllegalArgumentException("낙찰자를 찾을 수 없습니다."));

        // 낙찰 성공 경험치 지급 (구매자 경험치로 지급)
        int experience = AUCTION_WIN_EXP;
        String reason = "낙찰 성공";
        String reasonDetail = String.format("경매 '%s' 낙찰", auction.getTitle());

        log.info("낙찰 성공 경험치 지급 - 사용자: {}, 경매: {}, 경험치: {} EXP",
            winnerId, auction.getId(), experience);

        return awardBuyerExperienceWithReason(winner, experience, reason, reasonDetail);
    }
    
    /**
     * 현실적인 레벨업 체크 (구매자)
     * @return 레벨업 여부
     */
    private boolean checkBuyerLevelUp(User user) {
        int currentLevel = user.getBuyerLevel();
        int currentExp = user.getBuyerExp();
        int requiredExp = getRequiredExperienceForLevel(currentLevel + 1);

        if (currentExp >= requiredExp && currentLevel < 100) {
            user.setBuyerLevel(currentLevel + 1);
            log.info("🎉 구매자 레벨업! 사용자: {}, 새 레벨: {}", user.getId(), currentLevel + 1);
            return true;
        }
        return false;
    }

    /**
     * 현실적인 레벨업 체크 (판매자)
     * @return 레벨업 여부
     */
    private boolean checkSellerLevelUp(User user) {
        int currentLevel = user.getSellerLevel();
        int currentExp = user.getSellerExp();
        int requiredExp = getRequiredExperienceForLevel(currentLevel + 1);

        if (currentExp >= requiredExp && currentLevel < 100) {
            user.setSellerLevel(currentLevel + 1);
            log.info("🎉 판매자 레벨업! 사용자: {}, 새 레벨: {}", user.getId(), currentLevel + 1);
            return true;
        }
        return false;
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
     * @return 경험치 획득 응답 (buyer 타입으로 반환)
     */
    @Transactional
    public ExperienceGainResponse awardReviewBonus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        int bonusExp = 10; // 후기 작성 보너스: +10 EXP

        // 이전 상태 저장
        int buyerExpBefore = user.getBuyerExp();
        int sellerExpBefore = user.getSellerExp();
        int buyerLevelBefore = user.getBuyerLevel();
        int sellerLevelBefore = user.getSellerLevel();

        // 구매자와 판매자 경험치 모두에 보너스 지급 (후기는 거래 행위이므로)
        int newBuyerExp = user.getBuyerExp() + bonusExp;
        int newSellerExp = user.getSellerExp() + bonusExp;

        user.setBuyerExp(newBuyerExp);
        user.setSellerExp(newSellerExp);

        // 레벨업 체크
        boolean buyerLevelUp = checkBuyerLevelUp(user);
        boolean sellerLevelUp = checkSellerLevelUp(user);

        userRepository.save(user);

        // 경험치 히스토리 저장 (buyer 타입으로)
        ExperienceHistory history = ExperienceHistory.builder()
                .user(user)
                .type(ExperienceHistory.ExperienceType.BUYER)
                .expGained(bonusExp)
                .expBefore(buyerExpBefore)
                .expAfter(newBuyerExp)
                .levelBefore(buyerLevelBefore)
                .levelAfter(user.getBuyerLevel())
                .isLevelUp(buyerLevelUp)
                .reason("후기 작성")
                .reasonDetail("거래 후기 작성 보너스")
                .notificationSent(false)
                .build();

        experienceHistoryRepository.save(history);

        // 응답 생성 (buyer 타입으로 반환)
        int requiredExp = getRequiredExperienceForLevel(user.getBuyerLevel() + 1);
        ExperienceGainResponse response = ExperienceGainResponse.create(
            ExperienceHistory.ExperienceType.BUYER,
            bonusExp,
            buyerExpBefore,
            newBuyerExp,
            buyerLevelBefore,
            user.getBuyerLevel(),
            buyerLevelUp,
            "후기 작성",
            "거래 후기 작성 보너스",
            requiredExp
        );

        // 이벤트 발행 (비동기 알림 처리)
        eventPublisher.publishEvent(new ExperienceGainEvent(this, response, user.getId()));

        log.info("후기 작성 보너스 경험치 지급 - userId: {}, bonus: {} EXP", userId, bonusExp);

        return response;
    }

    /**
     * 경험치 히스토리 조회 (페이징)
     */
    public Page<ExperienceHistoryResponse> getExperienceHistory(Long userId, Pageable pageable) {
        Page<ExperienceHistory> historyPage = experienceHistoryRepository.findByUserIdWithUser(userId, pageable);
        return historyPage.map(ExperienceHistoryResponse::from);
    }
}