package com.cherrypick.app.domain.transaction.entity;

import com.cherrypick.app.domain.common.entity.BaseEntity;
import com.cherrypick.app.domain.transaction.enums.RatingType;
import com.cherrypick.app.domain.transaction.enums.ReviewType;
import com.cherrypick.app.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transaction_reviews")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_id", nullable = false)
    private User reviewee;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_type", nullable = false)
    private ReviewType reviewType;

    @Enumerated(EnumType.STRING)
    @Column(name = "rating_type", nullable = false)
    private RatingType ratingType;

    @Column(name = "content", length = 200)
    private String content;
}
