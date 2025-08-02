package com.cherrypick.app.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 레벨 진행률 응답 DTO
 * 레벨대별 차등 정보 제공으로 심리적 부담 완화
 */
@Schema(description = "레벨 진행률 정보")
@Getter
@Builder
public class LevelProgressResponse {
    
    @Schema(description = "현재 레벨", example = "15")
    private Integer currentLevel;
    
    @Schema(description = "진행률 표시 타입", example = "DETAILED")
    private ProgressDisplayType displayType;
    
    @Schema(description = "진행률 퍼센트 (0-100)", example = "80")
    private Integer progressPercent;
    
    @Schema(description = "현재 경험치 (낮은 레벨만 표시)", example = "1200")
    private Integer currentExp;
    
    @Schema(description = "다음 레벨 필요 경험치 (낮은 레벨만 표시)", example = "1500")
    private Integer requiredExp;
    
    @Schema(description = "남은 경험치 (낮은 레벨만 표시)", example = "300")
    private Integer remainingExp;
    
    @Schema(description = "진행 상태 메시지", example = "다음 레벨까지 300 EXP")
    private String progressMessage;
    
    @Schema(description = "레벨 구간 설명", example = "성장 구간")
    private String levelTierName;
    
    @Schema(description = "진행 바 표시용 (10단계)", example = "8")
    private Integer progressBarLevel;
    
    /**
     * 진행률 표시 타입
     */
    public enum ProgressDisplayType {
        DETAILED,    // 상세 표시 (레벨 1-30)
        SIMPLE,      // 간단 표시 (레벨 31-70)  
        MINIMAL      // 최소 표시 (레벨 71-100)
    }
    
    /**
     * 레벨 구간별 생성 메서드
     */
    public static LevelProgressResponse createForLevel(int currentLevel, int currentExp, int requiredExp) {
        ProgressDisplayType displayType = getDisplayType(currentLevel);
        int progressPercent = calculateProgressPercent(currentExp, requiredExp);
        int progressBarLevel = progressPercent / 10; // 0-10 단계
        
        return LevelProgressResponse.builder()
                .currentLevel(currentLevel)
                .displayType(displayType)
                .progressPercent(progressPercent)
                .currentExp(displayType == ProgressDisplayType.DETAILED ? currentExp : null)
                .requiredExp(displayType == ProgressDisplayType.DETAILED ? requiredExp : null)
                .remainingExp(displayType == ProgressDisplayType.DETAILED ? (requiredExp - currentExp) : null)
                .progressMessage(generateProgressMessage(displayType, currentLevel, requiredExp - currentExp, progressPercent))
                .levelTierName(getLevelTierName(currentLevel))
                .progressBarLevel(progressBarLevel)
                .build();
    }
    
    /**
     * 레벨별 표시 타입 결정
     */
    private static ProgressDisplayType getDisplayType(int level) {
        if (level <= 30) {
            return ProgressDisplayType.DETAILED;   // 1-30: 상세 표시
        } else if (level <= 70) {
            return ProgressDisplayType.SIMPLE;     // 31-70: 간단 표시
        } else {
            return ProgressDisplayType.MINIMAL;    // 71-100: 최소 표시
        }
    }
    
    /**
     * 진행률 계산
     */
    private static int calculateProgressPercent(int currentExp, int requiredExp) {
        if (requiredExp == 0) return 100;
        return Math.min(100, (currentExp * 100) / requiredExp);
    }
    
    /**
     * 레벨대별 진행 메시지 생성
     */
    private static String generateProgressMessage(ProgressDisplayType displayType, int level, int remainingExp, int progressPercent) {
        switch (displayType) {
            case DETAILED:
                if (remainingExp <= 50) {
                    return "곧 레벨업이에요! " + remainingExp + " EXP 남음";
                } else if (remainingExp <= 200) {
                    return "레벨업이 가까워요! " + remainingExp + " EXP 남음";
                } else {
                    return "다음 레벨까지 " + remainingExp + " EXP";
                }
                
            case SIMPLE:
                if (progressPercent >= 90) {
                    return "곧 레벨업이에요!";
                } else if (progressPercent >= 70) {
                    return "레벨업이 가까워요!";
                } else if (progressPercent >= 50) {
                    return "절반 이상 달성!";
                } else {
                    return "꾸준히 성장중이에요";
                }
                
            case MINIMAL:
                if (progressPercent >= 80) {
                    return "상당한 진전을 보이고 있어요";
                } else if (progressPercent >= 50) {
                    return "꾸준히 성장중이에요";
                } else {
                    return "차근차근 발전하고 있어요";
                }
                
            default:
                return "레벨 " + level;
        }
    }
    
    /**
     * 레벨 구간명 반환
     */
    private static String getLevelTierName(int level) {
        if (level <= 20) return "🟢 초보 구간";
        else if (level <= 40) return "🟡 성장 구간";
        else if (level <= 60) return "🟠 숙련 구간";
        else if (level <= 80) return "🔴 고수 구간";
        else if (level <= 95) return "🟣 마스터 구간";
        else return "⚫ 레전드 구간";
    }
}