package com.cherrypick.app.domain.user.entity;

import com.cherrypick.app.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 경험치 획득 히스토리 엔티티
 * - 경험치 획득 이벤트 추적
 * - 레벨업 알림 데이터
 * - 프론트엔드 애니메이션 트리거
 */
@Entity
@Table(name = "experience_history", indexes = {
    @Index(name = "idx_user_created", columnList = "user_id, created_at"),
    @Index(name = "idx_levelup", columnList = "is_level_up, created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 경험치 타입 (구매자/판매자)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExperienceType type;

    /**
     * 획득한 경험치
     */
    @Column(nullable = false)
    private Integer expGained;

    /**
     * 이전 경험치
     */
    @Column(nullable = false)
    private Integer expBefore;

    /**
     * 변경 후 경험치
     */
    @Column(nullable = false)
    private Integer expAfter;

    /**
     * 이전 레벨
     */
    @Column(nullable = false)
    private Integer levelBefore;

    /**
     * 변경 후 레벨
     */
    @Column(nullable = false)
    private Integer levelAfter;

    /**
     * 레벨업 여부
     */
    @Builder.Default
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isLevelUp = false;

    /**
     * 경험치 획득 사유
     */
    @Column(nullable = false, length = 50)
    private String reason;

    /**
     * 경험치 획득 사유 상세 (옵션)
     */
    @Column(length = 200)
    private String reasonDetail;

    /**
     * 알림 전송 여부
     */
    @Builder.Default
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean notificationSent = false;

    /**
     * 경험치 타입 enum
     */
    public enum ExperienceType {
        BUYER("구매력"),
        SELLER("판매력");

        private final String description;

        ExperienceType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 레벨업 체크
     */
    public void checkLevelUp() {
        this.isLevelUp = !this.levelBefore.equals(this.levelAfter);
    }
}