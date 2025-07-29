package com.cherrypick.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * 찜 목록 엔티티
 */
@Entity
@Table(name = "wishlists", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "auction_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wishlist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * 찜 목록에서 제거 (소프트 삭제)
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 찜 목록에 다시 추가
     */
    public void activate() {
        this.isActive = true;
    }
}