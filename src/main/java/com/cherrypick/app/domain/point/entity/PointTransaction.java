package com.cherrypick.app.domain.point.entity;

import com.cherrypick.app.domain.common.BaseEntity;
import com.cherrypick.app.domain.user.User;
import com.cherrypick.app.domain.point.enums.PointTransactionType;
import com.cherrypick.app.domain.point.enums.PointTransactionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "points")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointTransactionType type;

    @Column(nullable = false, precision = 10, scale = 0)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 10, scale = 0)
    private BigDecimal balanceAfter;

    @Column(name = "related_type")
    private String relatedType;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointTransactionStatus status = PointTransactionStatus.COMPLETED;
}