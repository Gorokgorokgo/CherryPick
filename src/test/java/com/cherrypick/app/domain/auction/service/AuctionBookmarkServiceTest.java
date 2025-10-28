package com.cherrypick.app.domain.auction.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.entity.AuctionBookmark;
import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.auction.repository.AuctionBookmarkRepository;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import com.cherrypick.app.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AuctionBookmarkService 테스트
 * 인스타그램/유튜브 스타일 좋아요 기능 구현 검증
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
    "spring.jpa.show-sql=false",
    "logging.level.org.hibernate.SQL=WARN"
})
class AuctionBookmarkServiceTest {

    @Autowired
    private AuctionBookmarkService bookmarkService;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuctionBookmarkRepository bookmarkRepository;

    private User user1;
    private User user2;
    private Auction auction;

    @BeforeEach
    void setUp() {
        // 고유한 타임스탬프 생성
        String timestamp = String.valueOf(System.currentTimeMillis() % 100000000);

        // 테스트 사용자 생성
        user1 = User.builder()
            .phoneNumber("010" + timestamp.substring(0, 8))
            .nickname("사용자1_" + timestamp)
            .email("user1_" + timestamp + "@test.com")
            .password("password")
            .build();
        user1 = userRepository.save(user1);

        user2 = User.builder()
            .phoneNumber("010" + (Long.parseLong(timestamp) + 1))
            .nickname("사용자2_" + timestamp)
            .email("user2_" + timestamp + "@test.com")
            .password("password")
            .build();
        user2 = userRepository.save(user2);

        // 테스트 경매 생성
        auction = Auction.builder()
            .title("테스트 경매 상품")
            .description("테스트 설명")
            .category(Category.ELECTRONICS)
            .startPrice(BigDecimal.valueOf(10000))
            .currentPrice(BigDecimal.valueOf(10000))
            .hopePrice(BigDecimal.valueOf(50000))
            .regionScope(RegionScope.NATIONWIDE)
            .regionCode("11")
            .status(AuctionStatus.ACTIVE)
            .seller(user1)
            .startAt(LocalDateTime.now())
            .endAt(LocalDateTime.now().plusHours(24))
            .auctionTimeHours(24)
            .viewCount(0)
            .bidCount(0)
            .build();
        auction = auctionRepository.save(auction);
    }

