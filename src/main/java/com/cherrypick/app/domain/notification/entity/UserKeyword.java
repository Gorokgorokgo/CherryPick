package com.cherrypick.app.domain.notification.entity;

import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.common.entity.BaseEntity;
import com.cherrypick.app.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자 키워드 알림 설정
 * 관심 키워드가 포함된 경매가 등록되면 알림 발송
 */
@Entity
@Table(name = "user_keywords", indexes = {
    @Index(name = "idx_user_keyword_user_id", columnList = "user_id"),
    @Index(name = "idx_user_keyword_keyword", columnList = "keyword"),
    @Index(name = "idx_user_keyword_category", columnList = "category"),
    @Index(name = "idx_user_keyword_active", columnList = "is_active")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserKeyword extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 알림 받을 키워드
     */
    @Column(nullable = false, length = 100)
    private String keyword;

    /**
     * 특정 카테고리로 제한 (null이면 모든 카테고리)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private Category category;

    /**
     * 활성화 여부
     */
    @Builder.Default
    @Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean isActive = true;

    // === 정적 팩토리 메서드 ===

    /**
     * 키워드 알림 생성 (모든 카테고리)
     */
    public static UserKeyword create(User user, String keyword) {
        return UserKeyword.builder()
                .user(user)
                .keyword(keyword.toLowerCase().trim())
                .isActive(true)
                .build();
    }

    /**
     * 키워드 알림 생성 (특정 카테고리)
     */
    public static UserKeyword create(User user, String keyword, Category category) {
        return UserKeyword.builder()
                .user(user)
                .keyword(keyword.toLowerCase().trim())
                .category(category)
                .isActive(true)
                .build();
    }

    // === 비즈니스 메서드 ===

    /**
     * 키워드 활성화/비활성화
     */
    public void setActive(boolean active) {
        this.isActive = active;
    }

    /**
     * 키워드 업데이트
     */
    public void updateKeyword(String newKeyword) {
        this.keyword = newKeyword.toLowerCase().trim();
    }

    /**
     * 카테고리 업데이트
     */
    public void updateCategory(Category category) {
        this.category = category;
    }
}
