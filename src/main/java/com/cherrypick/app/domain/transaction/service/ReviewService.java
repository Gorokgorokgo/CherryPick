package com.cherrypick.app.domain.transaction.service;

import com.cherrypick.app.domain.transaction.dto.request.CreateReviewRequest;
import com.cherrypick.app.domain.transaction.dto.response.ReviewResponse;
import com.cherrypick.app.domain.transaction.dto.response.ReviewStatsResponse;
import com.cherrypick.app.domain.transaction.entity.Transaction;
import com.cherrypick.app.domain.transaction.entity.TransactionReview;
import com.cherrypick.app.domain.transaction.enums.RatingType;
import com.cherrypick.app.domain.transaction.enums.ReviewType;
import com.cherrypick.app.domain.transaction.enums.TransactionStatus;
import com.cherrypick.app.domain.transaction.repository.ReviewRepository;
import com.cherrypick.app.domain.transaction.repository.TransactionRepository;
import com.cherrypick.app.domain.user.dto.response.ExperienceGainResponse;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import com.cherrypick.app.domain.user.service.ExperienceService;
import com.cherrypick.app.domain.notification.service.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final ExperienceService experienceService;
    private final NotificationEventPublisher notificationEventPublisher;

    private static final int REVIEW_BONUS_EXP = 10;

    /**
     * 후기 작성
     *
     * @param transactionId 거래 ID
     * @param request 후기 작성 요청
     * @param reviewerId 후기 작성자 ID
     * @return 후기 응답
     */
    @Transactional
    public ReviewResponse createReview(Long transactionId, CreateReviewRequest request, Long reviewerId) {
        // 1. 거래 검증
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다"));

        // 2. 거래 완료 상태인지 확인
        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new IllegalStateException("완료된 거래만 후기를 작성할 수 있습니다");
        }

        // 3. 본인 확인 및 대상자 결정
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        User reviewee;
        ReviewType reviewType;
        if (transaction.getSeller().getId().equals(reviewerId)) {
            reviewee = transaction.getBuyer();
            reviewType = ReviewType.BUYER;
        } else if (transaction.getBuyer().getId().equals(reviewerId)) {
            reviewee = transaction.getSeller();
            reviewType = ReviewType.SELLER;
        } else {
            throw new IllegalArgumentException("거래 당사자만 후기를 작성할 수 있습니다");
        }

        // 4. 중복 후기 방지
        if (reviewRepository.existsByTransactionIdAndReviewerId(transactionId, reviewerId)) {
            throw new IllegalStateException("이미 후기를 작성했습니다");
        }

        // 5. 후기 저장
        TransactionReview review = TransactionReview.builder()
                .transaction(transaction)
                .reviewer(reviewer)
                .reviewee(reviewee)
                .reviewType(reviewType)
                .ratingType(request.getRatingType())
                .content(request.getContent())
                .build();

        reviewRepository.save(review);

        // 6. 사용자 통계 업데이트
        updateUserReviewStats(reviewee, reviewType, request.getRatingType());

        // 7. 후기 작성 보너스 경험치 지급 (+10 EXP)
        boolean isReviewerSeller = transaction.getSeller().getId().equals(reviewerId);
        ExperienceGainResponse experienceResponse = experienceService.awardReviewBonus(reviewerId, isReviewerSeller);

        // 8. 상대방에게 알림 발송
        notificationEventPublisher.sendReviewReceivedNotification(reviewee, reviewer.getNickname());

        log.info("후기 작성 완료 - transactionId: {}, reviewer: {}, reviewee: {}, rating: {}",
                transactionId, reviewerId, reviewee.getId(), request.getRatingType());

        return ReviewResponse.fromWithExperience(review, experienceResponse);
    }

    /**
     * 사용자 후기 통계 업데이트
     */
    private void updateUserReviewStats(User user, ReviewType reviewType, RatingType ratingType) {
        if (reviewType == ReviewType.SELLER) {
            switch (ratingType) {
                case GOOD -> user.setSellerReviewGood(user.getSellerReviewGood() + 1);
                case NORMAL -> user.setSellerReviewNormal(user.getSellerReviewNormal() + 1);
                case BAD -> user.setSellerReviewBad(user.getSellerReviewBad() + 1);
            }
        } else {
            switch (ratingType) {
                case GOOD -> user.setBuyerReviewGood(user.getBuyerReviewGood() + 1);
                case NORMAL -> user.setBuyerReviewNormal(user.getBuyerReviewNormal() + 1);
                case BAD -> user.setBuyerReviewBad(user.getBuyerReviewBad() + 1);
            }
        }
        userRepository.save(user);
    }

    /**
     * 사용자 후기 통계 조회
     *
     * @param userId 사용자 ID
     * @param reviewType 후기 타입 (SELLER 또는 BUYER)
     * @return 후기 통계
     */
    public ReviewStatsResponse getReviewStats(Long userId, ReviewType reviewType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        int goodCount, normalCount, badCount;

        if (reviewType == ReviewType.SELLER) {
            goodCount = user.getSellerReviewGood();
            normalCount = user.getSellerReviewNormal();
            badCount = user.getSellerReviewBad();
        } else {
            goodCount = user.getBuyerReviewGood();
            normalCount = user.getBuyerReviewNormal();
            badCount = user.getBuyerReviewBad();
        }

        return ReviewStatsResponse.of(userId, reviewType, goodCount, normalCount, badCount);
    }

    /**
     * 받은 후기 목록 조회
     *
     * @param userId 사용자 ID
     * @param reviewType 후기 타입 (SELLER 또는 BUYER)
     * @return 후기 목록
     */
    public List<ReviewResponse> getReviews(Long userId, ReviewType reviewType) {
        // 사용자 존재 확인
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }

        // 후기 목록 조회
        List<TransactionReview> reviews = reviewRepository
                .findByRevieweeIdAndReviewTypeOrderByCreatedAtDesc(userId, reviewType);

        return reviews.stream()
                .map(review -> ReviewResponse.from(review, 0)) // 조회 시에는 보너스 EXP 없음
                .collect(Collectors.toList());
    }
}
