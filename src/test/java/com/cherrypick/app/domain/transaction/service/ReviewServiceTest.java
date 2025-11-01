package com.cherrypick.app.domain.transaction.service;

import com.cherrypick.app.domain.auction.entity.Auction;
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
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import com.cherrypick.app.domain.user.service.ExperienceService;
import com.cherrypick.app.domain.notification.service.NotificationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("후기 서비스 테스트")
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExperienceService experienceService;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @InjectMocks
    private ReviewService reviewService;

    private User seller;
    private User buyer;
    private Auction auction;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        // 판매자 생성
        seller = User.builder()
                .id(1L)
                .nickname("판매자123")
                .sellerReviewGood(10)
                .sellerReviewNormal(2)
                .sellerReviewBad(1)
                .build();

        // 구매자 생성
        buyer = User.builder()
                .id(2L)
                .nickname("구매자456")
                .buyerReviewGood(5)
                .buyerReviewNormal(1)
                .buyerReviewBad(0)
                .build();

        // 경매 생성
        auction = Auction.builder()
                .id(100L)
                .title("iPhone 14 Pro")
                .seller(seller)
                .build();

        // 거래 생성 (완료 상태)
        transaction = Transaction.builder()
                .id(1000L)
                .auction(auction)
                .seller(seller)
                .buyer(buyer)
                .finalPrice(BigDecimal.valueOf(700000))
                .status(TransactionStatus.COMPLETED)
                .build();
    }

    @Test
    @DisplayName("구매자가 판매자에게 '좋았어요' 후기 작성 성공")
    void createReview_BuyerToSeller_Good_Success() {
        // Given
        CreateReviewRequest request = new CreateReviewRequest(RatingType.GOOD);

        when(transactionRepository.findById(1000L)).thenReturn(Optional.of(transaction));
        when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(reviewRepository.existsByTransactionIdAndReviewerId(1000L, 2L)).thenReturn(false);
        when(reviewRepository.save(any(TransactionReview.class))).thenAnswer(invocation -> {
            TransactionReview review = invocation.getArgument(0);
            review.setId(1L);
            return review;
        });

        // When
        ReviewResponse response = reviewService.createReview(1000L, request, 2L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRatingType()).isEqualTo(RatingType.GOOD);
        assertThat(response.getReviewType()).isEqualTo(ReviewType.SELLER);

        // 판매자 통계 업데이트 확인
        verify(userRepository, times(1)).save(argThat(user ->
                user.getId().equals(1L) && user.getSellerReviewGood() == 11
        ));

        // 후기 작성 보너스 경험치 지급 확인
        verify(experienceService, times(1)).awardReviewBonus(2L);

        // 알림 발송 확인
        verify(notificationEventPublisher, times(1))
                .sendReviewReceivedNotification(eq(seller), eq("구매자456"));
    }

    @Test
    @DisplayName("판매자가 구매자에게 '평범해요' 후기 작성 성공")
    void createReview_SellerToBuyer_Normal_Success() {
        // Given
        CreateReviewRequest request = new CreateReviewRequest(RatingType.NORMAL);

        when(transactionRepository.findById(1000L)).thenReturn(Optional.of(transaction));
        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(reviewRepository.existsByTransactionIdAndReviewerId(1000L, 1L)).thenReturn(false);
        when(reviewRepository.save(any(TransactionReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ReviewResponse response = reviewService.createReview(1000L, request, 1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRatingType()).isEqualTo(RatingType.NORMAL);
        assertThat(response.getReviewType()).isEqualTo(ReviewType.BUYER);

        // 구매자 통계 업데이트 확인
        verify(userRepository, times(1)).save(argThat(user ->
                user.getId().equals(2L) && user.getBuyerReviewNormal() == 2
        ));
    }

    @Test
    @DisplayName("거래 미완료 상태에서 후기 작성 시도 시 예외 발생")
    void createReview_TransactionNotCompleted_ThrowsException() {
        // Given
        transaction.setStatus(TransactionStatus.PENDING);
        CreateReviewRequest request = new CreateReviewRequest(RatingType.GOOD);

        when(transactionRepository.findById(1000L)).thenReturn(Optional.of(transaction));

        // When & Then
        assertThatThrownBy(() -> reviewService.createReview(1000L, request, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("완료된 거래만 후기를 작성할 수 있습니다");
    }

    @Test
    @DisplayName("거래 당사자가 아닌 사람이 후기 작성 시도 시 예외 발생")
    void createReview_NotTransactionParty_ThrowsException() {
        // Given
        CreateReviewRequest request = new CreateReviewRequest(RatingType.GOOD);
        Long unauthorizedUserId = 999L;

        when(transactionRepository.findById(1000L)).thenReturn(Optional.of(transaction));
        when(userRepository.findById(999L)).thenReturn(Optional.of(
                User.builder().id(999L).nickname("제3자").build()
        ));

        // When & Then
        assertThatThrownBy(() -> reviewService.createReview(1000L, request, unauthorizedUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("거래 당사자만 후기를 작성할 수 있습니다");
    }

    @Test
    @DisplayName("이미 후기를 작성한 경우 중복 작성 시도 시 예외 발생")
    void createReview_AlreadyReviewed_ThrowsException() {
        // Given
        CreateReviewRequest request = new CreateReviewRequest(RatingType.GOOD);

        when(transactionRepository.findById(1000L)).thenReturn(Optional.of(transaction));
        when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(reviewRepository.existsByTransactionIdAndReviewerId(1000L, 2L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> reviewService.createReview(1000L, request, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 후기를 작성했습니다");
    }

    @Test
    @DisplayName("판매자 후기 통계 조회 성공")
    void getReviewStats_Seller_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));

        // When
        ReviewStatsResponse stats = reviewService.getReviewStats(1L, ReviewType.SELLER);

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.getUserId()).isEqualTo(1L);
        assertThat(stats.getReviewType()).isEqualTo(ReviewType.SELLER);
        assertThat(stats.getGoodCount()).isEqualTo(10);
        assertThat(stats.getNormalCount()).isEqualTo(2);
        assertThat(stats.getBadCount()).isEqualTo(1);
        assertThat(stats.getTotalReviews()).isEqualTo(13);
        assertThat(stats.getPositiveRate()).isEqualTo(76.92); // (10/13) * 100
    }

    @Test
    @DisplayName("구매자 후기 통계 조회 성공")
    void getReviewStats_Buyer_Success() {
        // Given
        when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));

        // When
        ReviewStatsResponse stats = reviewService.getReviewStats(2L, ReviewType.BUYER);

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.getGoodCount()).isEqualTo(5);
        assertThat(stats.getNormalCount()).isEqualTo(1);
        assertThat(stats.getBadCount()).isEqualTo(0);
        assertThat(stats.getTotalReviews()).isEqualTo(6);
        assertThat(stats.getPositiveRate()).isEqualTo(83.33); // (5/6) * 100
    }

    @Test
    @DisplayName("후기 통계 없는 사용자 조회 시 0으로 초기화된 통계 반환")
    void getReviewStats_NoReviews_ReturnsZeroStats() {
        // Given
        User newUser = User.builder()
                .id(3L)
                .nickname("신규유저")
                .sellerReviewGood(0)
                .sellerReviewNormal(0)
                .sellerReviewBad(0)
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(newUser));

        // When
        ReviewStatsResponse stats = reviewService.getReviewStats(3L, ReviewType.SELLER);

        // Then
        assertThat(stats.getTotalReviews()).isEqualTo(0);
        assertThat(stats.getPositiveRate()).isEqualTo(0.0);
    }
}
