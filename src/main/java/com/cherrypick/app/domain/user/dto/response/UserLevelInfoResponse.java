package com.cherrypick.app.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 사용자 종합 레벨 정보 응답 DTO
 */
@Schema(description = "사용자 종합 레벨 정보")
@Getter
@Builder
public class UserLevelInfoResponse {
    
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    
    @Schema(description = "구매자 레벨 진행률")
    private LevelProgressResponse buyerProgress;
    
    @Schema(description = "판매자 레벨 진행률")  
    private LevelProgressResponse sellerProgress;
    
    @Schema(description = "전체 등급", example = "숙련자")
    private String overallGrade;
    
    /**
     * 전체 등급 계산 후 응답 생성
     */
    public static UserLevelInfoResponse create(Long userId, LevelProgressResponse buyerProgress, LevelProgressResponse sellerProgress) {
        String overallGrade = calculateOverallGrade(buyerProgress.getCurrentLevel(), sellerProgress.getCurrentLevel());
        
        return UserLevelInfoResponse.builder()
            .userId(userId)
            .buyerProgress(buyerProgress)
            .sellerProgress(sellerProgress)
            .overallGrade(overallGrade)
            .build();
    }
    
    /**
     * 구매자/판매자 레벨을 종합한 전체 등급 계산
     */
    private static String calculateOverallGrade(int buyerLevel, int sellerLevel) {
        int maxLevel = Math.max(buyerLevel, sellerLevel);
        int avgLevel = (buyerLevel + sellerLevel) / 2;
        
        // 최고 레벨과 평균 레벨을 고려한 종합 등급
        if (maxLevel >= 95) {
            return "⚫ 레전드";
        } else if (maxLevel >= 80 && avgLevel >= 70) {
            return "🟣 그랜드 마스터";
        } else if (maxLevel >= 80) {
            return "🟣 마스터";
        } else if (maxLevel >= 60 && avgLevel >= 50) {
            return "🔴 전문가";
        } else if (maxLevel >= 60) {
            return "🔴 고수";
        } else if (maxLevel >= 40 && avgLevel >= 35) {
            return "🟠 숙련자";
        } else if (maxLevel >= 40) {
            return "🟠 중급자";
        } else if (maxLevel >= 20 && avgLevel >= 15) {
            return "🟡 활동적 사용자";
        } else if (maxLevel >= 20) {
            return "🟡 일반 사용자";
        } else {
            return "🟢 새싹 사용자";
        }
    }
}