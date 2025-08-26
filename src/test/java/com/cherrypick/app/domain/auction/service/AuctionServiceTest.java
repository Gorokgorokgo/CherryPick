package com.cherrypick.app.domain.auction.service;

import com.cherrypick.app.domain.auction.dto.AuctionResponse;
import com.cherrypick.app.domain.auction.dto.CreateAuctionRequest;
import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.entity.AuctionImage;
import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.auction.repository.AuctionImageRepository;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * AuctionService 핵심 비즈니스 로직 테스트
 * TDD 방식: 실제 비즈니스 규칙 검증에 집중
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("경매 서비스 핵심 비즈니스 로직")
class AuctionServiceTest {

    @Mock
    private AuctionRepository auctionRepository;
    
    @Mock
    private AuctionImageRepository auctionImageRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private AuctionService auctionService;
    
    private User validSeller;
    private CreateAuctionRequest validRequest;
    private List<String> testImageUrls;
    
    @BeforeEach
    void setUp() {
        validSeller = User.builder()
                .id(1L)
                .nickname("테스트판매자")
                .email("seller@test.com")
                .pointBalance(100000L)
                .build();
                
        testImageUrls = Arrays.asList(
            "https://example.com/image1.jpg",
            "https://example.com/image2.jpg"
        );
        
        validRequest = new CreateAuctionRequest();
        validRequest.setTitle("테스트 상품");
        validRequest.setDescription("테스트 설명");
        validRequest.setCategory(Category.ELECTRONICS);
        validRequest.setStartPrice(BigDecimal.valueOf(10000));
        validRequest.setHopePrice(BigDecimal.valueOf(50000));
        validRequest.setReservePrice(BigDecimal.valueOf(15000));
        validRequest.setAuctionTimeHours(24);
        validRequest.setRegionScope(RegionScope.NATIONWIDE);
        validRequest.setImageUrls(testImageUrls);
    }
    
    @Nested
    @DisplayName("경매 생성 - createAuction()")
    class CreateAuctionTest {
        
        @Test
        @DisplayName("성공: 유효한 경매 정보로 경매 생성")
        void createAuction_Success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(validSeller));
            
            Auction savedAuction = createTestAuction(1L, validSeller);
            given(auctionRepository.save(any(Auction.class))).willReturn(savedAuction);
            
            List<AuctionImage> savedImages = createTestImages(savedAuction);
            given(auctionImageRepository.saveAll(any())).willReturn(savedImages);
            
            // when
            AuctionResponse result = auctionService.createAuction(1L, validRequest);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("테스트 상품");
            assertThat(result.getCategory()).isEqualTo(Category.ELECTRONICS);
            assertThat(result.getStartPrice()).isEqualTo(BigDecimal.valueOf(10000));
            assertThat(result.getHopePrice()).isEqualTo(BigDecimal.valueOf(50000));
            assertThat(result.getStatus()).isEqualTo(AuctionStatus.ACTIVE);
            assertThat(result.getSellerNickname()).isEqualTo("테스트판매자");
            
