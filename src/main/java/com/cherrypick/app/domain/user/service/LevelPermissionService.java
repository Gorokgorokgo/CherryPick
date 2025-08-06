package com.cherrypick.app.domain.user.service;

import com.cherrypick.app.domain.user.dto.response.LevelPermissionResponse;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 레벨 기반 권한 관리 서비스
 * - 보증금 시스템 제거 후 레벨 시스템으로 신뢰도 관리
 * - 단계별 권한 및 혜택 제공
 * - 저레벨 사용자 보호 및 고레벨 사용자 우대
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LevelPermissionService {
    
    private final UserRepository userRepository;
    
    // 레벨별 권한 임계값
    private static final int NOVICE_THRESHOLD = 20;     // 초보자 구간
    private static final int INTERMEDIATE_THRESHOLD = 40; // 중급자 구간
    private static final int ADVANCED_THRESHOLD = 60;    // 숙련자 구간
    private static final int EXPERT_THRESHOLD = 80;      // 고수 구간
    private static final int MASTER_THRESHOLD = 95;      // 마스터 구간
    
    // 입찰 제한 없음 (보증금 시스템 제거로 인해)
    
    /**
     * 경매 등록 권한 확인
     */
    public boolean canCreateAuction(Long userId) {
        User user = getUserById(userId);
        
        // 모든 레벨에서 경매 등록 가능 (보증금 시스템 제거로 진입장벽 낮춤)
        return true;
    }
    
    /**
     * 입찰 권한 확인 (제한 없음)
     */
    public LevelPermissionResponse checkBiddingPermission(Long userId, BigDecimal bidAmount) {
        User user = getUserById(userId);
        
        int buyerLevel = user.getBuyerLevel();
        
        // 보증금 시스템 제거로 입찰 제한 없음
        return LevelPermissionResponse.builder()
                .userId(userId)
                .buyerLevel(buyerLevel)
                .sellerLevel(user.getSellerLevel())
                .canBid(true) // 항상 입찰 가능
                .maxBidAmount(BigDecimal.valueOf(Long.MAX_VALUE)) // 무제한
                .levelTier(getLevelTier(buyerLevel))
                .permissions(getUserPermissions(user))
                .benefits(getUserBenefits(user))
                .build();
    }
    
    /**
     * 연결 서비스 수수료 할인율 조회
     */
    public double getConnectionFeeDiscount(Long sellerId) {
        User seller = getUserById(sellerId);
        int sellerLevel = seller.getSellerLevel();
        
        if (sellerLevel >= MASTER_THRESHOLD) { // 81-100레벨
            return 0.4; // 40% 할인 (마스터+)
        } else if (sellerLevel >= EXPERT_THRESHOLD) { // 61-80레벨
            return 0.3; // 30% 할인 (고수)
        } else if (sellerLevel >= ADVANCED_THRESHOLD) { // 41-60레벨
            return 0.2; // 20% 할인 (숙련자)
        } else if (sellerLevel >= INTERMEDIATE_THRESHOLD) { // 21-40레벨
            return 0.1; // 10% 할인 (성장자)
        }
        
        return 0.0; // 할인 없음 (0-20레벨 초보자)
    }
    
    /**
     * 레벨 등급 조회
     */
    private String getLevelTier(int level) {
        if (level <= NOVICE_THRESHOLD) {
            return "🟢 초보 (Lv 1-20)";
        } else if (level <= INTERMEDIATE_THRESHOLD) {
            return "🟡 성장 (Lv 21-40)";
        } else if (level <= ADVANCED_THRESHOLD) {
            return "🟠 숙련 (Lv 41-60)";
        } else if (level <= EXPERT_THRESHOLD) {
            return "🔴 고수 (Lv 61-80)";
        } else if (level <= MASTER_THRESHOLD) {
            return "🟣 마스터 (Lv 81-95)";
        } else {
            return "⚫ 레전드 (Lv 96-100)";
        }
    }
    
    /**
     * 사용자 권한 목록 조회
     */
    private String[] getUserPermissions(User user) {
        int buyerLevel = user.getBuyerLevel();
        int sellerLevel = user.getSellerLevel();
        int maxLevel = Math.max(buyerLevel, sellerLevel);
        
        if (maxLevel <= NOVICE_THRESHOLD) {
            return new String[]{
                "무료 입찰 참여",
                "기본 경매 등록",
                "기본 채팅 기능"
            };
        } else if (maxLevel <= INTERMEDIATE_THRESHOLD) {
            return new String[]{
                "무료 입찰 참여",
                "경매 등록",
                "채팅 기능",
                "연결 수수료 10% 할인"
            };
        } else if (maxLevel <= ADVANCED_THRESHOLD) {
            return new String[]{
                "무료 입찰 참여",
                "프리미엄 경매 등록",
                "우선 채팅 연결",
                "연결 수수료 20% 할인"
            };
        } else if (maxLevel <= EXPERT_THRESHOLD) {
            return new String[]{
                "무료 입찰 참여",
                "VIP 경매 등록",
                "우선 고객 지원",
                "연결 수수료 30% 할인"
            };
        } else {
            return new String[]{
                "무료 입찰 참여",
                "최우선 고객 지원",
                "연결 수수료 40% 할인",
                "베타 기능 우선 체험"
            };
        }
    }
    
    /**
     * 사용자 혜택 목록 조회
     */
    private String[] getUserBenefits(User user) {
        int buyerLevel = user.getBuyerLevel();
        int sellerLevel = user.getSellerLevel();
        int maxLevel = Math.max(buyerLevel, sellerLevel);
        
        if (maxLevel <= NOVICE_THRESHOLD) {
            return new String[]{
                "신규 사용자 보호",
                "레벨업 가이드 제공"
            };
        } else if (maxLevel <= INTERMEDIATE_THRESHOLD) {
            return new String[]{
                "거래 우선권",
                "월간 통계 리포트"
            };
        } else if (maxLevel <= ADVANCED_THRESHOLD) {
            return new String[]{
                "프리미엄 뱃지 표시",
                "거래 신뢰도 우대",
                "특별 이벤트 초대"
            };
        } else if (maxLevel <= EXPERT_THRESHOLD) {
            return new String[]{
                "VIP 뱃지 표시",
                "전용 고객센터",
                "수수료 할인 혜택"
            };
        } else {
            return new String[]{
                "레전드/마스터 뱃지",
                "최고 우선순위",
                "특별 혜택 패키지",
                "커뮤니티 리더 권한"
            };
        }
    }
    
    /**
     * 사용자 조회 헬퍼 메서드
     */
    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
    
    /**
     * 사용자 레벨 권한 정보 조회
     */
    public LevelPermissionResponse getUserPermissionInfo(Long userId) {
        User user = getUserById(userId);
        
        return LevelPermissionResponse.builder()
                .userId(userId)
                .buyerLevel(user.getBuyerLevel())
                .sellerLevel(user.getSellerLevel())
                .canBid(true)
                .maxBidAmount(BigDecimal.valueOf(Long.MAX_VALUE)) // 무제한
                .levelTier(getLevelTier(Math.max(user.getBuyerLevel(), user.getSellerLevel())))
                .permissions(getUserPermissions(user))
                .benefits(getUserBenefits(user))
                .connectionFeeDiscount(getConnectionFeeDiscount(userId))
                .build();
    }
    
    /**
     * 레벨업 권한 업그레이드 안내
     */
    public String getLevelUpGuidance(int currentLevel, int newLevel) {
        if (newLevel > MASTER_THRESHOLD && currentLevel <= MASTER_THRESHOLD) {
            return "🎉 레전드 등급 달성! 모든 기능 무제한 이용 + 최대 50% 수수료 할인";
        } else if (newLevel > EXPERT_THRESHOLD && currentLevel <= EXPERT_THRESHOLD) {
            return "🎉 마스터 등급 달성! VIP 서비스 + 30% 수수료 할인";
        } else if (newLevel > ADVANCED_THRESHOLD && currentLevel <= ADVANCED_THRESHOLD) {
            return "🎉 고수 등급 달성! 무제한 입찰 + 20% 수수료 할인";
        } else if (newLevel > INTERMEDIATE_THRESHOLD && currentLevel <= INTERMEDIATE_THRESHOLD) {
            return "🎉 숙련자 등급 달성! 고액 입찰 가능 + 10% 수수료 할인";
        } else if (newLevel > NOVICE_THRESHOLD && currentLevel <= NOVICE_THRESHOLD) {
            return "🎉 성장 등급 달성! 일반 사용자 권한 획득";
        }
        
        return "레벨업 완료! 계속해서 경험을 쌓아보세요.";
    }
}