    @Test
    @DisplayName("북마크 추가 - 사용자가 경매를 북마크하면 isBookmarked=true, bookmarkCount=1 반환")
    void toggleBookmark_addBookmark_shouldReturnTrueAndCountOne() {
        // When: 사용자가 경매를 북마크
        Map<String, Object> result = bookmarkService.toggleBookmark(auction.getId(), user1.getId());

        // Then: isBookmarked=true, bookmarkCount=1
        assertThat(result.get("isBookmarked")).isEqualTo(true);
        assertThat(result.get("bookmarkCount")).isEqualTo(1L);

        // DB 검증
        boolean exists = bookmarkRepository.existsByAuctionAndUser(auction, user1);
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("북마크 삭제 - 북마크된 경매를 다시 토글하면 isBookmarked=false, bookmarkCount=0 반환")
    void toggleBookmark_removeBookmark_shouldReturnFalseAndCountZero() {
        // Given: 먼저 북마크 추가
        bookmarkService.toggleBookmark(auction.getId(), user1.getId());

        // When: 다시 토글 (삭제)
        Map<String, Object> result = bookmarkService.toggleBookmark(auction.getId(), user1.getId());

        // Then: isBookmarked=false, bookmarkCount=0
        assertThat(result.get("isBookmarked")).isEqualTo(false);
        assertThat(result.get("bookmarkCount")).isEqualTo(0L);

        // DB 검증
        boolean exists = bookmarkRepository.existsByAuctionAndUser(auction, user1);
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("북마크 카운트 정확성 - 여러 사용자가 북마크하면 카운트가 정확히 증가")
    void toggleBookmark_multipleUsers_shouldReturnCorrectCount() {
        // When: user1이 북마크
        Map<String, Object> result1 = bookmarkService.toggleBookmark(auction.getId(), user1.getId());

        // Then: bookmarkCount=1
        assertThat(result1.get("bookmarkCount")).isEqualTo(1L);

        // When: user2도 북마크
        Map<String, Object> result2 = bookmarkService.toggleBookmark(auction.getId(), user2.getId());

        // Then: bookmarkCount=2
        assertThat(result2.get("bookmarkCount")).isEqualTo(2L);

        // When: user1이 북마크 취소
        Map<String, Object> result3 = bookmarkService.toggleBookmark(auction.getId(), user1.getId());

        // Then: bookmarkCount=1 (user2만 남음)
        assertThat(result3.get("bookmarkCount")).isEqualTo(1L);
        assertThat(result3.get("isBookmarked")).isEqualTo(false);
    }

    @Test
    @DisplayName("북마크 상태 조회 - 북마크하지 않은 사용자는 isBookmarked=false")
    void isBookmarked_notBookmarked_shouldReturnFalse() {
        // When: 북마크하지 않은 상태에서 조회
        boolean isBookmarked = bookmarkService.isBookmarked(auction.getId(), user1.getId());

        // Then: false 반환
        assertThat(isBookmarked).isFalse();
    }

    @Test
    @DisplayName("북마크 상태 조회 - 북마크한 사용자는 isBookmarked=true")
    void isBookmarked_bookmarked_shouldReturnTrue() {
        // Given: 북마크 추가
        bookmarkService.toggleBookmark(auction.getId(), user1.getId());

        // When: 북마크 상태 조회
        boolean isBookmarked = bookmarkService.isBookmarked(auction.getId(), user1.getId());

        // Then: true 반환
        assertThat(isBookmarked).isTrue();
    }

    @Test
    @DisplayName("북마크 카운트 조회 - 북마크가 없으면 0 반환")
    void getBookmarkCount_noBookmarks_shouldReturnZero() {
        // When: 북마크가 없는 경매의 카운트 조회
        long count = bookmarkService.getBookmarkCount(auction.getId());

        // Then: 0 반환
        assertThat(count).isEqualTo(0L);
    }

    @Test
    @DisplayName("북마크 정보 조회 - isBookmarked와 bookmarkCount를 함께 반환")
    void getBookmarkInfo_shouldReturnBothStatusAndCount() {
        // Given: user1이 북마크
        bookmarkService.toggleBookmark(auction.getId(), user1.getId());

        // When: user2가 북마크 정보 조회
        Map<String, Object> info = bookmarkService.getBookmarkInfo(auction.getId(), user2.getId());

        // Then: isBookmarked=false (user2는 북마크 안함), bookmarkCount=1 (user1만 북마크)
        assertThat(info.get("isBookmarked")).isEqualTo(false);
        assertThat(info.get("bookmarkCount")).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 경매 북마크 - BusinessException 발생")
    void toggleBookmark_nonExistentAuction_shouldThrowException() {
        // When & Then: 존재하지 않는 경매 ID로 북마크 시도
        assertThatThrownBy(() -> bookmarkService.toggleBookmark(999999L, user1.getId()))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 북마크 - BusinessException 발생")
    void toggleBookmark_nonExistentUser_shouldThrowException() {
        // When & Then: 존재하지 않는 사용자 ID로 북마크 시도
        assertThatThrownBy(() -> bookmarkService.toggleBookmark(auction.getId(), 999999L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("동시 북마크 요청 처리 - 동일 사용자의 중복 요청이 올바르게 처리됨")
    void toggleBookmark_concurrentRequests_shouldHandleCorrectly() throws InterruptedException {
        // Given: 동시 요청 환경 설정
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // When: 동일 사용자가 동시에 여러 번 북마크 요청
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    bookmarkService.toggleBookmark(auction.getId(), user1.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 동시성으로 인한 예외는 무시 (락 대기 등)
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 최종 상태는 북마크 추가 또는 삭제 중 하나 (짝수면 삭제, 홀수면 추가)
        boolean finalStatus = bookmarkRepository.existsByAuctionAndUser(auction, user1);
        long finalCount = bookmarkRepository.countByAuction(auction);

        // 최종 카운트는 0 또는 1이어야 함
        assertThat(finalCount).isIn(0L, 1L);

        if (finalStatus) {
            assertThat(finalCount).isEqualTo(1L);
        } else {
            assertThat(finalCount).isEqualTo(0L);
        }
    }

    @Test
    @DisplayName("사용자별 북마크 목록 조회 - 북마크한 경매 목록 반환")
    void getUserBookmarks_shouldReturnBookmarkedAuctions() {
        // Given: user1이 여러 경매를 북마크
        Auction auction2 = Auction.builder()
            .title("테스트 경매 2")
            .description("설명 2")
            .category(Category.CLOTHING)
            .startPrice(BigDecimal.valueOf(20000))
            .currentPrice(BigDecimal.valueOf(20000))
            .hopePrice(BigDecimal.valueOf(60000))
            .regionScope(RegionScope.NATIONWIDE)
            .regionCode("11")
            .status(AuctionStatus.ACTIVE)
            .seller(user2)
            .startAt(LocalDateTime.now())
            .endAt(LocalDateTime.now().plusHours(24))
            .auctionTimeHours(24)
            .viewCount(0)
            .bidCount(0)
            .build();
        auction2 = auctionRepository.save(auction2);

        bookmarkService.toggleBookmark(auction.getId(), user1.getId());
        bookmarkService.toggleBookmark(auction2.getId(), user1.getId());

        // When: 북마크 목록 조회
        var bookmarks = bookmarkService.getUserBookmarks(user1.getId());

        // Then: 2개의 경매가 조회됨
        assertThat(bookmarks).hasSize(2);
        assertThat(bookmarks).extracting(Auction::getId)
            .containsExactlyInAnyOrder(auction.getId(), auction2.getId());
    }
}