            // 비즈니스 로직 검증: 경매는 즉시 ACTIVE 상태로 시작
            verify(auctionRepository).save(argThat(auction -> 
                auction.getStatus() == AuctionStatus.ACTIVE
            ));
        }
        
        @Test
        @DisplayName("실패: 존재하지 않는 판매자")
        void createAuction_SellerNotFound() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());
            
            // when & then
            assertThatThrownBy(() -> auctionService.createAuction(999L, validRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
            
            // 경매 생성 시도 안 함
            verify(auctionRepository, never()).save(any(Auction.class));
        }
        
        @Test
        @DisplayName("비즈니스 규칙: 이미지 순서 보장")
        void createAuction_ImageOrderPreserved() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(validSeller));
            
            Auction savedAuction = createTestAuction(1L, validSeller);
            given(auctionRepository.save(any(Auction.class))).willReturn(savedAuction);
            
            List<AuctionImage> savedImages = createTestImages(savedAuction);
            given(auctionImageRepository.saveAll(any())).willReturn(savedImages);
            
            // when
            AuctionResponse result = auctionService.createAuction(1L, validRequest);
            
            // then - 이미지 순서가 보장되는지 검증
            verify(auctionImageRepository).saveAll(argThat(images -> {
                List<AuctionImage> imageList = (List<AuctionImage>) images;
                return imageList.get(0).getSortOrder() == 0 &&
                       imageList.get(1).getSortOrder() == 1;
            }));
        }
    }
    
    @Nested
    @DisplayName("경매 종료 처리 - processAuctionEnd()")
    class ProcessAuctionEndTest {
        
        @Test
        @DisplayName("성공: Reserve Price 달성시 낙찰 처리")
        void processAuctionEnd_SuccessfulBid() {
            // given
            Auction activeAuction = createTestAuction(1L, validSeller);
            // Auction의 status는 이미 ACTIVE로 생성됨
            
            User winner = User.builder()
                    .id(2L)
                    .nickname("낙찰자")
                    .email("winner@test.com")
                    .build();
                    
            BigDecimal winningBid = BigDecimal.valueOf(20000); // Reserve Price(15000) 이상
            
            given(auctionRepository.findById(1L)).willReturn(Optional.of(activeAuction));
            given(auctionRepository.save(any(Auction.class))).willReturn(activeAuction);
            
            // when
            auctionService.processAuctionEnd(1L, winningBid, winner);
            
            // then - 비즈니스 로직 검증: 경매 저장 호출됨
            verify(auctionRepository).save(any(Auction.class));
        }
        
        @Test
        @DisplayName("성공: Reserve Price 미달시 유찰 처리")
        void processAuctionEnd_FailedAuction() {
            // given
            Auction activeAuction = createTestAuction(1L, validSeller);
            
            User bidder = User.builder()
                    .id(2L)
                    .nickname("입찰자")
                    .build();
                    
            BigDecimal insufficientBid = BigDecimal.valueOf(10000); // Reserve Price(15000) 미달
            
            given(auctionRepository.findById(1L)).willReturn(Optional.of(activeAuction));
            given(auctionRepository.save(any(Auction.class))).willReturn(activeAuction);
            
            // when
            auctionService.processAuctionEnd(1L, insufficientBid, bidder);
            
            // then - 비즈니스 로직 검증: 유찰 처리로 저장 호출됨
            verify(auctionRepository).save(any(Auction.class));
        }
        
        @Test
        @DisplayName("실패: 이미 종료된 경매 재처리 시도")
        void processAuctionEnd_AlreadyEnded() {
            // given
            Auction endedAuction = createTestAuction(1L, validSeller);
            // Reflection으로 상태 변경 (테스트용)
            setAuctionStatus(endedAuction, AuctionStatus.ENDED);
            
            User bidder = User.builder().id(2L).build();
            
            given(auctionRepository.findById(1L)).willReturn(Optional.of(endedAuction));
            
            // when & then
            assertThatThrownBy(() -> auctionService.processAuctionEnd(1L, BigDecimal.valueOf(20000), bidder))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 종료된 경매입니다");
        }
    }
    
    @Nested
    @DisplayName("경매 상세 조회 - getAuctionDetail()")
    class GetAuctionDetailTest {
        
        @Test
        @DisplayName("성공: 조회시 조회수 1 증가")
        void getAuctionDetail_IncreasesViewCount() {
            // given
            Auction auction = createTestAuction(1L, validSeller);
            int originalViewCount = auction.getViewCount();
            
            List<AuctionImage> images = createTestImages(auction);
            
            given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
            given(auctionRepository.save(any(Auction.class))).willReturn(auction);
            given(auctionImageRepository.findByAuctionIdOrderBySortOrder(1L)).willReturn(images);
            
            // when
            AuctionResponse result = auctionService.getAuctionDetail(1L);
            
            // then - 비즈니스 로직 검증: 조회수 1 증가
            verify(auctionRepository).save(argThat(savedAuction -> 
                savedAuction.getViewCount() == originalViewCount + 1
            ));
            
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("테스트 상품");
        }
        
        @Test
        @DisplayName("실패: 존재하지 않는 경매 조회")
        void getAuctionDetail_AuctionNotFound() {
            // given
            given(auctionRepository.findById(999L)).willReturn(Optional.empty());
            
            // when & then
            assertThatThrownBy(() -> auctionService.getAuctionDetail(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("경매를 찾을 수 없습니다");
                    
            // 조회수 증가 시도 안 함
            verify(auctionRepository, never()).save(any(Auction.class));
        }
    }
    
    // 테스트 헬퍼 메서드들 - Spring ReflectionTestUtils 활용으로 안정성 개선
    private Auction createTestAuction(Long id, User seller) {
        Auction auction = Auction.createAuction(
                seller,
                "테스트 상품",
                "테스트 설명",
                Category.ELECTRONICS,
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(15000),
                24,
                RegionScope.NATIONWIDE,
                null,
                null
        );
        
        // Spring Test의 ReflectionTestUtils 사용 - 안전하고 표준적인 방식
        ReflectionTestUtils.setField(auction, "id", id);
        return auction;
    }
    
    private List<AuctionImage> createTestImages(Auction auction) {
        return Arrays.asList(
            AuctionImage.builder()
                    .auction(auction)
                    .imageUrl("https://example.com/image1.jpg")
                    .sortOrder(0)
                    .build(),
            AuctionImage.builder()
                    .auction(auction)
                    .imageUrl("https://example.com/image2.jpg")
                    .sortOrder(1)
                    .build()
        );
    }
    
    // Spring ReflectionTestUtils 사용으로 안전한 상태 설정
    private void setAuctionStatus(Auction auction, AuctionStatus status) {
        ReflectionTestUtils.setField(auction, "status", status);
    }
}