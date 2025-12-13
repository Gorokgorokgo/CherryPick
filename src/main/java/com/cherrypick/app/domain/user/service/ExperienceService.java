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
    private static final int SELLER_BASE_BONUS_EXP = 70; // 판매자 밸런스 패치용 추가 경험치
    private static final int AUCTION_WIN_EXP = 80; // 낙찰 성공시 경험치
    
    /**
     * 사용자 친화적인 레벨업 테이블 (개선된 버전)
     * Lv 50: 35,000 EXP (헤비 유저 기준 1.5년)
     * Lv 100: 100,000 EXP (헤비 유저 기준 3.5년)
     */
    private static final int[] LEVEL_THRESHOLDS = {
        // 🟢 입문 구간 (Lv 1-10): 1~2회 거래로 1업 (총 3,000)
        0, 300, 600, 900, 1200, 1500, 1800, 2100, 2400, 2700,
        
        // 🟡 초보 구간 (Lv 11-30): 3~4회 거래로 1업 (총 15,000)
        3300, 3900, 4500, 5100, 5700, 6300, 6900, 7500, 8100, 8700, // Lv 11-20
        9300, 9900, 10500, 11100, 11700, 12300, 12900, 13500, 14100, 14700, // Lv 21-30
        
        // 🟠 숙련 구간 (Lv 31-50): 1,000 EXP 단위 (총 35,000)
        15700, 16700, 17700, 18700, 19700, 20700, 21700, 22700, 23700, 24700, // Lv 31-40
        25700, 26700, 27700, 28700, 29700, 30700, 31700, 32700, 33700, 34700, // Lv 41-50
        
        // 🔴 전문 구간 (Lv 51-70): 1,250 EXP 단위 (총 60,000)
        35950, 37200, 38450, 39700, 40950, 42200, 43450, 44700, 45950, 47200, // Lv 51-60
        48450, 49700, 50950, 52200, 53450, 54700, 55950, 57200, 58450, 59700, // Lv 61-70
        
        // 🟣 장인 구간 (Lv 71-90): 1,250 EXP 단위 (총 85,000)
        60950, 62200, 63450, 64700, 65950, 67200, 68450, 69700, 70950, 72200, // Lv 71-80
        73450, 74700, 75950, 77200, 78450, 79700, 80950, 82200, 83450, 84700, // Lv 81-90
        
        // ⚫ 전설 구간 (Lv 91-100): 1,500 EXP 단위 (총 100,000)
        86200, 87700, 89200, 90700, 92200, 93700, 95200, 96700, 98200, 99700 // Lv 91-100
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
        // 판매자는 밸런스를 위해 기본 보너스 추가 지급
        int sellerBaseExp = calculateTransactionExperience(transactionAmount, validation.getSellerMultiplier()) + SELLER_BASE_BONUS_EXP;

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
     * - 구매자는 구매자 경험치만, 판매자는 판매자 경험치만 증가
     *
     * @param userId 후기 작성자 ID
     * @param isSeller 작성자가 판매자인지 여부
     * @return 경험치 획득 응답
     */
    @Transactional
    public ExperienceGainResponse awardReviewBonus(Long userId, boolean isSeller) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        int bonusExp = 10; // 후기 작성 보너스: +10 EXP
        
        // 이전 상태 및 결과 변수 초기화
        int expBefore;
        int expAfter;
        int levelBefore;
        int levelAfter;
        boolean isLevelUp;
        ExperienceHistory.ExperienceType expType;
        
        if (isSeller) {
            // 판매자 경험치 증가
            expType = ExperienceHistory.ExperienceType.SELLER;
            expBefore = user.getSellerExp();
            levelBefore = user.getSellerLevel();
            
            expAfter = expBefore + bonusExp;
            user.setSellerExp(expAfter);
            
            isLevelUp = checkSellerLevelUp(user);
            levelAfter = user.getSellerLevel();
        } else {
            // 구매자 경험치 증가
            expType = ExperienceHistory.ExperienceType.BUYER;
            expBefore = user.getBuyerExp();
            levelBefore = user.getBuyerLevel();
            
            expAfter = expBefore + bonusExp;
            user.setBuyerExp(expAfter);
            
            isLevelUp = checkBuyerLevelUp(user);
            levelAfter = user.getBuyerLevel();
        }

        userRepository.save(user);

        // 경험치 히스토리 저장
        ExperienceHistory history = ExperienceHistory.builder()
                .user(user)
                .type(expType)
                .expGained(bonusExp)
                .expBefore(expBefore)
                .expAfter(expAfter)
                .levelBefore(levelBefore)
                .levelAfter(levelAfter)
                .isLevelUp(isLevelUp)
                .reason("후기 작성")
                .reasonDetail("거래 후기 작성 보너스")
                .notificationSent(false)
                .build();

        experienceHistoryRepository.save(history);

        // 응답 생성
        int requiredExp = getRequiredExperienceForLevel(levelAfter + 1);
        ExperienceGainResponse response = ExperienceGainResponse.create(
            expType,
            bonusExp,
            expBefore,
            expAfter,
            levelBefore,
            levelAfter,
            isLevelUp,
            "후기 작성",
            "거래 후기 작성 보너스",
            requiredExp
        );

        // 이벤트 발행 (비동기 알림 처리)
        eventPublisher.publishEvent(new ExperienceGainEvent(this, response, user.getId()));

        log.info("후기 작성 보너스 경험치 지급 - userId: {}, role: {}, bonus: {} EXP", 
                userId, isSeller ? "SELLER" : "BUYER", bonusExp);

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