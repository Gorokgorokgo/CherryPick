package com.cherrypick.app.domain.transaction.repository;

import com.cherrypick.app.domain.transaction.entity.TransactionReview;
import com.cherrypick.app.domain.transaction.enums.ReviewType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<TransactionReview, Long> {

    /**
     * 특정 거래에 대해 특정 사용자가 이미 후기를 작성했는지 확인
     */
    boolean existsByTransactionIdAndReviewerId(Long transactionId, Long reviewerId);

    /**
     * 특정 사용자가 받은 특정 타입의 후기 목록 조회 (최신순)
     */
    List<TransactionReview> findByRevieweeIdAndReviewTypeOrderByCreatedAtDesc(Long revieweeId, ReviewType reviewType);
}
