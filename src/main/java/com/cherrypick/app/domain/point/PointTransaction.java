package com.cherrypick.app.domain.point;

import com.cherrypick.app.domain.common.BaseEntity;
import com.cherrypick.app.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "point_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointTransactionType type;

    @Column
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Long balanceAfter;

    @Column
    private String referenceId; // 관련 경매 ID, 거래 ID 등
}